#!/usr/bin/env python3
"""Collect DeepScan predictions for a labelled image manifest using only stdlib."""

from __future__ import annotations

import argparse
import csv
import hashlib
import http.cookiejar
import json
import mimetypes
from pathlib import Path
import time
import urllib.error
import urllib.request
import uuid


OUTPUT_FIELDS = [
    "sample_id",
    "ground_truth",
    "split",
    "source",
    "manipulation_type",
    "file_sha256",
    "score",
    "verdict",
    "provider",
    "model_version",
    "latency_ms",
    "error",
]
REQUIRED_FIELDS = {"sample_id", "file_path", "ground_truth", "split"}


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--base-url", default="http://127.0.0.1:11000")
    parser.add_argument("--model-version", required=True)
    parser.add_argument("--timeout", type=float, default=360.0)
    parser.add_argument("--delay", type=float, default=0.0)
    return parser.parse_args()


def multipart_body(field_name: str, path: Path) -> tuple[bytes, str]:
    boundary = "----DeepScanBenchmark" + uuid.uuid4().hex
    mime_type = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    safe_name = path.name.replace('"', "_").replace("\r", "_").replace("\n", "_")
    header = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="{field_name}"; filename="{safe_name}"\r\n'
        f"Content-Type: {mime_type}\r\n\r\n"
    ).encode("utf-8")
    body = header + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode("ascii")
    return body, f"multipart/form-data; boundary={boundary}"


def request_json(opener: urllib.request.OpenerDirector,
                 request: urllib.request.Request,
                 timeout: float) -> dict:
    try:
        with opener.open(request, timeout=timeout) as response:
            payload = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exc.code}: {detail[:300]}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Connection failed: {exc.reason}") from exc
    try:
        return json.loads(payload)
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"Non-JSON response: {payload[:300]}") from exc


def collect_one(opener: urllib.request.OpenerDirector,
                base_url: str,
                image_path: Path,
                timeout: float) -> dict:
    body, content_type = multipart_body("file", image_path)
    create_request = urllib.request.Request(
        base_url.rstrip("/") + "/api/v1/verifications",
        data=body,
        headers={"Content-Type": content_type, "Accept": "application/json"},
        method="POST",
    )
    started = time.perf_counter()
    created = request_json(opener, create_request, timeout)
    latency_ms = round((time.perf_counter() - started) * 1000.0, 1)
    if not created.get("success"):
        error = created.get("error") or {}
        raise RuntimeError(f"{error.get('code', 'API_ERROR')}: {error.get('message', 'unknown error')}")

    data = created.get("data") or {}
    detail_data = {}
    verification_id = data.get("id")
    if verification_id is not None:
        detail_request = urllib.request.Request(
            base_url.rstrip("/") + f"/api/v1/verifications/{verification_id}",
            headers={"Accept": "application/json"},
            method="GET",
        )
        detail = request_json(opener, detail_request, timeout)
        if detail.get("success"):
            detail_data = detail.get("data") or {}

    return {
        "score": detail_data.get("score", data.get("score", "")),
        "verdict": detail_data.get("verdict", data.get("verdict", "")),
        "provider": detail_data.get("apiProvider", ""),
        "latency_ms": latency_ms,
    }


def read_completed_ids(output: Path) -> set[str]:
    if not output.exists():
        return set()
    with output.open("r", encoding="utf-8-sig", newline="") as handle:
        return {row.get("sample_id", "").strip() for row in csv.DictReader(handle)}


def main() -> int:
    args = parse_args()
    manifest = args.manifest.resolve()
    output = args.output.resolve()
    if not manifest.is_file():
        raise SystemExit(f"Manifest not found: {manifest}")
    if not args.model_version.strip():
        raise SystemExit("--model-version must not be blank")

    output.parent.mkdir(parents=True, exist_ok=True)
    completed_ids = read_completed_ids(output)
    write_header = not output.exists() or output.stat().st_size == 0
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    with manifest.open("r", encoding="utf-8-sig", newline="") as source_handle:
        reader = csv.DictReader(source_handle)
        missing = REQUIRED_FIELDS.difference(reader.fieldnames or [])
        if missing:
            raise SystemExit(f"Manifest is missing columns: {', '.join(sorted(missing))}")
        rows = list(reader)

    seen: set[str] = set()
    with output.open("a", encoding="utf-8", newline="") as output_handle:
        writer = csv.DictWriter(output_handle, fieldnames=OUTPUT_FIELDS)
        if write_header:
            writer.writeheader()

        for index, row in enumerate(rows, start=1):
            sample_id = row.get("sample_id", "").strip()
            if not sample_id or sample_id in seen:
                raise SystemExit(f"Missing or duplicate sample_id at manifest row {index + 1}: {sample_id!r}")
            seen.add(sample_id)
            if sample_id in completed_ids:
                print(f"[{index}/{len(rows)}] skip {sample_id} (already collected)")
                continue

            image_path = Path(row.get("file_path", ""))
            if not image_path.is_absolute():
                image_path = manifest.parent / image_path

            result = {
                "sample_id": sample_id,
                "ground_truth": row.get("ground_truth", "").strip(),
                "split": row.get("split", "").strip(),
                "source": row.get("source", "").strip(),
                "manipulation_type": row.get("manipulation_type", "").strip(),
                "file_sha256": "",
                "score": "",
                "verdict": "",
                "provider": "",
                "model_version": args.model_version.strip(),
                "latency_ms": "",
                "error": "",
            }
            try:
                if not image_path.is_file():
                    raise RuntimeError(f"Image not found: {image_path}")
                result["file_sha256"] = file_sha256(image_path)
                result.update(collect_one(opener, args.base_url, image_path, args.timeout))
                print(f"[{index}/{len(rows)}] ok {sample_id}: {result['verdict']} {result['score']}")
            except Exception as exc:  # Continue so coverage/error rate is measurable.
                result["error"] = str(exc)[:500]
                print(f"[{index}/{len(rows)}] error {sample_id}: {result['error']}")

            writer.writerow(result)
            output_handle.flush()
            if args.delay > 0:
                time.sleep(args.delay)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

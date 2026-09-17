#!/usr/bin/env python3
"""Generate reproducible binary deepfake-detection metrics from prediction CSV."""

from __future__ import annotations

import argparse
import csv
import json
import math
from pathlib import Path
import random
from statistics import mean
from typing import Optional


METRIC_NAMES = [
    "accuracy",
    "precision",
    "recall",
    "specificity",
    "f1",
    "balanced_accuracy",
    "roc_auc",
    "average_precision",
    "brier_score",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--threshold", type=float, default=0.5)
    parser.add_argument("--selection-split", default="")
    parser.add_argument("--evaluation-split", default="test")
    parser.add_argument("--bootstrap-samples", type=int, default=1000)
    parser.add_argument("--seed", type=int, default=2973)
    parser.add_argument("--min-subgroup-size", type=int, default=10)
    return parser.parse_args()


def normalize_label(value: str) -> int:
    normalized = (value or "").strip().upper()
    if normalized in {"FAKE", "MANIPULATED", "1", "TRUE"}:
        return 1
    if normalized in {"REAL", "AUTHENTIC", "0", "FALSE"}:
        return 0
    raise ValueError(f"unsupported ground_truth {value!r}")


def safe_div(numerator: float, denominator: float) -> Optional[float]:
    return numerator / denominator if denominator else None


def percentile(values: list[float], probability: float) -> Optional[float]:
    if not values:
        return None
    ordered = sorted(values)
    position = (len(ordered) - 1) * probability
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    fraction = position - lower
    return ordered[lower] * (1.0 - fraction) + ordered[upper] * fraction


def roc_auc(labels: list[int], scores: list[float]) -> Optional[float]:
    positives = sum(labels)
    negatives = len(labels) - positives
    if positives == 0 or negatives == 0:
        return None

    ranked = sorted(zip(scores, labels), key=lambda item: item[0])
    positive_rank_sum = 0.0
    index = 0
    while index < len(ranked):
        end = index + 1
        while end < len(ranked) and ranked[end][0] == ranked[index][0]:
            end += 1
        average_rank = ((index + 1) + end) / 2.0
        positive_rank_sum += average_rank * sum(label for _, label in ranked[index:end])
        index = end
    return (positive_rank_sum - positives * (positives + 1) / 2.0) / (positives * negatives)


def average_precision(labels: list[int], scores: list[float]) -> Optional[float]:
    positives = sum(labels)
    if positives == 0:
        return None
    ranked = sorted(zip(scores, labels), key=lambda item: item[0], reverse=True)
    true_positives = 0
    false_positives = 0
    previous_recall = 0.0
    area = 0.0
    index = 0
    while index < len(ranked):
        end = index + 1
        while end < len(ranked) and ranked[end][0] == ranked[index][0]:
            end += 1
        true_positives += sum(label for _, label in ranked[index:end])
        false_positives += sum(1 - label for _, label in ranked[index:end])
        recall = true_positives / positives
        precision = true_positives / (true_positives + false_positives)
        area += (recall - previous_recall) * precision
        previous_recall = recall
        index = end
    return area


def calculate_metrics(rows: list[dict], threshold: float) -> dict:
    labels = [row["label"] for row in rows]
    scores = [row["score"] for row in rows]
    predictions = [1 if score >= threshold else 0 for score in scores]
    tp = sum(label == 1 and prediction == 1 for label, prediction in zip(labels, predictions))
    tn = sum(label == 0 and prediction == 0 for label, prediction in zip(labels, predictions))
    fp = sum(label == 0 and prediction == 1 for label, prediction in zip(labels, predictions))
    fn = sum(label == 1 and prediction == 0 for label, prediction in zip(labels, predictions))
    precision = safe_div(tp, tp + fp)
    recall = safe_div(tp, tp + fn)
    specificity = safe_div(tn, tn + fp)
    f1 = safe_div(2 * precision * recall, precision + recall) if precision is not None and recall is not None else None
    balanced = (recall + specificity) / 2.0 if recall is not None and specificity is not None else None
    return {
        "support": len(rows),
        "positive": sum(labels),
        "negative": len(labels) - sum(labels),
        "confusion_matrix": {"tp": tp, "fp": fp, "tn": tn, "fn": fn},
        "accuracy": safe_div(tp + tn, len(rows)),
        "precision": precision,
        "recall": recall,
        "specificity": specificity,
        "f1": f1,
        "balanced_accuracy": balanced,
        "roc_auc": roc_auc(labels, scores),
        "average_precision": average_precision(labels, scores),
        "brier_score": mean((score - label) ** 2 for label, score in zip(labels, scores)),
    }


def select_threshold(rows: list[dict]) -> tuple[float, dict]:
    candidates = sorted({0.0, 1.0, *(row["score"] for row in rows)})
    evaluated = [(threshold, calculate_metrics(rows, threshold)) for threshold in candidates]

    def ranking(item: tuple[float, dict]) -> tuple[float, float, float, float]:
        threshold, metrics = item
        return (
            metrics["f1"] if metrics["f1"] is not None else -1.0,
            metrics["balanced_accuracy"] if metrics["balanced_accuracy"] is not None else -1.0,
            -abs(threshold - 0.5),
            -threshold,
        )

    return max(evaluated, key=ranking)


def bootstrap_intervals(rows: list[dict], threshold: float, samples: int, seed: int) -> dict:
    if samples <= 0 or not rows:
        return {}
    generator = random.Random(seed)
    collected = {name: [] for name in METRIC_NAMES}
    for _ in range(samples):
        sample = [rows[generator.randrange(len(rows))] for _ in rows]
        metrics = calculate_metrics(sample, threshold)
        for name in METRIC_NAMES:
            value = metrics[name]
            if value is not None:
                collected[name].append(value)
    return {
        name: {
            "low": percentile(values, 0.025),
            "high": percentile(values, 0.975),
            "valid_samples": len(values),
        }
        for name, values in collected.items()
    }


def load_rows(path: Path) -> tuple[list[dict], list[dict]]:
    valid: list[dict] = []
    invalid: list[dict] = []
    seen: set[str] = set()
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        required = {"sample_id", "ground_truth", "split", "score"}
        missing = required.difference(reader.fieldnames or [])
        if missing:
            raise ValueError(f"missing columns: {', '.join(sorted(missing))}")
        for line_number, raw in enumerate(reader, start=2):
            sample_id = (raw.get("sample_id") or "").strip()
            reason = (raw.get("error") or "").strip()
            try:
                if not sample_id or sample_id in seen:
                    raise ValueError("sample_id is missing or duplicated")
                seen.add(sample_id)
                if reason:
                    raise ValueError(f"detector error: {reason}")
                label = normalize_label(raw.get("ground_truth", ""))
                score = float(raw.get("score") or "")
                if not math.isfinite(score) or score < 0.0 or score > 1.0:
                    raise ValueError("score must be between 0 and 1")
                latency_text = (raw.get("latency_ms") or "").strip()
                latency_ms = float(latency_text) if latency_text else None
                if latency_ms is not None and (not math.isfinite(latency_ms) or latency_ms < 0):
                    raise ValueError("latency_ms must be non-negative")
                valid.append({
                    "sample_id": sample_id,
                    "label": label,
                    "score": score,
                    "split": (raw.get("split") or "").strip(),
                    "source": (raw.get("source") or "").strip() or "unspecified",
                    "manipulation_type": (raw.get("manipulation_type") or "").strip() or "unspecified",
                    "provider": (raw.get("provider") or "").strip() or "unspecified",
                    "model_version": (raw.get("model_version") or "").strip() or "unspecified",
                    "file_sha256": (raw.get("file_sha256") or "").strip().lower(),
                    "latency_ms": latency_ms,
                })
            except (ValueError, TypeError) as exc:
                invalid.append({
                    "line": line_number,
                    "sample_id": sample_id,
                    "split": (raw.get("split") or "").strip(),
                    "reason": str(exc),
                })
    return valid, invalid


def subgroup_reports(rows: list[dict], threshold: float, field: str, minimum: int) -> list[dict]:
    values = sorted({row[field] for row in rows})
    reports = []
    for value in values:
        subset = [row for row in rows if row[field] == value]
        if len(subset) >= minimum:
            reports.append({"value": value, **calculate_metrics(subset, threshold)})
    return reports


def fmt(value: Optional[float]) -> str:
    return "N/A" if value is None else f"{value:.3f}"


def render_markdown(report: dict) -> str:
    metrics = report["evaluation"]["metrics"]
    intervals = report["evaluation"]["confidence_intervals_95"]
    confusion = metrics["confusion_matrix"]
    lines = [
        "# DeepScan detection benchmark report",
        "",
        "> This report measures the supplied labelled dataset only; it is not a universal accuracy claim.",
        "",
        "## Protocol",
        "",
        f"- Evaluation split: `{report['protocol']['evaluation_split']}`",
        f"- Threshold: `{report['protocol']['threshold']:.6f}`",
        f"- Threshold source: {report['protocol']['threshold_source']}",
        f"- Bootstrap samples: {report['protocol']['bootstrap_samples']}",
        f"- Model versions: {', '.join(report['dataset']['model_versions'])}",
        f"- Providers: {', '.join(report['dataset']['providers'])}",
        "",
        "## Dataset and coverage",
        "",
        f"- Total rows: {report['dataset']['total_rows']}",
        f"- Evaluable rows: {report['dataset']['evaluable_rows']}",
        f"- Invalid/error rows: {report['dataset']['invalid_rows']}",
        f"- Evaluation coverage: {report['evaluation']['coverage']:.1%}",
        f"- Test support: {metrics['support']} ({metrics['positive']} fake, {metrics['negative']} real)",
        "",
        "## Primary metrics",
        "",
        "| Metric | Estimate | 95% bootstrap CI |",
        "|---|---:|---:|",
    ]
    for name in METRIC_NAMES:
        ci = intervals.get(name, {})
        interval = "N/A" if ci.get("low") is None else f"{ci['low']:.3f}–{ci['high']:.3f}"
        lines.append(f"| {name.replace('_', ' ').title()} | {fmt(metrics[name])} | {interval} |")
    lines.extend([
        "",
        "## Confusion matrix",
        "",
        "| | Predicted fake | Predicted real |",
        "|---|---:|---:|",
        f"| Actual fake | {confusion['tp']} | {confusion['fn']} |",
        f"| Actual real | {confusion['fp']} | {confusion['tn']} |",
        "",
        "## Latency",
        "",
        f"- Samples with latency: {report['evaluation']['latency']['support']}",
        f"- p50: {fmt(report['evaluation']['latency']['p50_ms'])} ms",
        f"- p95: {fmt(report['evaluation']['latency']['p95_ms'])} ms",
    ])
    for field, groups in report["subgroups"].items():
        lines.extend(["", f"## Subgroups: {field}", ""])
        if not groups:
            lines.append("No subgroup met the minimum sample size.")
            continue
        lines.extend(["| Group | N | F1 | Recall | Specificity | ROC-AUC |", "|---|---:|---:|---:|---:|---:|"])
        for group in groups:
            lines.append(
                f"| {group['value']} | {group['support']} | {fmt(group['f1'])} | "
                f"{fmt(group['recall'])} | {fmt(group['specificity'])} | {fmt(group['roc_auc'])} |"
            )
    lines.extend([
        "",
        "## Reporting cautions",
        "",
        "- Do not compare models tested on different image sets as if they used the same benchmark.",
        "- Do not tune the threshold on this test split after reading this report.",
        "- Review invalid/error rows separately; excluding failures without reporting coverage inflates quality.",
        "- Report subgroup failures and confidence intervals alongside the headline F1.",
        "",
    ])
    return "\n".join(lines)


def build_report(valid: list[dict], invalid: list[dict], args: argparse.Namespace) -> dict:
    versions = sorted({row["model_version"] for row in valid})
    if len(versions) > 1:
        raise ValueError(
            "multiple model_version values found; evaluate one frozen model configuration at a time: "
            + ", ".join(versions)
        )
    hashes: dict[str, str] = {}
    for row in valid:
        digest = row.get("file_sha256", "")
        if not digest:
            continue
        previous = hashes.get(digest)
        if previous is not None:
            raise ValueError(
                f"duplicate file_sha256 detected for {previous!r} and {row['sample_id']!r}"
            )
        hashes[digest] = row["sample_id"]

    threshold = args.threshold
    threshold_source = "pre-declared fixed threshold"
    selection = None
    if args.selection_split:
        selection_rows = [row for row in valid if row["split"] == args.selection_split]
        if not selection_rows:
            raise ValueError(f"no evaluable rows for selection split {args.selection_split!r}")
        threshold, selection_metrics = select_threshold(selection_rows)
        selection = {"split": args.selection_split, "metrics": selection_metrics}
        threshold_source = f"maximum validation F1 on split {args.selection_split!r}"

    evaluation_rows = [row for row in valid if row["split"] == args.evaluation_split]
    if not evaluation_rows:
        raise ValueError(f"no evaluable rows for evaluation split {args.evaluation_split!r}")
    metrics = calculate_metrics(evaluation_rows, threshold)
    intervals = bootstrap_intervals(
        evaluation_rows, threshold, args.bootstrap_samples, args.seed
    )
    latencies = [row["latency_ms"] for row in evaluation_rows if row["latency_ms"] is not None]
    all_evaluation_rows = len(evaluation_rows) + sum(
        1 for row in invalid if row.get("split") == args.evaluation_split
    )
    coverage_denominator = all_evaluation_rows
    coverage = safe_div(len(evaluation_rows), coverage_denominator) or 0.0

    return {
        "protocol": {
            "positive_class": "FAKE",
            "score_meaning": "probability-like manipulation risk in [0, 1]",
            "evaluation_split": args.evaluation_split,
            "threshold": threshold,
            "threshold_source": threshold_source,
            "bootstrap_samples": args.bootstrap_samples,
            "bootstrap_seed": args.seed,
            "selection": selection,
        },
        "dataset": {
            "total_rows": len(valid) + len(invalid),
            "evaluable_rows": len(valid),
            "invalid_rows": len(invalid),
            "invalid_details": invalid,
            "providers": sorted({row["provider"] for row in valid}),
            "model_versions": versions,
        },
        "evaluation": {
            "coverage": coverage,
            "metrics": metrics,
            "confidence_intervals_95": intervals,
            "latency": {
                "support": len(latencies),
                "p50_ms": percentile(latencies, 0.50),
                "p95_ms": percentile(latencies, 0.95),
            },
        },
        "subgroups": {
            "source": subgroup_reports(evaluation_rows, threshold, "source", args.min_subgroup_size),
            "manipulation_type": subgroup_reports(
                evaluation_rows, threshold, "manipulation_type", args.min_subgroup_size
            ),
        },
    }


def main() -> int:
    args = parse_args()
    if not 0.0 <= args.threshold <= 1.0:
        raise SystemExit("--threshold must be between 0 and 1")
    if args.bootstrap_samples < 0:
        raise SystemExit("--bootstrap-samples must be non-negative")
    if not args.input.is_file():
        raise SystemExit(f"Input not found: {args.input}")

    try:
        valid, invalid = load_rows(args.input)
        report = build_report(valid, invalid, args)
    except ValueError as exc:
        raise SystemExit(str(exc)) from exc

    args.output_dir.mkdir(parents=True, exist_ok=True)
    json_path = args.output_dir / "metrics.json"
    markdown_path = args.output_dir / "report.md"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    markdown_path.write_text(render_markdown(report), encoding="utf-8")

    metrics = report["evaluation"]["metrics"]
    print(f"threshold={report['protocol']['threshold']:.6f}")
    print(f"support={metrics['support']} coverage={report['evaluation']['coverage']:.1%}")
    print(f"precision={fmt(metrics['precision'])} recall={fmt(metrics['recall'])} f1={fmt(metrics['f1'])}")
    print(f"roc_auc={fmt(metrics['roc_auc'])} average_precision={fmt(metrics['average_precision'])}")
    print(f"wrote {json_path}")
    print(f"wrote {markdown_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

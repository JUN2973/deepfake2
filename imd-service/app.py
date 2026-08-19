import base64
import io
import logging
import os
import sys
import tempfile
from functools import lru_cache
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile

app = FastAPI(title="IMD Adapter", version="1.0.0")
logger = logging.getLogger("imd-service")

# Spring Boot가 이 FastAPI 서버를 호출할 때 사용하는 히트맵 처리 기준값이다.
# 운영/시연 환경마다 민감도를 바꿀 수 있도록 환경변수로 덮어쓸 수 있게 했다.
HEATMAP_THRESHOLD = float(os.environ.get("IMD_HEATMAP_THRESHOLD", "0.6"))
MIN_REGION_AREA_RATIO = float(os.environ.get("IMD_MIN_REGION_AREA_RATIO", "0.003"))
HEATMAP_BLUR_KERNEL = int(os.environ.get("IMD_HEATMAP_BLUR_KERNEL", "7"))
HEATMAP_OPEN_KERNEL = int(os.environ.get("IMD_HEATMAP_OPEN_KERNEL", "3"))
HEATMAP_OVERLAY_ALPHA = float(os.environ.get("IMD_OVERLAY_ALPHA", "0.48"))
IMD_MAX_IMAGE_SIZE = int(os.environ.get("IMD_MAX_IMAGE_SIZE", "768"))
FACE_FOCUS_ENABLED = os.environ.get("IMD_FACE_FOCUS_ENABLED", "true").lower() == "true"
FACE_PADDING_RATIO = float(os.environ.get("IMD_FACE_PADDING_RATIO", "0.28"))


@lru_cache(maxsize=1)
def _image_libs():
    # OpenCV, numpy는 무거운 라이브러리라 실제 분석 요청이 들어왔을 때 한 번만 불러온다.
    import cv2
    import matplotlib
    import numpy as np

    matplotlib.use("Agg")
    return cv2, np


def _imd_repo() -> Path:
    # IMD 원본 모델 코드는 별도 저장소에 있으므로 IMD_REPO 환경변수로 위치를 받는다.
    # 이 경로가 없으면 모델 파일을 찾을 수 없어 분석을 진행할 수 없다.
    repo = Path(os.environ.get("IMD_REPO", "")).expanduser()
    if not repo.exists():
        raise RuntimeError("Set IMD_REPO to the cloned gagan3012/IMD directory.")
    if str(repo) not in sys.path:
        sys.path.insert(0, str(repo))
    return repo


@lru_cache(maxsize=1)
def _load_mantranet():
    # ManTraNet 모델은 이미지 조작 흔적을 히트맵 형태로 찾기 위해 사용한다.
    # 모델 로딩 비용이 크므로 lru_cache로 서버 실행 중 한 번만 로딩한다.
    repo = _imd_repo()
    device = os.environ.get("IMD_DEVICE", "cpu")
    current_dir = Path.cwd()
    try:
        os.chdir(repo)
        from MantraNet.mantranet import check_forgery, pre_trained_model

        model = pre_trained_model(weight_path=str(repo / "MantraNet" / "MantraNetv4.pt"), device=device)
    finally:
        os.chdir(current_dir)
    return model, check_forgery, device


@lru_cache(maxsize=1)
def _load_busternet():
    # BusterNet은 복사-붙여넣기 조작 탐지 보조 모델이다.
    # 기본값으로는 꺼져 있고, IMD_ENABLE_BUSTERNET=true일 때만 응답에 추가한다.
    repo = _imd_repo()
    from BusterNet.BusterNetCore import (
        create_BusterNet_testing_model,
        simple_cmfd_decoder,
        visualize_result,
    )

    model = create_BusterNet_testing_model(str(repo / "BusterNet" / "pretrained_busterNet.hd5"))
    return model, simple_cmfd_decoder, visualize_result


def _figure_png_base64(figure) -> str:
    buffer = io.BytesIO()
    figure.savefig(buffer, format="png", bbox_inches="tight", pad_inches=0)
    return base64.b64encode(buffer.getvalue()).decode("ascii")


def _png_base64(image) -> str:
    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    return base64.b64encode(buffer.getvalue()).decode("ascii")


def _normalize_mask(mask):
    # 모델 출력값은 이미지마다 범위가 다를 수 있으므로 0~1 사이 값으로 정규화한다.
    # 이렇게 맞춰야 같은 기준값으로 히트맵 색상과 의심 영역을 계산할 수 있다.
    _, np = _image_libs()
    mask = np.nan_to_num(mask, nan=0.0, posinf=1.0, neginf=0.0).astype("float32")
    if mask.size == 0:
        return mask

    low, high = np.percentile(mask, [1, 99])
    if high > low:
        mask = (mask - low) / (high - low)
    return np.clip(mask, 0.0, 1.0)


def _detect_face_boxes(source_image):
    # 얼굴 중심 분석을 위해 OpenCV의 기본 얼굴 검출기를 사용한다.
    # 얼굴 주변만 히트맵으로 강조하면 배경 노이즈가 결과에 섞이는 것을 줄일 수 있다.
    cv2, np = _image_libs()
    image_rgb = np.array(source_image.convert("RGB"))
    gray = cv2.cvtColor(image_rgb, cv2.COLOR_RGB2GRAY)
    gray = cv2.equalizeHist(gray)

    cascade_path = Path(cv2.data.haarcascades) / "haarcascade_frontalface_default.xml"
    detector = cv2.CascadeClassifier(str(cascade_path))
    if detector.empty():
        return []

    faces = detector.detectMultiScale(
        gray,
        scaleFactor=1.08,
        minNeighbors=4,
        minSize=(max(32, source_image.width // 12), max(32, source_image.height // 12)),
        flags=cv2.CASCADE_SCALE_IMAGE,
    )

    boxes = []
    for x, y, w, h in faces:
        pad_x = int(w * FACE_PADDING_RATIO)
        pad_y = int(h * FACE_PADDING_RATIO)
        left = max(0, int(x) - pad_x)
        top = max(0, int(y) - pad_y)
        right = min(source_image.width, int(x + w) + pad_x)
        bottom = min(source_image.height, int(y + h) + pad_y)
        if right > left and bottom > top:
            boxes.append((left, top, right, bottom))

    boxes.sort(key=lambda item: (item[2] - item[0]) * (item[3] - item[1]), reverse=True)
    return boxes


def _face_focus_mask(size, face_boxes):
    # 검출된 얼굴 박스만 1로 표시하는 마스크를 만든다.
    # 이후 히트맵과 곱해서 얼굴 밖 영역은 분석 표시에서 약하게 만든다.
    _, np = _image_libs()
    width, height = size
    focus = np.zeros((height, width), dtype="float32")
    if not face_boxes:
        return np.ones((height, width), dtype="float32")

    for left, top, right, bottom in face_boxes:
        focus[top:bottom, left:right] = 1.0
    return focus


def _odd_kernel_size(value: int, minimum: int = 3) -> int:
    value = max(minimum, int(value))
    return value if value % 2 == 1 else value + 1


def _remove_small_components(mask, min_area_ratio=MIN_REGION_AREA_RATIO):
    # 작은 점처럼 흩어진 노이즈는 실제 조작 흔적으로 보기 어렵다.
    # 연결된 영역 크기가 일정 비율보다 작은 부분은 제거한다.
    cv2, np = _image_libs()
    binary = (mask > 0).astype("uint8")
    if binary.size == 0:
        return binary

    component_count, labels, stats, _ = cv2.connectedComponentsWithStats(binary, 8)
    min_area = max(1, int(binary.shape[0] * binary.shape[1] * min_area_ratio))
    cleaned = np.zeros(binary.shape, dtype="uint8")
    for index in range(1, component_count):
        if stats[index, cv2.CC_STAT_AREA] >= min_area:
            cleaned[labels == index] = 1
    return cleaned


def _postprocess_mask(normalized):
    # 정규화된 마스크를 화면에 보여주기 좋은 형태로 후처리한다.
    # 기준값 적용, 작은 영역 제거, 블러 처리를 거쳐 자연스러운 히트맵을 만든다.
    cv2, np = _image_libs()
    if normalized.size == 0:
        return normalized

    thresholded = np.where(normalized >= HEATMAP_THRESHOLD, normalized, 0.0).astype("float32")
    if not np.any(thresholded):
        high_signal = float(np.percentile(normalized, 92))
        dynamic_threshold = max(0.08, high_signal)
        thresholded = np.where(normalized >= dynamic_threshold, normalized, 0.0).astype("float32")
    if not np.any(thresholded):
        return np.zeros_like(normalized, dtype="float32")

    open_kernel = _odd_kernel_size(HEATMAP_OPEN_KERNEL)
    kernel = np.ones((open_kernel, open_kernel), dtype="uint8")
    binary = (thresholded > 0).astype("uint8")
    opened = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel)
    cleaned = _remove_small_components(opened, MIN_REGION_AREA_RATIO)
    if not np.any(cleaned):
        cleaned = _remove_small_components(binary, max(0.0003, MIN_REGION_AREA_RATIO * 0.35))
    if not np.any(cleaned):
        cleaned = binary

    processed = thresholded * cleaned.astype("float32")
    blur_kernel = _odd_kernel_size(HEATMAP_BLUR_KERNEL)
    processed = cv2.GaussianBlur(processed, (blur_kernel, blur_kernel), 0)
    processed = processed * cleaned.astype("float32")

    high = float(processed.max())
    if high > 0:
        processed = processed / high
    return np.clip(processed, 0.0, 1.0).astype("float32")


def _strongest_component_contour(mask):
    # 가장 큰 의심 영역의 외곽선을 찾아 결과 화면에서 테두리로 표시한다.
    cv2, np = _image_libs()
    binary = (mask > 0).astype("uint8")
    if binary.size == 0 or not np.any(binary):
        return None

    contours, _ = cv2.findContours(binary * 255, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        return None
    return max(contours, key=cv2.contourArea)


def _mask_to_colored_image(mask, transparent_low=False, draw_contour=False):
    # 숫자 마스크를 사람이 보기 쉬운 컬러 히트맵 이미지로 변환한다.
    # transparent_low=True면 의심도가 낮은 부분은 투명하게 만들어 원본 위에 겹칠 수 있다.
    from PIL import Image

    cv2, np = _image_libs()
    colormap = getattr(cv2, "COLORMAP_TURBO", cv2.COLORMAP_INFERNO)
    colored_bgr = cv2.applyColorMap((mask * 255).astype("uint8"), colormap)
    colored_rgb = cv2.cvtColor(colored_bgr, cv2.COLOR_BGR2RGB)

    if transparent_low:
        alpha = np.where(mask > 0, np.clip(mask * 255 * 1.15, 0, 255), 0).astype("uint8")
        image_rgba = np.dstack([colored_rgb, alpha])
        if draw_contour:
            contour = _strongest_component_contour(mask)
            if contour is not None:
                cv2.drawContours(image_rgba, [contour], -1, (255, 244, 214, 230), 2)
        return Image.fromarray(image_rgba, mode="RGBA")

    return Image.fromarray(colored_rgb, mode="RGB")


def _mask_to_heatmap_assets(mask, source_image, overlay_alpha=None, face_boxes=None):
    # 모델 마스크 하나에서 세 가지 화면용 이미지를 만든다.
    # rawHeatmap: 원본 모델 출력, processedHeatmap: 노이즈 제거 결과,
    # overlayHeatmap: 원본 이미지 위에 히트맵을 얹은 결과다.
    from PIL import Image

    cv2, np = _image_libs()
    normalized = _normalize_mask(mask)
    if normalized.size == 0:
        normalized = np.zeros((source_image.height, source_image.width), dtype="float32")

    mask_image = Image.fromarray((normalized * 255).astype("uint8"), mode="L")
    if mask_image.size != source_image.size:
        mask_image = mask_image.resize(source_image.size, Image.Resampling.BILINEAR)
    resized_mask = np.array(mask_image).astype("float32") / 255.0
    if FACE_FOCUS_ENABLED:
        focus = _face_focus_mask(source_image.size, face_boxes or [])
        resized_mask = resized_mask * focus
    processed_mask = _postprocess_mask(resized_mask)

    raw_heatmap_image = _mask_to_colored_image(resized_mask, transparent_low=False)
    processed_heatmap_image = _mask_to_colored_image(processed_mask, transparent_low=True, draw_contour=True)

    alpha_value = overlay_alpha
    if alpha_value is None:
        alpha_value = HEATMAP_OVERLAY_ALPHA
    alpha_value = min(0.62, max(0.25, alpha_value))

    original = np.array(source_image.convert("RGB")).astype("float32")
    processed_rgb = np.array(processed_heatmap_image.convert("RGB")).astype("float32")
    weight = np.clip(processed_mask * 1.2, 0.0, 1.0)[..., None] * alpha_value
    heatmap_visible = np.repeat((processed_mask > 0)[..., None], 3, axis=2)
    heatmap = np.where(heatmap_visible, processed_rgb, original)
    overlay = (original * (1.0 - weight) + heatmap * weight).clip(0, 255).astype("uint8")
    contour = _strongest_component_contour(processed_mask)
    if contour is not None:
        cv2.drawContours(overlay, [contour], -1, (255, 244, 214), 2)
    overlay_image = Image.fromarray(overlay, mode="RGB")

    return raw_heatmap_image, processed_heatmap_image, overlay_image, resized_mask, processed_mask


def _run_mantranet_heatmap_assets(model, image_path: str, device: str):
    # 저장된 이미지를 ManTraNet에 넣고 히트맵 이미지와 마스크 데이터를 함께 반환한다.
    # Spring Boot 결과 화면은 여기서 만든 base64 PNG 데이터를 받아 표시한다.
    import torch
    from PIL import Image

    _, np = _image_libs()
    image = Image.open(image_path).convert("RGB")
    image_array = np.array(image)
    face_boxes = _detect_face_boxes(image) if FACE_FOCUS_ENABLED else []

    model.to(device)
    model.eval()

    tensor = torch.Tensor(image_array)
    tensor = tensor.unsqueeze(0)
    tensor = tensor.transpose(2, 3).transpose(1, 2)
    tensor = tensor.to(device)

    with torch.no_grad():
        output = model(tensor)

    mask = output[0][0].cpu().detach().numpy()
    raw_heatmap_image, processed_heatmap_image, overlay_image, resized_mask, processed_mask = _mask_to_heatmap_assets(
        mask,
        image,
        face_boxes=face_boxes,
    )

    return {
        "rawHeatmap": _png_base64(raw_heatmap_image),
        "processedHeatmap": _png_base64(processed_heatmap_image),
        "overlayHeatmap": _png_base64(overlay_image),
        "rawMask": resized_mask,
        "processedMask": processed_mask,
        "faceBoxes": face_boxes,
    }


def _png_to_array(data: str):
    cv2, np = _image_libs()
    raw = base64.b64decode(data)
    image = cv2.imdecode(np.frombuffer(raw, dtype=np.uint8), cv2.IMREAD_COLOR)
    if image is None:
        return None
    return image


def _score_and_regions_from_mask(mask):
    # 후처리된 히트맵 마스크를 이용해 최종 위험 점수와 의심 영역 좌표를 계산한다.
    # 좌표는 화면 크기가 달라도 맞게 표시되도록 0~1 비율값으로 내려준다.
    cv2, np = _image_libs()
    if mask is None or mask.size == 0:
        return 0.0, []

    binary = (mask > 0).astype("uint8") * 255
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    regions = []
    image_area = float(mask.shape[0] * mask.shape[1])
    active_area = 0.0
    for contour in contours:
        x, y, w, h = cv2.boundingRect(contour)
        area = float(w * h)
        if area < image_area * 0.003:
            continue
        active_area += area
        region_mask = mask[y:y + h, x:x + w]
        confidence = float(region_mask.max()) if region_mask.size else 0.0
        regions.append(
            {
                "x": round(x / mask.shape[1], 4),
                "y": round(y / mask.shape[0], 4),
                "width": round(w / mask.shape[1], 4),
                "height": round(h / mask.shape[0], 4),
                "label": "주변 픽셀 패턴과 다르게 감지된 영역입니다.",
                "confidence": round(min(1.0, max(0.0, confidence)), 4),
            }
        )

    score = min(1.0, max(0.0, float(mask.mean()) * 0.65 + (active_area / image_area) * 2.5))
    regions.sort(key=lambda item: item.get("confidence", 0), reverse=True)
    return round(score, 4), regions[:8]


def _normalized_face_boxes(face_boxes, width, height):
    # 얼굴 검출 박스도 화면에서 표시할 수 있도록 0~1 비율 좌표로 변환한다.
    if not face_boxes or width <= 0 or height <= 0:
        return []

    boxes = []
    for left, top, right, bottom in face_boxes:
        boxes.append(
            {
                "x": round(left / width, 4),
                "y": round(top / height, 4),
                "width": round((right - left) / width, 4),
                "height": round((bottom - top) / height, 4),
                "label": "Face focus area",
                "confidence": 1.0,
            }
        )
    return boxes


def _symptoms(score: float, regions):
    # 점수와 의심 영역 개수를 바탕으로 화면에 보여줄 간단한 설명 문구 목록을 만든다.
    symptoms = []
    if score >= 0.35:
        symptoms.append(
            {
                "label": "픽셀 패턴 이상 감지",
                "severity": "high" if score >= 0.7 else "medium",
                "description": "주변 픽셀 패턴과 다른 영역이 감지되었습니다.",
            }
        )
    if len(regions) >= 2:
        symptoms.append(
            {
                "label": "여러 픽셀 이상 영역 감지",
                "severity": "medium",
                "description": "서로 떨어진 여러 영역에서 픽셀 패턴 차이가 감지되었습니다.",
            }
        )
    if not symptoms:
        symptoms.append(
            {
                "label": "뚜렷한 국소 이상 없음",
                "severity": "low",
                "description": "IMD 특이점 보기에서 뚜렷한 픽셀 패턴 이상 영역은 확인되지 않았습니다.",
            }
        )
    return symptoms


def _normalize_image_for_mantranet(image_path: str) -> str:
    # ManTraNet 입력 안정성을 위해 이미지를 RGB JPEG로 변환하고 너무 큰 이미지는 축소한다.
    # 원본 임시 파일은 그대로 두고 모델 입력용 파일만 별도로 만든다.
    from PIL import Image

    with Image.open(image_path) as image:
        rgb = image.convert("RGB")
        max_size = max(256, IMD_MAX_IMAGE_SIZE)
        if max(rgb.size) > max_size:
            rgb.thumbnail((max_size, max_size), Image.Resampling.LANCZOS)
        normalized_path = f"{image_path}_rgb.jpg"
        rgb.save(normalized_path, format="JPEG", quality=95)
        return normalized_path


@app.get("/health")
def health():
    # Spring Boot의 ImdLocalServiceManager가 서버가 살아 있는지 확인할 때 호출한다.
    return {"status": "ok"}


@app.post("/analyze")
async def analyze(file: UploadFile = File(...)):
    # Spring Boot가 이미지를 multipart/form-data로 보내면 이 엔드포인트가 분석을 수행한다.
    # 응답은 verdict, score, 히트맵 base64, 의심 영역 좌표를 포함한 JSON이다.
    suffix = Path(file.filename or "image.png").suffix or ".png"
    image_path = None
    normalized_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as temp:
            temp.write(await file.read())
            image_path = temp.name

        mantranet_model, check_forgery, device = _load_mantranet()
        normalized_path = _normalize_image_for_mantranet(image_path)
        heatmap_assets = _run_mantranet_heatmap_assets(mantranet_model, normalized_path, device)
        score, regions = _score_and_regions_from_mask(heatmap_assets["processedMask"])

        verdict = "FAKE" if score >= 0.7 else "SUSPICIOUS" if score >= 0.35 else "REAL"
        response = {
            "provider": "imd",
            "modelName": "IMD ManTraNet",
            "verdict": verdict,
            "score": score,
            "rawHeatmap": {
                "type": "image/png",
                "data": heatmap_assets["rawHeatmap"],
            },
            "processedHeatmap": {
                "type": "image/png",
                "data": heatmap_assets["processedHeatmap"],
            },
            "overlayHeatmap": {
                "type": "image/png",
                "data": heatmap_assets["overlayHeatmap"],
            },
            "heatmap": {
                "type": "image/png",
                "data": heatmap_assets["rawHeatmap"],
            },
            "overlay": {
                "type": "image/png",
                "data": heatmap_assets["overlayHeatmap"],
            },
            "regions": regions,
            "faceFocus": {
                "enabled": FACE_FOCUS_ENABLED,
                "detected": len(heatmap_assets["faceBoxes"]) > 0,
                "paddingRatio": FACE_PADDING_RATIO,
                "regions": _normalized_face_boxes(
                    heatmap_assets["faceBoxes"],
                    heatmap_assets["processedMask"].shape[1],
                    heatmap_assets["processedMask"].shape[0],
                ),
            },
            "symptoms": _symptoms(score, regions),
            "explanation": "얼굴 영역 안에서 주변 픽셀 패턴과 다르게 감지된 영역입니다. 이 표시는 조작 확정이 아니라 참고용 시각화입니다.",
            "visualization": {
                "threshold": HEATMAP_THRESHOLD,
                "minRegionAreaRatio": MIN_REGION_AREA_RATIO,
                "colormap": "turbo",
                "defaultMode": "overlay",
                "scope": "face",
            },
        }

        if os.environ.get("IMD_ENABLE_BUSTERNET", "false").lower() == "true":
            cv2, _ = _image_libs()
            buster_model, simple_cmfd_decoder, visualize_result = _load_busternet()
            rgb = cv2.imread(image_path)
            pred = simple_cmfd_decoder(buster_model, rgb)
            buster_figure = visualize_result(rgb, pred, pred, figsize=(20, 20), title="BusterNet CMFD")
            response["busterNet"] = {
                "type": "image/png",
                "data": _figure_png_base64(buster_figure),
            }

        return response
    except Exception as exc:
        logger.exception("IMD analysis failed")
        raise HTTPException(status_code=500, detail="IMD analysis failed.") from exc
    finally:
        if normalized_path:
            try:
                os.remove(normalized_path)
            except Exception:
                pass
        if image_path:
            try:
                os.remove(image_path)
            except Exception:
                pass

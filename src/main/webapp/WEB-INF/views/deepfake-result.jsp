<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>분석 리포트 - DeepScan</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://unpkg.com/lucide@latest"></script>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
    <style>
        body {
            font-family: "Pretendard", "Noto Sans KR", sans-serif;
            background: #020617;
        }

        .report-shell {
            border: 1px solid rgba(148, 163, 184, 0.18);
            background: rgba(15, 23, 42, 0.78);
            box-shadow: 0 24px 80px rgba(2, 6, 23, 0.38);
        }

        .mode-tab {
            display: inline-flex;
            flex: 1 1 0;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            border-bottom: 2px solid transparent;
            padding: 0.95rem 0.55rem;
            color: #94a3b8;
            font-size: 0.875rem;
            font-weight: 700;
            transition: background-color 160ms ease, color 160ms ease, border-color 160ms ease;
        }

        .mode-tab:hover {
            background: rgba(255, 255, 255, 0.04);
            color: #f8fafc;
        }

        .mode-tab.active {
            border-color: #38bdf8;
            background: rgba(14, 165, 233, 0.18);
            color: #7dd3fc;
        }

        .analysis-stage {
            position: relative;
            display: flex;
            min-height: 360px;
            align-items: center;
            justify-content: center;
            overflow: hidden;
            border-radius: 1rem;
            background: #020617;
        }

        .analysis-image-wrap {
            position: relative;
            display: inline-block;
            max-width: 100%;
            line-height: 0;
        }

        .analysis-image-wrap img {
            display: block;
            max-height: 70vh;
            max-width: 100%;
            object-fit: contain;
            border-radius: 0.9rem;
        }

        .heatmap-overlay-image {
            position: absolute;
            inset: 0;
            width: 100%;
            height: 100%;
            object-fit: contain;
            opacity: 0.56;
            mix-blend-mode: screen;
            pointer-events: none;
        }

        .region-layer {
            position: absolute;
            inset: 0;
            pointer-events: none;
        }

        .region-box {
            position: absolute;
            border: 2px solid rgba(248, 113, 113, 0.96);
            border-radius: 0.75rem;
            background: rgba(248, 113, 113, 0.16);
            box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.12), 0 0 28px rgba(248, 113, 113, 0.28);
            pointer-events: auto;
        }

        .region-box::after {
            content: attr(data-label);
            position: absolute;
            left: 0;
            bottom: calc(100% + 0.5rem);
            max-width: 13rem;
            border: 1px solid rgba(56, 189, 248, 0.28);
            border-radius: 0.75rem;
            background: rgba(15, 23, 42, 0.96);
            padding: 0.42rem 0.62rem;
            color: #e0f2fe;
            font-size: 0.75rem;
            font-weight: 700;
            line-height: 1.15rem;
            opacity: 0;
            pointer-events: none;
            white-space: nowrap;
            transform: translateY(4px);
            transition: opacity 140ms ease, transform 140ms ease;
        }

        .region-box:hover::after {
            opacity: 1;
            transform: translateY(0);
        }

        .notice-box {
            border: 1px solid rgba(251, 191, 36, 0.22);
            background: rgba(251, 191, 36, 0.08);
            color: #fde68a;
        }

        .provider-badge {
            border: 1px solid rgba(56, 189, 248, 0.28);
            background: rgba(14, 165, 233, 0.12);
            color: #7dd3fc;
        }

        .detail-row {
            display: grid;
            grid-template-columns: 1fr 1.15fr 2fr auto;
            gap: 1rem;
            align-items: center;
            border: 1px solid rgba(148, 163, 184, 0.14);
            border-radius: 1rem;
            background: rgba(15, 23, 42, 0.72);
            padding: 1rem;
            transition: border-color 160ms ease, background-color 160ms ease;
        }

        .detail-row:hover {
            border-color: rgba(56, 189, 248, 0.28);
            background: rgba(30, 41, 59, 0.74);
        }

        .risk-badge {
            display: inline-flex;
            min-width: 4.25rem;
            justify-content: center;
            border-radius: 999px;
            border: 1px solid transparent;
            padding: 0.35rem 0.7rem;
            font-size: 0.75rem;
            font-weight: 800;
            white-space: nowrap;
        }

        .risk-high {
            border-color: rgba(248, 113, 113, 0.35);
            background: rgba(239, 68, 68, 0.16);
            color: #fca5a5;
        }

        .risk-medium {
            border-color: rgba(251, 146, 60, 0.35);
            background: rgba(249, 115, 22, 0.14);
            color: #fdba74;
        }

        .risk-low {
            border-color: rgba(96, 165, 250, 0.3);
            background: rgba(59, 130, 246, 0.12);
            color: #93c5fd;
        }

        .risk-unknown {
            border-color: rgba(148, 163, 184, 0.25);
            background: rgba(100, 116, 139, 0.14);
            color: #cbd5e1;
        }

        @media (max-width: 760px) {
            .detail-row {
                grid-template-columns: 1fr;
                gap: 0.55rem;
            }

            .detail-heading {
                display: none;
            }
        }
    </style>
</head>
<body class="min-h-screen bg-slate-950 text-white">
<script id="apiRawData" type="application/json"><c:out value="${safeApiRaw}" escapeXml="false"/></script>
<script id="analysisJsonData" type="application/json"><c:out value="${safeAnalysisJson}" escapeXml="false"/></script>

<div class="pointer-events-none fixed inset-0 overflow-hidden">
    <div id="topGlow" class="absolute left-1/4 top-20 h-96 w-96 rounded-full blur-3xl opacity-20"></div>
    <div class="absolute bottom-20 right-1/4 h-96 w-96 rounded-full bg-cyan-500 blur-3xl opacity-10"></div>
</div>

<main class="relative mx-auto max-w-7xl px-4 py-8">
    <section class="report-shell overflow-hidden rounded-3xl">
        <header class="flex items-start justify-between border-b border-white/10 px-6 py-5">
            <div>
                <h1 class="text-2xl font-bold text-white md:text-3xl">분석 리포트</h1>
                <p class="mt-2 max-w-xl break-all text-sm text-slate-400"><c:out value="${result.originalName}"/></p>
            </div>
            <div class="flex items-center gap-3">
                <span class="provider-badge hidden rounded-full px-3 py-1 text-xs font-bold sm:inline-flex">Reality Defender</span>
                <button type="button" onclick="goPage('<%= contextPath %>/history')" class="rounded-full p-2 text-slate-400 transition hover:bg-white/10 hover:text-white" aria-label="닫기">
                    <i data-lucide="x" class="h-6 w-6"></i>
                </button>
            </div>
        </header>

        <div class="grid gap-7 p-5 lg:grid-cols-[1.08fr_0.92fr] lg:p-7">
            <section class="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/60">
                <div class="flex border-b border-white/10">
                    <button id="originalTab" type="button" onclick="setViewMode('original')" class="mode-tab">
                        <i data-lucide="eye" class="h-4 w-4"></i>
                        원본
                    </button>
                    <button id="regionsTab" type="button" onclick="setViewMode('regions')" class="mode-tab">
                        <i data-lucide="scan-search" class="h-4 w-4"></i>
                        특이점 보기
                    </button>
                    <button id="overlayTab" type="button" onclick="setViewMode('overlay')" class="mode-tab active">
                        <i data-lucide="layers" class="h-4 w-4"></i>
                        오버레이
                    </button>
                </div>

                <div class="p-4">
                    <div class="analysis-stage">
                        <div id="imageWrap" class="analysis-image-wrap">
                            <img id="baseImage" src="<c:out value='${result.publicUrl}'/>" alt="<c:out value='${result.originalName}'/>">
                            <img id="heatmapImage" class="heatmap-overlay-image hidden" alt="IMD 히트맵 오버레이">
                            <div id="regionLayer" class="region-layer hidden"></div>
                        </div>
                    </div>

                    <div class="mt-4 rounded-2xl border border-white/10 bg-slate-800/40 p-4">
                        <div class="mb-2 flex items-center gap-2 text-sm font-semibold text-white">
                            <i data-lucide="activity" class="h-4 w-4 text-amber-300"></i>
                            참고용 시각화
                        </div>
                        <p id="visualDescription" class="text-sm leading-6 text-slate-300"></p>
                        <p class="mt-2 text-xs leading-5 text-slate-500">
                            IMD / ManTraNet 시각화는 최종 판정 기준이 아니라, 픽셀 패턴 차이를 확인하기 위한 참고 자료입니다.
                        </p>
                    </div>

                    <div id="visualNoticeBox" class="notice-box mt-4 hidden rounded-2xl p-4 text-sm leading-6"></div>
                </div>
            </section>

            <section class="space-y-5">
                <div class="relative">
                    <div id="verdictGlow" class="absolute inset-0 rounded-3xl blur-2xl opacity-25"></div>
                    <div class="relative rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                        <div class="mb-5 flex items-center justify-between gap-3">
                            <div class="flex items-center gap-4">
                                <div id="verdictIconWrap" class="rounded-2xl p-3"></div>
                                <div>
                                    <div class="mb-1 text-sm text-slate-400">최종 판정</div>
                                    <div id="verdictLabel" class="text-2xl font-bold"></div>
                                </div>
                            </div>
                            <span class="provider-badge rounded-full px-3 py-1 text-xs font-bold">Reality Defender</span>
                        </div>

                        <div class="mb-4 text-center">
                            <div class="text-6xl font-bold text-white">
                                <span id="scoreValue">0</span>
                                <span class="text-3xl text-slate-400">/100</span>
                            </div>
                            <div class="mt-2 text-sm text-slate-400">Reality Defender 최종 점수</div>
                        </div>

                        <div class="h-3 overflow-hidden rounded-full bg-slate-800">
                            <div id="scoreProgress" class="h-full transition-transform duration-700 ease-out"></div>
                        </div>
                        <div id="scoreHint" class="mt-3 text-xs text-slate-500"></div>
                    </div>
                </div>

                <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <h2 class="mb-3 text-lg font-semibold text-white">분석 설명</h2>
                    <p id="explanationText" class="mb-4 leading-7 text-slate-300"></p>
                    <div class="notice-box rounded-2xl p-4 text-sm leading-6">
                        이 결과는 조작 여부를 확정하는 값이 아닙니다. 최종 판정과 점수는 Reality Defender 결과를 기준으로 표시하며, 히트맵과 영역 표시는 픽셀 패턴 차이를 이해하기 위한 참고용 분석입니다.
                    </div>
                </div>

                <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <h2 class="mb-4 text-lg font-semibold text-white">상세 정보</h2>
                    <div class="space-y-3">
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">최종 분석 제공사</span>
                            <span id="apiProviderText" class="font-medium text-white">Reality Defender</span>
                        </div>
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">최종 분석 모델</span>
                            <span id="modelNameText" class="font-medium text-white">Reality Defender</span>
                        </div>
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">히트맵 시각화</span>
                            <span id="heatmapProviderText" class="font-medium text-white">IMD / ManTraNet</span>
                        </div>
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">표시 영역 수</span>
                            <span id="regionCountText" class="font-medium text-white">0개</span>
                        </div>
                        <div class="flex items-center justify-between border-b border-white/5 py-2">
                            <span class="text-sm text-slate-400">히트맵 상태</span>
                            <span id="heatmapStatusText" class="font-medium text-white">확인 중</span>
                        </div>
                        <div class="flex items-center justify-between py-2">
                            <span class="text-sm text-slate-400">분석 일시</span>
                            <span class="font-medium text-white"><c:out value="${result.regDt}"/></span>
                        </div>
                    </div>
                </div>

                <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
                    <h2 class="mb-4 text-lg font-semibold text-white">분석 상세 근거</h2>
                    <div class="detail-heading grid grid-cols-[1fr_1.15fr_2fr_auto] gap-4 px-4 pb-2 text-xs font-semibold text-slate-500">
                        <span>분석 항목</span>
                        <span>감지 결과</span>
                        <span>설명</span>
                        <span>위험도</span>
                    </div>
                    <div id="detailedAnalysisRows" class="space-y-3"></div>
                </div>

                <div class="grid grid-cols-2 gap-4">
                    <button type="button" onclick="goPage('<%= contextPath %>/history')" class="flex items-center justify-center gap-2 rounded-2xl border border-white/15 bg-white/5 px-6 py-4 font-semibold text-white transition hover:bg-white/10">
                        <i data-lucide="history" class="h-5 w-5"></i>
                        검증기록
                    </button>
                    <button id="shareButton" type="button" class="flex items-center justify-center gap-2 rounded-2xl bg-sky-500 px-6 py-4 font-semibold text-white transition hover:bg-sky-400">
                        <i id="shareIcon" data-lucide="share-2" class="h-5 w-5"></i>
                        <span id="shareLabel">결과 공유</span>
                    </button>
                </div>
            </section>
        </div>
    </section>
</main>

<script>
    const contextPath = "<%= contextPath %>";
    const verdict = "<c:out value='${result.verdict}'/>";
    const score = Number("<c:out value='${result.score}'/>");
    const resultId = "<c:out value='${result.id}'/>";
    const originalImageUrl = "<c:out value='${result.publicUrl}'/>";
    const apiProvider = "<c:out value='${result.apiProvider}'/>";
    const confidence = Number.isNaN(score) ? 0 : Math.round(Math.max(0, Math.min(score, 1)) * 100);

    const parsedApiRaw = parseJsonFromNode("apiRawData");
    const parsedAnalysis = parseJsonFromNode("analysisJsonData");
    const combinedResult = Object.assign({}, parsedApiRaw || {}, parsedAnalysis || {}, {
        verdict: verdict,
        score: score,
        publicUrl: originalImageUrl,
        apiProvider: apiProvider
    });

    const heatmapUrl = getHeatmapUrl(combinedResult);
    const anomalyRegions = getAnomalyRegions(combinedResult);

    const statusConfig = {
        SAFE: { label: "낮은 의심", glowClass: "bg-green-500", badgeClass: "text-green-300", icon: "check-circle", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500" },
        REAL: { label: "낮은 의심", glowClass: "bg-green-500", badgeClass: "text-green-300", icon: "check-circle", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500" },
        AUTHENTIC: { label: "낮은 의심", glowClass: "bg-green-500", badgeClass: "text-green-300", icon: "check-circle", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500" },
        SUSPECT: { label: "주의 필요", glowClass: "bg-yellow-500", badgeClass: "text-yellow-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500" },
        SUSPICIOUS: { label: "주의 필요", glowClass: "bg-yellow-500", badgeClass: "text-yellow-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500" },
        HIGH_RISK: { label: "추가 확인 필요", glowClass: "bg-red-500", badgeClass: "text-red-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-red-500 to-rose-500" },
        FAKE: { label: "추가 확인 필요", glowClass: "bg-red-500", badgeClass: "text-red-300", icon: "alert-triangle", progressClass: "bg-gradient-to-r from-red-500 to-rose-500" },
        NOT_APPLICABLE: { label: "분석 제한", glowClass: "bg-slate-500", badgeClass: "text-slate-300", icon: "ban", progressClass: "bg-gradient-to-r from-slate-500 to-slate-400" },
        UNABLE_TO_EVALUATE: { label: "분석 제한", glowClass: "bg-slate-500", badgeClass: "text-slate-300", icon: "ban", progressClass: "bg-gradient-to-r from-slate-500 to-slate-400" }
    };

    const fallbackStatusKey = confidence >= 70 ? "HIGH_RISK" : confidence >= 35 ? "SUSPICIOUS" : "SAFE";
    const statusKey = statusConfig[verdict] ? verdict : fallbackStatusKey;
    const config = statusConfig[statusKey];

    function parseJsonFromNode(id) {
        const node = document.getElementById(id);
        const text = node ? node.textContent.trim() : "";
        if (!text) return {};
        try {
            return JSON.parse(text);
        } catch (error) {
            return {};
        }
    }

    function goPage(path) {
        window.location.href = path;
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    function valueAt(data, path) {
        if (!data || typeof data !== "object") return null;
        return path.split(".").reduce(function(current, key) {
            return current && Object.prototype.hasOwnProperty.call(current, key) ? current[key] : null;
        }, data);
    }

    function firstValue(data, paths) {
        for (const path of paths) {
            const value = valueAt(data, path);
            if (value !== null && value !== undefined && value !== "") return value;
        }
        return null;
    }

    function normalizeImageUrl(url) {
        if (!url) return "";
        const value = String(url).trim();
        if (!value) return "";
        if (value.startsWith("data:") || value.startsWith("blob:") || value.startsWith("http://") || value.startsWith("https://")) return value;
        if (value.startsWith("//")) return window.location.protocol + value;
        if (value.startsWith("/")) return contextPath && !value.startsWith(contextPath + "/") ? contextPath + value : value;
        return contextPath + "/" + value.replace(/^\.?\//, "");
    }

    function normalizeImageData(value) {
        if (!value) return "";
        if (typeof value === "string") return normalizeImageUrl(value);
        if (typeof value !== "object") return "";
        if (value.url) return normalizeImageUrl(value.url);
        const data = String(value.data || value.base64 || "").trim();
        if (!data) return "";
        if (data.startsWith("data:")) return data;
        if (isImageUrlLike(data)) return normalizeImageUrl(data);
        return "data:" + (value.type || value.mimeType || "image/png") + ";base64," + data.replace(/\s/g, "");
    }

    function isImageUrlLike(value) {
        return value.startsWith("http://")
            || value.startsWith("https://")
            || value.startsWith("/")
            || value.startsWith("./")
            || value.startsWith("../")
            || value.startsWith("blob:");
    }

    function getHeatmapUrl(result) {
        const paths = [
            "heatmapUrl",
            "heatmap_url",
            "overlayUrl",
            "overlay_url",
            "imdHeatmapUrl",
            "imd_heatmap_url",
            "visualizationUrl",
            "visualization_url",
            "processedHeatmap",
            "overlayHeatmap",
            "rawHeatmap",
            "heatmap",
            "overlay",
            "imdHeatmapResult.heatmapUrl",
            "imdHeatmapResult.heatmap_url",
            "imdHeatmapResult.overlayUrl",
            "imdHeatmapResult.overlay_url",
            "imdHeatmapResult.imdHeatmapUrl",
            "imdHeatmapResult.imd_heatmap_url",
            "imdHeatmapResult.visualizationUrl",
            "imdHeatmapResult.visualization_url",
            "imdHeatmapResult.processedHeatmap",
            "imdHeatmapResult.overlayHeatmap",
            "imdHeatmapResult.rawHeatmap",
            "imdHeatmapResult.heatmap",
            "imdHeatmapResult.overlay"
        ];
        const value = firstValue(result, paths);
        return normalizeImageData(value);
    }

    function getAnomalyRegions(result) {
        const candidates = [
            firstValue(result, ["suspiciousRegions"]),
            firstValue(result, ["anomalyRegions"]),
            firstValue(result, ["regions"]),
            firstValue(result, ["boxes"]),
            firstValue(result, ["bboxes"]),
            firstValue(result, ["imdHeatmapResult.suspiciousRegions"]),
            firstValue(result, ["imdHeatmapResult.anomalyRegions"]),
            firstValue(result, ["imdHeatmapResult.regions"]),
            firstValue(result, ["imdHeatmapResult.boxes"]),
            firstValue(result, ["imdHeatmapResult.bboxes"]),
            firstValue(result, ["resultsSummary.suspiciousRegions"]),
            firstValue(result, ["resultsSummary.metadata.suspiciousRegions"])
        ];

        const source = candidates.find(Array.isArray) || [];
        return source.map(normalizeRegion).filter(function(region) {
            return region.width > 0 && region.height > 0;
        });
    }

    function normalizeRegion(region) {
        if (Array.isArray(region)) {
            return normalizeRegion({ x: region[0], y: region[1], width: region[2], height: region[3] });
        }

        const box = region && (region.bbox || region.box || region.bounds) || {};
        let x = Number(region?.x ?? region?.left ?? box.x ?? box.left ?? 0);
        let y = Number(region?.y ?? region?.top ?? box.y ?? box.top ?? 0);
        let width = Number(region?.width ?? region?.w ?? box.width ?? box.w ?? 0);
        let height = Number(region?.height ?? region?.h ?? box.height ?? box.h ?? 0);

        if ((!width || !height) && Number.isFinite(Number(region?.right)) && Number.isFinite(Number(region?.bottom))) {
            width = Number(region.right) - x;
            height = Number(region.bottom) - y;
        }

        const normalized = region?.normalized === true || (x <= 1 && y <= 1 && width <= 1 && height <= 1);
        const confidenceRaw = Number(region?.confidence ?? region?.score ?? region?.intensity ?? region?.weight ?? 0);
        const confidence = Number.isNaN(confidenceRaw) ? 0 : (confidenceRaw > 1 ? confidenceRaw / 100 : confidenceRaw);

        return {
            x: normalized ? clamp01(x) : x,
            y: normalized ? clamp01(y) : y,
            width: normalized ? clamp01(width) : width,
            height: normalized ? clamp01(height) : height,
            normalized: normalized,
            label: getKoreanRegionLabel(region),
            confidence: clamp01(confidence)
        };
    }

    function clamp01(value) {
        return Math.max(0, Math.min(1, Number(value) || 0));
    }

    function getKoreanRegionLabel(region) {
        const label = String(region?.label || region?.name || region?.type || "").toLowerCase();
        if (label.includes("eye")) return "눈 주변 픽셀 패턴 차이";
        if (label.includes("mouth")) return "입 주변 픽셀 패턴 차이";
        if (label.includes("skin") || label.includes("face")) return "얼굴 영역 픽셀 패턴 차이";
        if (label.includes("boundary") || label.includes("edge")) return "경계선 패턴 차이";
        return "픽셀 패턴 차이 영역";
    }

    function isHighRisk() {
        return ["HIGH_RISK", "FAKE"].includes(statusKey) || confidence >= 70;
    }

    function isMediumRisk() {
        return ["SUSPECT", "SUSPICIOUS"].includes(statusKey) || (confidence >= 35 && confidence < 70);
    }

    function getPrimaryRiskLevel() {
        if (isHighRisk()) return "높음";
        if (isMediumRisk()) return "중간";
        return "낮음";
    }

    function getSecondaryRiskLevel() {
        if (isHighRisk()) return confidence >= 85 ? "높음" : "중간";
        if (isMediumRisk()) return "중간";
        return "낮음";
    }

    function getDetailedAnalysisRows(result) {
        const hasHeatmap = Boolean(getHeatmapUrl(result));
        const primaryRisk = getPrimaryRiskLevel();
        const secondaryRisk = getSecondaryRiskLevel();

        return [
            {
                item: "픽셀 패턴",
                detected: primaryRisk === "낮음" ? "강한 차이는 크지 않음" : "주변 픽셀과 다른 패턴 감지",
                description: "얼굴 또는 주요 영역에서 주변 픽셀 패턴과 다른 변화가 감지될 수 있습니다.",
                risk: primaryRisk
            },
            {
                item: "경계선",
                detected: secondaryRisk === "낮음" ? "뚜렷한 경계 차이는 낮음" : "일부 경계선이 부자연스러움",
                description: "얼굴, 손, 물체 윤곽 등에서 색상이나 질감 변화가 갑자기 나타나는 영역을 참고합니다.",
                risk: secondaryRisk
            },
            {
                item: "질감",
                detected: secondaryRisk === "낮음" ? "질감 차이 신호 낮음" : "질감 분포 차이 감지",
                description: "피부, 배경, 옷감 등의 표면 패턴이 주변과 다르게 나타나는지 확인합니다.",
                risk: secondaryRisk
            },
            {
                item: "압축 흔적",
                detected: confidence >= 60 ? "압축 패턴 차이 가능성" : "강한 압축 차이는 낮음",
                description: "이미지 일부 영역에서 압축 또는 재저장 과정에서 생길 수 있는 패턴 차이를 참고합니다.",
                risk: confidence >= 60 ? "중간" : "낮음"
            },
            {
                item: "색상/조명",
                detected: secondaryRisk === "낮음" ? "색상 또는 밝기 차이 낮음" : "색상 또는 밝기 차이 감지",
                description: "특정 영역의 밝기, 색감, 그림자 패턴이 주변과 다르게 보일 수 있는지 확인합니다.",
                risk: secondaryRisk
            },
            {
                item: "히트맵 반응",
                detected: hasHeatmap ? "특정 영역에서 반응 확인" : "분석 데이터 없음",
                description: hasHeatmap
                    ? "IMD 히트맵에서 주변 픽셀 패턴과 다르게 감지된 영역이 강조되었습니다."
                    : "현재 이미지에 대해 표시 가능한 IMD 히트맵 데이터가 제공되지 않았습니다.",
                risk: hasHeatmap ? primaryRisk : "정보 없음"
            }
        ];
    }

    function getRiskBadgeClass(level) {
        if (level === "높음") return "risk-high";
        if (level === "중간") return "risk-medium";
        if (level === "낮음") return "risk-low";
        return "risk-unknown";
    }

    function buildDetailedDescription() {
        if (["NOT_APPLICABLE", "UNABLE_TO_EVALUATE"].includes(statusKey)) {
            return "현재 이미지는 분석 기준을 충분히 만족하지 못해 결과 해석에 제한이 있습니다. Reality Defender 응답을 기준으로 최종 상태를 표시했으며, 히트맵 또는 영역 데이터가 제공된 경우에만 참고 시각화를 표시합니다. 가능한 경우 더 선명한 원본 이미지와 출처 정보를 함께 확인하는 것이 좋습니다.";
        }

        if (isHighRisk()) {
            return "분석 결과, 이미지 내 일부 영역에서 비정상적인 패턴이 감지되었습니다. 이 결과는 조작 여부를 확정하는 것이 아니라, 추가 확인이 필요한 참고 신호로 볼 수 있습니다. 특히 히트맵에서 강조되는 부분과 픽셀 패턴 차이가 나타나는 영역을 중심으로 원본 여부를 함께 검토하는 것이 좋습니다.";
        }

        if (isMediumRisk()) {
            return "분석 결과, 일부 영역에서 주변과 다른 픽셀 패턴 차이가 관찰될 수 있습니다. 다만 이 결과만으로 조작 여부를 단정할 수 없으며, 추가 확인이 필요한 참고용 신호로 해석하는 것이 적절합니다. 원본 출처, 촬영 맥락, 재저장 여부를 함께 확인하는 것이 좋습니다.";
        }

        return "분석 결과, 강한 조작 의심 신호는 크지 않습니다. 다만 AI 분석은 참고용 결과이므로 원본 출처와 촬영 맥락을 함께 확인하는 것이 좋습니다. 히트맵이나 영역 데이터가 제공된 경우에도 최종 판정 기준은 Reality Defender 결과를 우선합니다.";
    }

    function renderDetailedAnalysisRows(rows) {
        const container = document.getElementById("detailedAnalysisRows");
        container.innerHTML = rows.map(function(row) {
            return ""
                + '<div class="detail-row">'
                + '  <div class="font-semibold text-white">' + escapeHtml(row.item) + '</div>'
                + '  <div class="text-sm text-slate-200">' + escapeHtml(row.detected) + '</div>'
                + '  <div class="text-sm leading-6 text-slate-400">' + escapeHtml(row.description) + '</div>'
                + '  <div><span class="risk-badge ' + getRiskBadgeClass(row.risk) + '">' + escapeHtml(row.risk) + '</span></div>'
                + '</div>';
        }).join("");
    }

    function showNotice(message) {
        const box = document.getElementById("visualNoticeBox");
        box.textContent = message;
        box.classList.remove("hidden");
    }

    function hideNotice() {
        const box = document.getElementById("visualNoticeBox");
        box.textContent = "";
        box.classList.add("hidden");
    }

    function renderRegionBoxes(show) {
        const layer = document.getElementById("regionLayer");
        const image = document.getElementById("baseImage");
        layer.innerHTML = "";
        layer.classList.toggle("hidden", !show || anomalyRegions.length === 0);
        if (!show || anomalyRegions.length === 0) return;

        anomalyRegions.forEach(function(region) {
            const box = document.createElement("div");
            box.className = "region-box";
            box.dataset.label = region.label;

            const left = region.normalized ? region.x * 100 : region.x / Math.max(1, image.naturalWidth) * 100;
            const top = region.normalized ? region.y * 100 : region.y / Math.max(1, image.naturalHeight) * 100;
            const width = region.normalized ? region.width * 100 : region.width / Math.max(1, image.naturalWidth) * 100;
            const height = region.normalized ? region.height * 100 : region.height / Math.max(1, image.naturalHeight) * 100;

            box.style.left = left + "%";
            box.style.top = top + "%";
            box.style.width = width + "%";
            box.style.height = height + "%";
            layer.appendChild(box);
        });
    }

    function renderHeatmapOverlay() {
        const heatmapImage = document.getElementById("heatmapImage");
        if (!heatmapUrl) {
            heatmapImage.classList.add("hidden");
            heatmapImage.removeAttribute("src");
            document.getElementById("heatmapStatusText").textContent = "데이터 없음";
            showNotice("IMD 히트맵 데이터가 없습니다. 현재는 참고 가능한 히트맵 시각화가 제공되지 않았습니다.");
            return false;
        }

        heatmapImage.src = heatmapUrl;
        heatmapImage.classList.remove("hidden");
        document.getElementById("heatmapStatusText").textContent = "표시 가능";
        return true;
    }

    function setViewMode(mode) {
        const baseImage = document.getElementById("baseImage");
        const heatmapImage = document.getElementById("heatmapImage");
        const regionLayer = document.getElementById("regionLayer");
        const tabs = {
            original: document.getElementById("originalTab"),
            regions: document.getElementById("regionsTab"),
            overlay: document.getElementById("overlayTab")
        };
        Object.keys(tabs).forEach(function(key) {
            tabs[key].classList.toggle("active", key === mode);
        });

        baseImage.src = originalImageUrl;
        heatmapImage.classList.add("hidden");
        heatmapImage.removeAttribute("src");
        heatmapImage.style.opacity = "0";
        regionLayer.classList.add("hidden");
        regionLayer.innerHTML = "";
        renderRegionBoxes(false);
        hideNotice();

        if (mode === "original") {
            document.getElementById("visualDescription").textContent = "원본 이미지를 기준으로 Reality Defender의 최종 분석 결과를 함께 확인합니다.";
            return;
        }

        if (mode === "regions") {
            document.getElementById("visualDescription").textContent = "응답에 포함된 세부 의심 영역 좌표를 원본 이미지 위에 표시합니다.";
            renderRegionBoxes(true);
            if (anomalyRegions.length === 0) {
                showNotice("표시 가능한 세부 의심 영역이 없습니다. 이 경우 오버레이 탭의 전체 히트맵을 참고해주세요.");
            }
            return;
        }

        document.getElementById("visualDescription").textContent = "원본 이미지 위에 IMD / ManTraNet 히트맵을 반투명하게 겹쳐 픽셀 패턴 차이를 참고할 수 있습니다.";
        heatmapImage.style.opacity = "0.56";
        renderHeatmapOverlay();
    }

    function setTemporaryShareState(copied) {
        document.getElementById("shareIcon").setAttribute("data-lucide", copied ? "check" : "share-2");
        document.getElementById("shareLabel").textContent = copied ? "링크 복사됨" : "결과 공유";
        lucide.createIcons();
    }

    function legacyCopyToClipboard(text) {
        const textArea = document.createElement("textarea");
        textArea.value = text;
        textArea.style.position = "fixed";
        textArea.style.left = "-999999px";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        try {
            document.execCommand("copy");
            setTemporaryShareState(true);
            setTimeout(function() { setTemporaryShareState(false); }, 2000);
        } finally {
            document.body.removeChild(textArea);
        }
    }

    document.getElementById("topGlow").classList.add(config.glowClass);
    document.getElementById("verdictGlow").classList.add(config.glowClass);
    document.getElementById("verdictIconWrap").innerHTML = '<i data-lucide="' + config.icon + '" class="h-6 w-6 ' + config.badgeClass + '"></i>';
    document.getElementById("verdictLabel").className = "text-2xl font-bold " + config.badgeClass;
    document.getElementById("verdictLabel").textContent = config.label;
    document.getElementById("scoreValue").textContent = String(confidence);
    document.getElementById("scoreProgress").className = "h-full transition-transform duration-700 ease-out " + config.progressClass;
    document.getElementById("scoreProgress").style.transform = "translateX(-" + (100 - confidence) + "%)";
    document.getElementById("scoreHint").textContent =
        confidence >= 85 ? "Reality Defender 기준으로 강한 조작 의심 신호가 감지된 상태입니다." :
        confidence >= 45 ? "Reality Defender 기준으로 추가 확인이 필요한 신호가 있습니다." :
        "Reality Defender 기준으로 강한 조작 의심 신호는 크지 않습니다.";
    document.getElementById("explanationText").textContent = buildDetailedDescription();
    document.getElementById("apiProviderText").textContent = apiProvider || "Reality Defender";
    document.getElementById("heatmapProviderText").textContent = firstValue(combinedResult, ["imdHeatmapResult.modelName", "heatmapModelName"]) || "IMD / ManTraNet";
    document.getElementById("regionCountText").textContent = anomalyRegions.length + "개";
    document.getElementById("heatmapStatusText").textContent = heatmapUrl ? "표시 가능" : "데이터 없음";

    document.getElementById("baseImage").addEventListener("load", function() {
        const activeMode = document.querySelector(".mode-tab.active")?.id === "regionsTab" ? "regions" : null;
        if (activeMode) renderRegionBoxes(true);
    });

    document.getElementById("heatmapImage").addEventListener("error", function() {
        document.getElementById("heatmapStatusText").textContent = "로딩 실패";
        this.classList.add("hidden");
        showNotice("히트맵 이미지를 불러오지 못했습니다. 파일 경로 또는 응답 필드를 확인해주세요.");
    });

    document.getElementById("shareButton").addEventListener("click", function() {
        const shareUrl = window.location.origin + contextPath + "/detail/" + resultId;
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(shareUrl).then(function() {
                setTemporaryShareState(true);
                setTimeout(function() { setTemporaryShareState(false); }, 2000);
            }).catch(function() {
                legacyCopyToClipboard(shareUrl);
            });
        } else {
            legacyCopyToClipboard(shareUrl);
        }
    });

    renderDetailedAnalysisRows(getDetailedAnalysisRows(combinedResult));
    setViewMode("overlay");
    lucide.createIcons();
</script>
</body>
</html>

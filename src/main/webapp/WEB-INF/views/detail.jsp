<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
  String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>분석 상세 - DeepScan</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
  <style>
    body {
      font-family: "Pretendard", "Noto Sans KR", sans-serif;
      background: #020617;
    }
    .analysis-overlay rect {
      fill: rgba(248, 113, 113, 0.12);
      stroke: rgba(248, 113, 113, 0.92);
      stroke-width: 3;
      vector-effect: non-scaling-stroke;
      rx: 16;
      ry: 16;
    }
    .analysis-overlay text {
      font-family: "Pretendard", "Noto Sans KR", sans-serif;
      fill: #f8fafc;
      font-size: 22px;
      font-weight: 700;
      paint-order: stroke;
      stroke: rgba(15, 23, 42, 0.96);
      stroke-width: 6;
      stroke-linejoin: round;
    }
  </style>
</head>
<body class="min-h-screen bg-slate-950 text-white">
  <div class="pointer-events-none fixed inset-0 overflow-hidden">
    <div id="topGlow" class="absolute left-1/4 top-16 h-80 w-80 rounded-full blur-3xl opacity-20"></div>
    <div class="absolute bottom-10 right-1/4 h-80 w-80 rounded-full bg-cyan-500 blur-3xl opacity-10"></div>
  </div>

  <div class="relative mx-auto max-w-6xl px-4 pb-16 pt-8">
    <div class="mb-8 flex items-center justify-between">
      <button
        type="button"
        onclick="location.href='<%= contextPath %>/history'"
        class="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-4 py-2 text-sm text-slate-200 transition hover:bg-white/10"
      >
        <i data-lucide="arrow-left" class="h-4 w-4"></i>
        분석 기록으로
      </button>
      <button
        id="shareButton"
        type="button"
        class="inline-flex items-center gap-2 rounded-full bg-sky-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-sky-400"
      >
        <i id="shareIcon" data-lucide="share-2" class="h-4 w-4"></i>
        <span id="shareLabel">결과 공유</span>
      </button>
    </div>

    <div class="grid gap-8 lg:grid-cols-[1.15fr_0.85fr]">
      <section class="space-y-6">
        <div class="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/60 shadow-2xl shadow-slate-950/50 backdrop-blur-xl">
          <div id="analysisImageWrap" class="relative mx-auto">
            <img
              id="analysisImage"
              src="<c:out value='${result.publicUrl}'/>"
              alt="<c:out value='${result.originalName}'/>"
              class="block h-auto w-full bg-slate-950"
            >
            <svg id="regionOverlay" class="analysis-overlay pointer-events-none absolute inset-0 hidden h-full w-full"></svg>
          </div>
        </div>

        <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-5 backdrop-blur-xl">
          <div class="mb-2 flex items-center gap-2 text-slate-200">
            <i data-lucide="scan-search" class="h-5 w-5 text-sky-300"></i>
            <span class="font-semibold">영역 표시</span>
          </div>
          <p id="overlayInfoText" class="text-sm leading-6 text-slate-300">
            응답에 좌표 정보가 포함된 경우에만 분석 영역을 원본 이미지 위에 표시합니다.
          </p>
        </div>
      </section>

      <section class="space-y-6">
        <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
          <div class="mb-4 flex items-start justify-between gap-4">
            <div>
              <p class="mb-2 text-sm text-slate-400">파일명</p>
              <h1 class="break-all text-2xl font-bold leading-tight text-white"><c:out value="${result.originalName}"/></h1>
            </div>
            <div class="flex flex-col items-end gap-2">
              <c:if test="${showAnalysisSourceBadge}">
                <span id="analysisSourceBadge" class="hidden rounded-full border px-3 py-1 text-xs font-semibold"></span>
              </c:if>
              <span id="statusBadge" class="rounded-full border px-4 py-2 text-sm font-semibold"></span>
            </div>
          </div>

          <div class="mb-3 flex items-center justify-between">
            <span class="text-sm text-slate-400">조작 판단도</span>
            <span id="scoreText" class="text-3xl font-bold text-white">-</span>
          </div>
          <div class="h-3 overflow-hidden rounded-full bg-slate-800">
            <div id="progressIndicator" class="h-full transition-transform duration-500 ease-out"></div>
          </div>
          <p id="scoreHint" class="mt-3 text-xs text-slate-500"></p>
        </div>

        <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
          <h2 class="mb-3 text-lg font-semibold text-white">분석 결과</h2>
          <p id="explanationText" class="leading-7 text-slate-300"></p>
          <p id="regionSummaryText" class="mt-4 text-sm leading-6 text-slate-400"></p>
          <div class="mt-4 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
            <p id="analysisDisclaimer" class="text-sm leading-6 text-slate-300"></p>
          </div>
        </div>

        <div class="rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
          <h2 class="mb-4 text-lg font-semibold text-white">상세 정보</h2>
          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <p class="mb-1 text-xs text-slate-500">API 제공처</p>
              <p id="apiProviderText" class="font-medium text-white">-</p>
            </div>
            <div>
              <p class="mb-1 text-xs text-slate-500">분석 모델</p>
              <p id="modelNameText" class="font-medium text-white">AI Image Detection</p>
            </div>
            <div>
              <p class="mb-1 text-xs text-slate-500">분석 일시</p>
              <p class="font-medium text-white"><c:out value="${result.regDt}"/></p>
            </div>
            <div>
              <p class="mb-1 text-xs text-slate-500">MIME 타입</p>
              <p class="font-medium text-white"><c:out value="${result.mimeType}"/></p>
            </div>
            <div>
              <p class="mb-1 text-xs text-slate-500">파일 크기</p>
              <p id="fileSizeText" class="font-medium text-white">-</p>
            </div>
            <div>
              <p class="mb-1 text-xs text-slate-500">위치 데이터</p>
              <p id="locationDataText" class="font-medium text-white">확인 중</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>

  <script id="apiRawData" type="application/json"><c:out value="${result.apiRaw}"/></script>
  <script id="analysisJsonData" type="application/json"><c:out value="${result.analysisJson}"/></script>
  <script>
    const contextPath = "<%= contextPath %>";
    const showAnalysisSourceBadge = ${showAnalysisSourceBadge ? "true" : "false"};
    const verdict = "<c:out value='${result.verdict}'/>";
    const score = Number("<c:out value='${result.score}'/>");
    const resultId = "<c:out value='${result.id}'/>";
    const apiProvider = "<c:out value='${result.apiProvider}'/>";
    const fileSizeValue = Number("<c:out value='${result.fileSize}'/>");
    const rawResponseText = document.getElementById("apiRawData").textContent || "";
    const analysisJsonText = document.getElementById("analysisJsonData").textContent || "";

    const unavailableVerdicts = ["NOT_APPLICABLE", "UNABLE_TO_EVALUATE"];
    const isUnavailableVerdict = unavailableVerdicts.includes(verdict);
    const scorePercent = Number.isNaN(score) || isUnavailableVerdict ? null : Math.round(Math.max(0, Math.min(score, 1)) * 100);

    const statusMap = {
      SAFE: {
        label: "안전",
        badgeClass: "border-green-500/30 bg-green-500/10 text-green-300",
        progressClass: "bg-gradient-to-r from-green-500 to-emerald-500",
        glowClass: "bg-green-500",
        text: "조작 가능성이 낮게 감지되었습니다."
      },
      AUTHENTIC: {
        label: "안전",
        badgeClass: "border-green-500/30 bg-green-500/10 text-green-300",
        progressClass: "bg-gradient-to-r from-green-500 to-emerald-500",
        glowClass: "bg-green-500",
        text: "조작 가능성이 낮게 감지되었습니다."
      },
      SUSPECT: {
        label: "주의",
        badgeClass: "border-yellow-500/30 bg-yellow-500/10 text-yellow-300",
        progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500",
        glowClass: "bg-yellow-500",
        text: "조작 또는 과한 보정으로 해석될 수 있는 신호가 감지되었습니다."
      },
      SUSPICIOUS: {
        label: "주의",
        badgeClass: "border-yellow-500/30 bg-yellow-500/10 text-yellow-300",
        progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500",
        glowClass: "bg-yellow-500",
        text: "조작 또는 과한 보정으로 해석될 수 있는 신호가 감지되었습니다."
      },
      HIGH_RISK: {
        label: "고위험",
        badgeClass: "border-red-500/30 bg-red-500/10 text-red-300",
        progressClass: "bg-gradient-to-r from-red-500 to-rose-500",
        glowClass: "bg-red-500",
        text: "조작 가능성이 높게 감지되었습니다."
      },
      FAKE: {
        label: "고위험",
        badgeClass: "border-red-500/30 bg-red-500/10 text-red-300",
        progressClass: "bg-gradient-to-r from-red-500 to-rose-500",
        glowClass: "bg-red-500",
        text: "조작 가능성이 높게 감지되었습니다."
      }
    };

    statusMap.REAL = statusMap.AUTHENTIC;
    statusMap.NOT_APPLICABLE = {
      label: "분석 불가",
      badgeClass: "border-slate-500/40 bg-slate-500/10 text-slate-200",
      progressClass: "bg-slate-500",
      glowClass: "bg-slate-500",
      text: "이 이미지는 신뢰할 수 있는 딥페이크 분석에 적합하지 않습니다."
    };
    statusMap.UNABLE_TO_EVALUATE = statusMap.NOT_APPLICABLE;

    const fallbackStatus = scorePercent === null
      ? statusMap.NOT_APPLICABLE
      : scorePercent >= 85
        ? statusMap.HIGH_RISK
        : scorePercent >= 45
          ? statusMap.SUSPECT
          : statusMap.SAFE;
    const status = statusMap[verdict] || fallbackStatus;

    let parsedRaw = null;
    let parsedAnalysis = null;

    try {
      parsedRaw = rawResponseText ? JSON.parse(rawResponseText) : null;
    } catch (error) {
      parsedRaw = null;
    }

    try {
      parsedAnalysis = analysisJsonText ? JSON.parse(analysisJsonText) : null;
    } catch (error) {
      parsedAnalysis = null;
    }

    function resolveAnalysisSource(data) {
      if (!data || typeof data !== "object") {
        return {
          key: "unknown",
          label: "분석 원본 미확인",
          className: "border-slate-500/30 bg-slate-500/10 text-slate-300"
        };
      }
      if (data.source === "dummy" || data.provider === "dummy" || data.mode === "offline") {
        return {
          key: "dummy",
          label: "더미 결과",
          className: "border-amber-500/30 bg-amber-500/10 text-amber-200"
        };
      }
      if (data.source === "preflight" || data.provider === "preflight") {
        return {
          key: "preflight",
          label: "이미지 사전 검사",
          className: "border-slate-500/40 bg-slate-500/10 text-slate-200"
        };
      }
      if (data.source === "real" || data.resultsSummary || data.requestId || data.mediaId) {
        return {
          key: "real",
          label: "실제 API 결과",
          className: "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
        };
      }
      return {
        key: "unknown",
        label: "분석 원본 미확인",
        className: "border-slate-500/30 bg-slate-500/10 text-slate-300"
      };
    }

    function formatFileSize(size) {
      if (Number.isNaN(size) || size <= 0) {
        return "-";
      }
      if (size >= 1024 * 1024) {
        return (size / (1024 * 1024)).toFixed(2) + " MB";
      }
      if (size >= 1024) {
        return (size / 1024).toFixed(1) + " KB";
      }
      return size + " B";
    }

    function summarizeRegions(regions) {
      if (isUnavailableVerdict) {
        return "얼굴이 너무 작거나, 사람이 너무 많거나, 사람 얼굴이 아니거나, 이미지가 흐릿해 분석 기준을 충족하지 못했습니다.";
      }

      if (!Array.isArray(regions) || !regions.length) {
        return "응답에 위치 데이터가 없어 점수와 설명만 표시합니다.";
      }

      const labels = regions
        .map(function (region) { return region && region.label ? String(region.label).trim() : ""; })
        .filter(function (label) { return !!label; });

      if (!labels.length) {
        return "좌표 정보는 확인되었지만 표시 가능한 라벨 정보는 없습니다.";
      }

      const uniqueLabels = Array.from(new Set(labels));
      return "응답에 포함된 주요 영역: " + uniqueLabels.join(", ");
    }

    function valueAt(data, path) {
      if (!data || typeof data !== "object") {
        return null;
      }

      return path.split(".").reduce(function (current, key) {
        return current && Object.prototype.hasOwnProperty.call(current, key) ? current[key] : null;
      }, data);
    }

    function firstTextValue(data, paths) {
      for (const path of paths) {
        const value = valueAt(data, path);
        if (typeof value === "string" && value.trim()) {
          return value.trim();
        }
      }
      return null;
    }

    function resolveUnavailableReason(data) {
      const apiReason = firstTextValue(data, [
        "resultsSummary.reason",
        "resultsSummary.metadata.reason",
        "reason",
        "message",
        "data.reason",
        "error.message"
      ]);

      return apiReason || "얼굴이 너무 작거나, 사람이 너무 많거나, 사람 얼굴이 아니거나, 이미지가 흐릿해 신뢰할 수 있는 분석을 진행할 수 없습니다.";
    }

    function toAbsoluteRect(region, imageWidth, imageHeight) {
      const x = Number(region.x || 0);
      const y = Number(region.y || 0);
      const width = Number(region.width || 0);
      const height = Number(region.height || 0);
      const normalized = region.normalized === true;

      if (normalized) {
        return {
          x: x * imageWidth,
          y: y * imageHeight,
          width: width * imageWidth,
          height: height * imageHeight,
          label: region.label || "",
          confidence: region.confidence
        };
      }

      return {
        x: x,
        y: y,
        width: width,
        height: height,
        label: region.label || "",
        confidence: region.confidence
      };
    }

    function renderRegionOverlay(imageElement, regions) {
      const overlay = document.getElementById("regionOverlay");
      if (!imageElement || !overlay) {
        return;
      }

      if (!Array.isArray(regions) || !regions.length || !imageElement.naturalWidth || !imageElement.naturalHeight) {
        overlay.classList.add("hidden");
        overlay.innerHTML = "";
        return;
      }

      const width = imageElement.naturalWidth;
      const height = imageElement.naturalHeight;
      overlay.setAttribute("viewBox", "0 0 " + width + " " + height);

      const fragments = regions.map(function (region) {
        const rect = toAbsoluteRect(region, width, height);
        if (rect.width <= 0 || rect.height <= 0) {
          return "";
        }

        const label = rect.label
          ? '<text x="' + Math.max(12, rect.x + 12) + '" y="' + Math.max(26, rect.y - 10) + '">' + rect.label + '</text>'
          : "";

        return ''
          + '<rect x="' + rect.x + '" y="' + rect.y + '" width="' + rect.width + '" height="' + rect.height + '"></rect>'
          + label;
      }).join("");

      overlay.innerHTML = fragments;
      overlay.classList.toggle("hidden", !fragments);
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
        setTimeout(function () { setTemporaryShareState(false); }, 2000);
      } finally {
        document.body.removeChild(textArea);
      }
    }

    const analysisData = parsedAnalysis || { source: "unknown", locationMode: "none", regions: [] };
    const analysisSource = resolveAnalysisSource(analysisData.source ? analysisData : parsedRaw);
    const regions = Array.isArray(analysisData.regions) ? analysisData.regions : [];

    document.getElementById("statusBadge").className = "rounded-full border px-4 py-2 text-sm font-semibold " + status.badgeClass;
    document.getElementById("statusBadge").textContent = status.label;
    document.getElementById("scoreText").textContent = scorePercent === null ? "-" : scorePercent + "%";
    document.getElementById("progressIndicator").className = "h-full " + status.progressClass;
    document.getElementById("progressIndicator").style.transform = "translateX(-" + (100 - (scorePercent || 0)) + "%)";
    document.getElementById("scoreHint").textContent =
      scorePercent === null ? "판정 점수를 확인할 수 없습니다." :
      scorePercent >= 85 ? "강한 조작 신호가 관찰되었습니다." :
      scorePercent >= 60 ? "중간 이상 수준의 조작 신호가 관찰되었습니다." :
      scorePercent >= 45 ? "약한 조작 신호가 관찰되었습니다." :
      "조작 신호가 낮게 감지되었습니다.";
    if (isUnavailableVerdict) {
      document.getElementById("scoreHint").textContent = "분석 기준을 충족하지 못해 신뢰도 점수를 제공하지 않습니다.";
      document.getElementById("explanationText").textContent = resolveUnavailableReason(parsedRaw);
    } else {
      document.getElementById("explanationText").textContent = status.text;
    }
    document.getElementById("regionSummaryText").textContent = summarizeRegions(regions);
    document.getElementById("analysisDisclaimer").textContent =
      "참고 안내: 이 결과는 원본 여부를 확정하는 값이 아니라 이미지에서 감지된 조작 신호를 보여주는 참고용 분석입니다. " +
      "압축, 보정, 캡처, 기사 이미지, 재업로드 이미지에서도 유사한 패턴이 발생할 수 있습니다.";
    document.getElementById("apiProviderText").textContent = apiProvider || "-";
    document.getElementById("modelNameText").textContent =
      analysisSource.key === "dummy" ? "Dummy Analyzer" :
      analysisSource.key === "preflight" ? "Image Suitability Check" :
      "Reality Defender";
    document.getElementById("fileSizeText").textContent = formatFileSize(fileSizeValue);
    document.getElementById("locationDataText").textContent = regions.length ? "좌표 정보 있음" : "좌표 정보 없음";
    document.getElementById("overlayInfoText").textContent = regions.length
      ? "응답에 포함된 좌표 정보를 기준으로 분석 영역을 표시했습니다."
      : "현재 분석 결과에는 위치 데이터가 없어 원본 이미지와 점수, 설명만 표시합니다.";
    document.getElementById("topGlow").classList.add(status.glowClass);

    if (showAnalysisSourceBadge) {
      const sourceBadge = document.getElementById("analysisSourceBadge");
      if (sourceBadge) {
        sourceBadge.className = "rounded-full border px-3 py-1 text-xs font-semibold " + analysisSource.className;
        sourceBadge.textContent = analysisSource.label;
        sourceBadge.classList.remove("hidden");
      }
    }

    const imageElement = document.getElementById("analysisImage");
    if (imageElement.complete) {
      renderRegionOverlay(imageElement, regions);
    } else {
      imageElement.addEventListener("load", function () {
        renderRegionOverlay(imageElement, regions);
      });
    }

    document.getElementById("shareButton").addEventListener("click", function () {
      const shareUrl = window.location.origin + contextPath + "/detail/" + resultId;
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(shareUrl).then(function () {
          setTemporaryShareState(true);
          setTimeout(function () { setTemporaryShareState(false); }, 2000);
        }).catch(function () {
          legacyCopyToClipboard(shareUrl);
        });
      } else {
        legacyCopyToClipboard(shareUrl);
      }
    });

    lucide.createIcons();
  </script>
</body>
</html>

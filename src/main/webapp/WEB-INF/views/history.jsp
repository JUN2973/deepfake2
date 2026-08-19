<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
  String contextPath = request.getContextPath();
  Object userIdObj = session.getAttribute("USER_ID");
  Object userNameObj = session.getAttribute("USER_NAME");
  boolean isAuthenticated = (userIdObj != null);
  String userName = userNameObj == null ? "" : String.valueOf(userNameObj);
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>&#xAC80;&#xC99D;&#xAE30;&#xB85D; - DeepScan</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
  <style>
    body {
      font-family: "Pretendard", "Noto Sans KR", sans-serif;
      background: #020617;
    }

    .panel-open {
      overflow: hidden;
    }
  </style>
</head>
<body class="min-h-screen bg-slate-950 text-white">
<% if (!isAuthenticated) { %>
  <div class="min-h-screen bg-slate-950 flex items-center justify-center p-4 relative overflow-hidden">
    <div class="absolute inset-0">
      <div class="absolute top-20 left-1/4 w-96 h-96 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-full blur-3xl opacity-20"></div>
      <div class="absolute bottom-20 right-1/4 w-96 h-96 bg-gradient-to-r from-cyan-600 to-blue-600 rounded-full blur-3xl opacity-20"></div>
    </div>

    <div class="text-center relative z-10">
      <div class="relative inline-block mb-6">
        <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-75"></div>
        <div class="relative w-24 h-24 bg-gradient-to-br from-sky-500 to-cyan-500 rounded-3xl flex items-center justify-center mx-auto shadow-lg shadow-sky-500/50">
          <i data-lucide="shield" class="w-12 h-12 text-white"></i>
        </div>
      </div>
      <h2 class="text-4xl font-bold text-white mb-4">&#xB85C;&#xADF8;&#xC778;&#xC774; &#xD544;&#xC694;&#xD569;&#xB2C8;&#xB2E4;</h2>
      <p class="text-slate-400 mb-8 text-lg">
        &#xAC80;&#xC99D; &#xAE30;&#xB85D;&#xC744; &#xD655;&#xC778;&#xD558;&#xB824;&#xBA74; &#xB85C;&#xADF8;&#xC778;&#xD574;&#xC8FC;&#xC138;&#xC694;
      </p>
      <div class="flex gap-3 justify-center">
        <button
          type="button"
          onclick="location.href='<%= contextPath %>/login'"
          class="px-8 py-4 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white rounded-xl transition-all shadow-lg shadow-sky-500/50 flex items-center gap-2 font-semibold text-lg"
        >
          <i data-lucide="log-in" class="w-5 h-5"></i>
          &#xB85C;&#xADF8;&#xC778;
        </button>
        <button
          type="button"
          onclick="location.href='<%= contextPath %>/'"
          class="px-8 py-4 bg-white/10 hover:bg-white/20 backdrop-blur-xl text-white rounded-xl transition-all border border-white/20 font-semibold text-lg"
        >
          &#xD648;&#xC73C;&#xB85C;
        </button>
      </div>
    </div>
  </div>
  <script>
    lucide.createIcons();
  </script>
<% } else { %>
  <div class="relative min-h-screen overflow-hidden">
    <div class="pointer-events-none absolute inset-0">
      <div class="absolute top-20 left-1/4 h-96 w-96 rounded-full bg-sky-500 blur-3xl opacity-15"></div>
      <div class="absolute bottom-16 right-1/4 h-96 w-96 rounded-full bg-cyan-500 blur-3xl opacity-10"></div>
    </div>

    <nav class="fixed left-0 right-0 top-0 z-40 border-b border-white/10 bg-slate-950/80 backdrop-blur-xl">
      <div class="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <button
          type="button"
          onclick="location.href='<%= contextPath %>/'"
          class="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm font-medium text-white backdrop-blur-xl transition-all hover:bg-white/20"
        >
          <i data-lucide="arrow-left" class="h-4 w-4"></i>
          &#xD648;&#xC73C;&#xB85C;
        </button>
        <div class="text-sm text-slate-400">
          <span class="font-medium text-sky-400"><%= userName %></span>&#xB2D8;&#xC758; &#xAC80;&#xC99D;&#xAE30;&#xB85D;
        </div>
      </div>
    </nav>

    <div class="relative mx-auto max-w-7xl px-4 pb-12 pt-24">
      <div class="mb-10 text-center">
        <div class="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-500 to-cyan-500 shadow-lg shadow-sky-500/50">
          <i data-lucide="calendar" class="h-10 w-10 text-white"></i>
        </div>
        <h1 class="mb-3 text-5xl font-bold">&#xAC80;&#xC99D;&#xAE30;&#xB85D;</h1>
        <p class="text-xl text-slate-400">&#xCD1D; <span id="resultCount" class="font-semibold text-sky-400">0&#xAC1C;</span>&#xC758; &#xAC80;&#xC99D; &#xACB0;&#xACFC;</p>
      </div>

      <div class="mb-8 rounded-3xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl">
        <div class="relative mb-4">
          <i data-lucide="search" class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500"></i>
          <input
            id="searchInput"
            type="text"
            placeholder="&#xD30C;&#xC77C;&#xBA85;&#xC73C;&#xB85C; &#xAC80;&#xC0C9;"
            class="w-full rounded-2xl border border-white/10 bg-slate-800/50 py-3 pl-12 pr-4 text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
        </div>
        <div class="flex flex-wrap gap-2">
          <button type="button" data-filter="all" class="filter-chip rounded-full border border-sky-500/30 bg-sky-500/20 px-4 py-2 text-sm font-medium text-sky-300">&#xC804;&#xCCB4;</button>
          <button type="button" data-filter="SAFE" class="filter-chip flex items-center gap-2 rounded-lg border border-white/10 bg-slate-800/50 px-4 py-2 text-sm font-medium text-slate-300"><i data-lucide="check-circle" class="h-4 w-4"></i>&#xC548;&#xC804;</button>
          <button type="button" data-filter="SUSPECT" class="filter-chip flex items-center gap-2 rounded-lg border border-white/10 bg-slate-800/50 px-4 py-2 text-sm font-medium text-slate-300"><i data-lucide="alert-triangle" class="h-4 w-4"></i>&#xC758;&#xC2EC;</button>
          <button type="button" data-filter="HIGH_RISK" class="filter-chip flex items-center gap-2 rounded-lg border border-white/10 bg-slate-800/50 px-4 py-2 text-sm font-medium text-slate-300"><i data-lucide="alert-triangle" class="h-4 w-4"></i>&#xC704;&#xD5D8;</button>
        </div>
      </div>

      <div id="emptyState" class="hidden rounded-3xl border border-white/10 bg-slate-900/60 p-12 text-center backdrop-blur-xl">
        <i data-lucide="filter" class="mx-auto mb-4 h-16 w-16 text-slate-700"></i>
        <p id="emptyMessage" class="mb-6 text-lg text-slate-400">&#xAC80;&#xC99D;&#xAE30;&#xB85D;&#xC774; &#xC5C6;&#xC2B5;&#xB2C8;&#xB2E4;.</p>
        <button
          type="button"
          onclick="location.href='<%= contextPath %>/'"
          class="rounded-2xl bg-sky-500 px-6 py-3 font-semibold text-white transition hover:bg-sky-400"
        >
          &#xC774;&#xBBF8;&#xC9C0; &#xAC80;&#xC99D;&#xD558;&#xB7EC; &#xAC00;&#xAE30;
        </button>
      </div>

      <div id="historyGrid" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3"></div>
    </div>

    <div id="detailBackdrop" class="fixed inset-0 z-50 hidden bg-black/60 backdrop-blur-sm"></div>
    <aside id="detailPanel" class="fixed bottom-0 right-0 top-0 z-50 w-full translate-x-full overflow-y-auto border-l border-white/10 bg-slate-900 transition-transform duration-300 md:w-[620px]">
      <div class="sticky top-0 z-10 border-b border-white/10 bg-slate-900/95 p-6 backdrop-blur-xl">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h2 class="mb-1 text-2xl font-bold text-white">&#xBD84;&#xC11D; &#xB9AC;&#xD3EC;&#xD2B8;</h2>
            <p id="detailFileName" class="text-sm text-slate-400">-</p>
          </div>
          <button type="button" id="detailCloseButton" class="rounded-lg p-2 transition hover:bg-white/10">
            <i data-lucide="x" class="h-6 w-6 text-slate-400"></i>
          </button>
        </div>
      </div>

      <div class="space-y-6 p-6">
        <div class="overflow-hidden rounded-2xl border border-white/10 bg-slate-800/50 backdrop-blur-xl">
          <div class="flex border-b border-white/10">
            <button
              type="button"
              id="detailTabOriginal"
              class="flex-1 px-4 py-3 text-sm font-semibold transition-all bg-sky-500/20 text-sky-400 border-b-2 border-sky-500"
            >
              <span class="flex items-center justify-center gap-2">
                <i data-lucide="eye" class="h-4 w-4"></i>
                &#xC6D0;&#xBCF8; &#xBCF4;&#xAE30;
              </span>
            </button>
            <button
              type="button"
              id="detailTabAnomaly"
              class="flex-1 px-4 py-3 text-sm font-semibold transition-all text-slate-400 hover:text-white hover:bg-white/5"
            >
              <span class="flex items-center justify-center gap-2">
                <i data-lucide="alert-triangle" class="h-4 w-4"></i>
                &#xC774;&#xC0C1; &#xC601;&#xC5ED; &#xBCF4;&#xAE30;
              </span>
            </button>
          </div>
          <div class="p-4">
            <div id="detailImageStage" class="relative overflow-hidden rounded-xl">
              <img id="detailOriginalImage" src="" alt="" class="w-full h-auto bg-slate-950">
              <div id="detailAnomalyOverlay" class="pointer-events-none absolute inset-0 hidden">
                <svg
                  class="absolute inset-0 h-full w-full"
                  viewBox="0 0 100 100"
                  preserveAspectRatio="none"
                >
                  <ellipse cx="30" cy="35" rx="12" ry="8" fill="rgba(239,68,68,0.4)" stroke="rgba(239,68,68,0.8)" stroke-width="0.5"></ellipse>
                  <ellipse cx="70" cy="35" rx="12" ry="8" fill="rgba(239,68,68,0.4)" stroke="rgba(239,68,68,0.8)" stroke-width="0.5"></ellipse>
                  <ellipse id="detailOverlaySkin" cx="75" cy="55" rx="18" ry="15" fill="rgba(234,179,8,0.35)" stroke="rgba(234,179,8,0.7)" stroke-width="0.5"></ellipse>
                  <ellipse id="detailOverlayMouth" cx="50" cy="70" rx="15" ry="10" fill="rgba(59,130,246,0.35)" stroke="rgba(59,130,246,0.7)" stroke-width="0.5"></ellipse>
                </svg>

                <div class="absolute inset-0">
                  <div class="absolute" style="left: 15%; top: 28%;">
                    <div class="flex items-center gap-2">
                      <div class="h-12 w-1 bg-red-400" style="transform: rotate(45deg);"></div>
                      <div class="rounded-lg border border-red-500/50 bg-slate-900/95 px-3 py-1.5 backdrop-blur-sm">
                        <span class="text-xs font-bold text-red-400">EYES</span>
                      </div>
                    </div>
                  </div>

                  <div id="detailLabelSkin" class="absolute" style="right: 8%; top: 48%;">
                    <div class="flex items-center gap-2">
                      <div class="rounded-lg border border-yellow-500/50 bg-slate-900/95 px-3 py-1.5 backdrop-blur-sm">
                        <span class="text-xs font-bold text-yellow-400">SKIN</span>
                      </div>
                      <div class="h-12 w-1 bg-yellow-400" style="transform: rotate(-45deg);"></div>
                    </div>
                  </div>

                  <div id="detailLabelMouth" class="absolute" style="left: 25%; bottom: 22%;">
                    <div class="flex items-center gap-2">
                      <div class="h-12 w-1 bg-blue-400" style="transform: rotate(30deg);"></div>
                      <div class="rounded-lg border border-blue-500/50 bg-slate-900/95 px-3 py-1.5 backdrop-blur-sm">
                        <span class="text-xs font-bold text-blue-400">MOUTH</span>
                      </div>
                    </div>
                  </div>

                  <div id="detailAnomalyBadge" class="absolute bottom-4 left-4 rounded-lg border border-white/20 bg-slate-900/95 px-4 py-2 backdrop-blur-sm">
                    <span class="text-sm font-semibold text-white">&#xC774;&#xC0C1; &#xC601;&#xC5ED; &#xAC10;&#xC9C0;</span>
                    <p class="mt-0.5 text-xs text-slate-400">&#xC5EC;&#xB7EC; &#xC601;&#xC5ED;&#xC5D0;&#xC11C; &#xC758;&#xC2EC;&#xC2A4;&#xB7EC;&#xC6B4; &#xD328;&#xD134; &#xBC1C;&#xACAC;</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="rounded-3xl border border-white/10 bg-slate-800/40 p-6">
          <div class="mb-4 flex items-center justify-between gap-4">
            <span class="text-sm text-slate-400">&#xCD5C;&#xC885; &#xD310;&#xC815;</span>
            <span id="detailStatusLabel" class="rounded-full border px-4 py-2 text-sm font-semibold"></span>
          </div>
          <div class="mb-3 flex items-center justify-between">
            <span class="text-sm text-slate-400">&#xC2E0;&#xB8B0;&#xB3C4; &#xC810;&#xC218;</span>
            <span id="detailConfidenceText" class="text-3xl font-bold text-white">0%</span>
          </div>
          <div class="h-3 overflow-hidden rounded-full bg-slate-900">
            <div id="detailConfidenceBar" class="h-full transition-transform duration-500 ease-out"></div>
          </div>
          <p id="detailConfidenceHint" class="mt-3 text-xs text-slate-500"></p>
        </div>

        <div class="rounded-3xl border border-amber-400/20 bg-amber-500/10 p-5">
          <div class="mb-2 flex items-center gap-2 text-amber-300">
            <i data-lucide="info" class="h-5 w-5"></i>
            <span class="font-semibold">&#xC774;&#xBBF8;&#xC9C0; &#xBD84;&#xC11D; &#xC548;&#xB0B4;</span>
          </div>
          <p class="text-sm leading-6 text-amber-100/90">
            &#xD604;&#xC7AC; &#xAC80;&#xC99D;&#xAE30;&#xB85D; &#xC0C1;&#xC138;&#xC5D0;&#xC11C;&#xB294; &#xC800;&#xC7A5;&#xB41C; &#xAC80;&#xC99D; &#xACB0;&#xACFC;&#xB9CC; &#xD45C;&#xC2DC;&#xD569;&#xB2C8;&#xB2E4;.
            &#xC758;&#xC2EC; &#xBD80;&#xC704; &#xC88C;&#xD45C;&#xB098; &#xC624;&#xBC84;&#xB808;&#xC774; &#xB370;&#xC774;&#xD130;&#xB294; &#xAE30;&#xB85D;&#xC5D0; &#xD568;&#xAED8; &#xC800;&#xC7A5;&#xB418;&#xC9C0; &#xC54A;&#xC544; &#xC694;&#xC57D;&#xB41C; &#xD310;&#xC815; &#xC815;&#xBCF4; &#xC911;&#xC2EC;&#xC73C;&#xB85C; &#xBCF4;&#xC5EC;&#xC9D1;&#xB2C8;&#xB2E4;.
          </p>
        </div>

        <div class="rounded-3xl border border-white/10 bg-slate-800/40 p-6">
          <h3 class="mb-3 text-lg font-semibold text-white">&#xBD84;&#xC11D; &#xC124;&#xBA85;</h3>
          <p id="detailExplanation" class="mb-4 leading-7 text-slate-300"></p>
          <div class="grid gap-4 border-t border-white/10 pt-4 sm:grid-cols-2">
            <div>
              <div class="mb-1 text-xs text-slate-500">API &#xC81C;&#xACF5;&#xC0AC;</div>
              <div id="detailApiProvider" class="font-medium text-white">-</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-slate-500">&#xBD84;&#xC11D; &#xBAA8;&#xB378;</div>
              <div id="detailModelName" class="font-medium text-white">AI Image Detection</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-slate-500">&#xC751;&#xB2F5; &#xC2DC;&#xAC04;</div>
              <div id="detailResponseTime" class="font-medium text-white">-</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-slate-500">&#xBD84;&#xC11D; &#xC77C;&#xC2DC;</div>
              <div id="detailTimestamp" class="font-medium text-white">-</div>
            </div>
          </div>
        </div>

        <button
          type="button"
          id="detailDeleteButton"
          class="flex w-full items-center justify-center gap-2 rounded-2xl border border-red-500/30 bg-red-500/15 px-6 py-4 font-semibold text-red-300 transition hover:bg-red-500/25"
        >
          <i data-lucide="trash-2" class="h-5 w-5"></i>
          &#xAC80;&#xC99D;&#xAE30;&#xB85D; &#xC0AD;&#xC81C;
        </button>
      </div>
    </aside>

    <div id="deleteModal" class="fixed inset-0 z-[60] hidden bg-black/60 backdrop-blur-sm">
      <div class="flex min-h-screen items-center justify-center px-4">
        <div class="w-full max-w-md rounded-3xl border border-white/10 bg-slate-900/95 p-6 shadow-2xl">
          <div class="mb-4 flex items-center justify-between">
            <h3 class="text-xl font-bold text-white">&#xC0AD;&#xC81C; &#xD655;&#xC778;</h3>
            <button type="button" id="deleteCancelIcon" class="rounded-lg p-2 transition hover:bg-white/10">
              <i data-lucide="x" class="h-6 w-6 text-slate-400"></i>
            </button>
          </div>
          <p id="deleteMessage" class="mb-6 text-sm leading-6 text-slate-400">
            &#xC120;&#xD0DD;&#xD55C; &#xAC80;&#xC99D; &#xACB0;&#xACFC;&#xB97C; &#xC0AD;&#xC81C;&#xD558;&#xC2DC;&#xACA0;&#xC2B5;&#xB2C8;&#xAE4C;? &#xC0AD;&#xC81C;&#xD55C; &#xACB0;&#xACFC;&#xB294; &#xBCF5;&#xAD6C;&#xD560; &#xC218; &#xC5C6;&#xC2B5;&#xB2C8;&#xB2E4;.
          </p>
          <div class="flex justify-end gap-3">
            <button type="button" id="deleteCancelButton" class="rounded-2xl border border-white/15 bg-white/5 px-5 py-3 font-semibold text-white transition hover:bg-white/10">&#xCDE8;&#xC18C;</button>
            <button type="button" id="deleteConfirmButton" class="rounded-2xl bg-red-500 px-5 py-3 font-semibold text-white transition hover:bg-red-400">&#xC0AD;&#xC81C;</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <script>
    const contextPath = "<%= contextPath %>";
    const rawHistory = [
      <c:forEach var="item" items="${historyList}" varStatus="status">
      {
        id: "<c:out value='${item.id}'/>",
        fileName: "<c:out value='${item.originalName}'/>",
        imageUrl: "<c:out value='${item.publicUrl}'/>",
        status: "<c:out value='${item.verdict}'/>",
        confidence: <c:out value="${item.score != null ? item.score * 100 : 0}"/>,
        timestamp: "<c:out value='${item.regDt}'/>"
      }<c:if test="${!status.last}">,</c:if>
      </c:forEach>
    ];

    let activeFilter = "all";
    let selectedItem = null;
    let deleteTargetId = null;
    let detailTab = "original";

    const TEXT = {
      safe: "안전",
      suspect: "의심",
      risk: "위험",
      noHistory: "검증기록이 없습니다.",
      noFilteredHistory: "조건에 맞는 검증기록이 없습니다.",
      confidence: "신뢰도 점수",
      detail: "상세보기",
      detailLoadFailed: "상세 정보를 불러오지 못했습니다.",
      deleteFailed: "검증기록 삭제에 실패했습니다.",
      confidenceVeryHigh: "매우 높은 신뢰도",
      confidenceHigh: "높은 신뢰도",
      confidenceMedium: "중간 수준의 신뢰도",
      confidenceLow: "참고 수준의 신뢰도",
      unavailable: "분석 불가",
      unavailableHint: "분석 기준을 충족하지 못해 신뢰도 점수를 제공하지 않습니다.",
      explanationUnavailable: "얼굴이 너무 작거나, 사람이 너무 많거나, 사람 얼굴이 아니거나, 이미지가 흐릿해 신뢰할 수 있는 분석을 진행할 수 없습니다.",
      explanationRisk: "조작 또는 생성 이미지일 가능성이 높습니다.",
      explanationSuspect: "추가 확인이 필요한 의심 신호가 감지되었습니다.",
      explanationSafe: "정상 이미지일 가능성이 높습니다.",
      deleteConfirmSuffix: "결과를 삭제하시겠습니까? 삭제한 결과는 복구할 수 없습니다.",
      tabOriginal: "원본 보기",
      tabAnomaly: "이상 영역 보기",
      anomalyDetected: "이상 영역 감지",
      anomalyHint: "여러 영역에서 의심스러운 패턴 발견"
    };

    const statusConfig = {
      SAFE: { label: TEXT.safe, badgeClass: "border-green-500/30 bg-green-500/10 text-green-300", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500" },
      AUTHENTIC: { label: TEXT.safe, badgeClass: "border-green-500/30 bg-green-500/10 text-green-300", progressClass: "bg-gradient-to-r from-green-500 to-emerald-500" },
      SUSPECT: { label: TEXT.suspect, badgeClass: "border-yellow-500/30 bg-yellow-500/10 text-yellow-300", progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500" },
      SUSPICIOUS: { label: TEXT.suspect, badgeClass: "border-yellow-500/30 bg-yellow-500/10 text-yellow-300", progressClass: "bg-gradient-to-r from-yellow-500 to-amber-500" },
      HIGH_RISK: { label: TEXT.risk, badgeClass: "border-red-500/30 bg-red-500/10 text-red-300", progressClass: "bg-gradient-to-r from-red-500 to-rose-500" },
      FAKE: { label: TEXT.risk, badgeClass: "border-red-500/30 bg-red-500/10 text-red-300", progressClass: "bg-gradient-to-r from-red-500 to-rose-500" },
      NOT_APPLICABLE: { label: TEXT.unavailable, badgeClass: "border-slate-500/40 bg-slate-500/10 text-slate-200", progressClass: "bg-slate-500" },
      UNABLE_TO_EVALUATE: { label: TEXT.unavailable, badgeClass: "border-slate-500/40 bg-slate-500/10 text-slate-200", progressClass: "bg-slate-500" }
    };

    function escapeHtml(value) {
      return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
    }

    function getNormalizedStatus(status, confidence) {
      if (status === "NOT_APPLICABLE" || status === "UNABLE_TO_EVALUATE") return status;
      if (status === "REAL") return "SAFE";
      if (status === "AUTHENTIC") return "SAFE";
      if (status === "SUSPICIOUS") return "SUSPECT";
      if (status === "FAKE") return "HIGH_RISK";
      if (status === "SAFE" || status === "SUSPECT" || status === "HIGH_RISK") return status;
      if (confidence >= 70) return "HIGH_RISK";
      if (confidence >= 35) return "SUSPECT";
      return "SAFE";
    }

    function resolveStatus(status) {
      return statusConfig[status] || statusConfig.SAFE;
    }

    function getFileType(fileName) {
      const ext = String(fileName || "").split(".").pop().toLowerCase();
      const typeMap = { jpg: "JPEG", jpeg: "JPEG", png: "PNG", gif: "GIF", webp: "WebP", bmp: "BMP" };
      return typeMap[ext] || "Image";
    }

    function formatDateLabel(value) {
      if (!value) return "-";
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return value;
      return date.toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric" });
    }

    function formatDateTime(value) {
      if (!value) return "-";
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return value;
      return date.toLocaleString("ko-KR");
    }

    function getConfidenceHint(confidence) {
      if (confidence === null) return TEXT.unavailableHint;
      if (confidence >= 90) return TEXT.confidenceVeryHigh;
      if (confidence >= 70) return TEXT.confidenceHigh;
      if (confidence >= 50) return TEXT.confidenceMedium;
      return TEXT.confidenceLow;
    }

    function getExplanation(status) {
      if (status === "NOT_APPLICABLE" || status === "UNABLE_TO_EVALUATE") return TEXT.explanationUnavailable;
      if (status === "HIGH_RISK") return TEXT.explanationRisk;
      if (status === "SUSPECT") return TEXT.explanationSuspect;
      return TEXT.explanationSafe;
    }

    function readReason(data) {
      if (!data || typeof data !== "object") return null;
      const candidates = [
        data.reason,
        data.message,
        data.data && data.data.reason,
        data.error && data.error.message,
        data.resultsSummary && data.resultsSummary.reason,
        data.resultsSummary && data.resultsSummary.metadata && data.resultsSummary.metadata.reason
      ];
      return candidates.find(function(value) {
        return typeof value === "string" && value.trim();
      }) || null;
    }

    function getFilteredHistory() {
      const keyword = document.getElementById("searchInput").value.trim().toLowerCase();
      return rawHistory.filter(function(item) {
        const normalizedStatus = getNormalizedStatus(item.status, item.confidence);
        const matchedFilter = activeFilter === "all" || activeFilter === normalizedStatus;
        const matchedKeyword = !keyword || String(item.fileName || "").toLowerCase().includes(keyword);
        return matchedFilter && matchedKeyword;
      });
    }

    function renderGrid() {
      const filtered = getFilteredHistory();
      const grid = document.getElementById("historyGrid");
      const emptyState = document.getElementById("emptyState");
      const emptyMessage = document.getElementById("emptyMessage");
      document.getElementById("resultCount").textContent = filtered.length + "개";

      if (!filtered.length) {
        grid.innerHTML = "";
        emptyMessage.textContent = rawHistory.length ? TEXT.noFilteredHistory : TEXT.noHistory;
        emptyState.classList.remove("hidden");
        lucide.createIcons();
        return;
      }

      emptyState.classList.add("hidden");
      grid.innerHTML = filtered.map(function(item) {
        const normalizedStatus = getNormalizedStatus(item.status, item.confidence);
        const config = resolveStatus(normalizedStatus);
        const unavailable = normalizedStatus === "NOT_APPLICABLE" || normalizedStatus === "UNABLE_TO_EVALUATE";
        const progressWidth = unavailable ? 0 : Math.max(0, Math.min(100, Math.round(item.confidence)));
        const confidenceLabel = unavailable ? "-" : progressWidth + "%";
        return ""
          + '<article class="relative group">'
          + '  <div class="absolute inset-0 rounded-2xl bg-gradient-to-r from-sky-600/0 to-cyan-600/0 transition-all duration-300 group-hover:from-sky-600/10 group-hover:to-cyan-600/10"></div>'
          + '  <div class="relative overflow-hidden rounded-2xl border border-white/10 bg-slate-900/50 backdrop-blur-xl transition-all group-hover:border-sky-500/50">'
          + '    <div class="relative h-48 overflow-hidden bg-slate-800/50">'
          + '      <img src="' + escapeHtml(item.imageUrl) + '" alt="' + escapeHtml(item.fileName) + '" class="h-full w-full object-cover">'
          + '      <div class="absolute right-3 top-3 flex items-center gap-2">'
          + '        <button type="button" onclick="event.stopPropagation(); prepareDelete(\'' + escapeHtml(item.id) + '\')" class="rounded-lg border border-red-500/30 bg-red-500/20 p-2 opacity-0 transition-all group-hover:opacity-100 hover:bg-red-500/30">'
          + '          <i data-lucide="trash-2" class="h-4 w-4 text-red-400"></i>'
          + '        </button>'
          + '        <span class="flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-semibold backdrop-blur-xl ' + config.badgeClass + '">'
          + '          <i data-lucide="' + (normalizedStatus === "SAFE" ? 'check-circle' : 'alert-triangle') + '" class="h-4 w-4"></i>'
          +           config.label
          + '        </span>'
          + '      </div>'
          + '    </div>'
          + '    <div class="space-y-3 p-4">'
          + '      <div>'
          + '        <div class="mb-1 flex items-center gap-2">'
          + '          <i data-lucide="file-image" class="h-4 w-4 text-sky-400"></i>'
          + '          <span class="text-xs font-medium text-slate-500">' + getFileType(item.fileName) + '</span>'
          + '        </div>'
          + '        <h3 class="truncate font-semibold text-white">' + escapeHtml(item.fileName) + '</h3>'
          + '      </div>'
          + '      <div>'
          + '        <div class="mb-1.5 flex items-center justify-between">'
          + '          <span class="text-xs text-slate-400">' + TEXT.confidence + '</span>'
          + '          <span class="text-sm font-bold text-white">' + confidenceLabel + '</span>'
          + '        </div>'
          + '        <div class="h-2 w-full rounded-full bg-slate-800/50">'
          + '          <div class="h-2 rounded-full ' + config.progressClass + '" style="width:' + progressWidth + '%;"></div>'
          + '        </div>'
          + '      </div>'
          + '      <div class="flex items-center gap-2 text-xs text-slate-500">'
          + '        <i data-lucide="calendar" class="h-3.5 w-3.5"></i>'
          +           escapeHtml(formatDateLabel(item.timestamp))
          + '      </div>'
          + '      <button type="button" class="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-sky-600 to-cyan-600 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-sky-500/30 transition-all hover:from-sky-500 hover:to-cyan-500" onclick="openDetail(\'' + escapeHtml(item.id) + '\')">'
          +           TEXT.detail
          + '        <i data-lucide="chevron-right" class="h-4 w-4"></i>'
          + '      </button>'
          + '    </div>'
          + '  </div>'
          + '  </div>'
          + '</article>';
      }).join("");

      lucide.createIcons();
    }

    function updateFilterButtons() {
      document.querySelectorAll(".filter-chip").forEach(function(button) {
        const isActive = button.dataset.filter === activeFilter;
        const filter = button.dataset.filter;
        if (filter === "all") {
          button.className = isActive
            ? "filter-chip rounded-lg border border-sky-500/30 bg-sky-500/20 px-4 py-2 text-sm font-medium text-sky-300"
            : "filter-chip rounded-lg border border-white/10 bg-slate-800/50 px-4 py-2 text-sm font-medium text-slate-400 hover:text-white";
          return;
        }

        if (filter === "SAFE") {
          button.className = isActive
            ? "filter-chip flex items-center gap-2 rounded-lg border border-green-500/30 bg-green-500/20 px-4 py-2 text-sm font-medium text-green-400"
            : "filter-chip flex items-center gap-2 rounded-lg border border-white/10 bg-slate-800/50 px-4 py-2 text-sm font-medium text-slate-400 hover:text-white";
          return;
        }

        if (filter === "SUSPECT") {
          button.className = isActive
            ? "filter-chip flex items-center gap-2 rounded-lg border border-yellow-500/30 bg-yellow-500/20 px-4 py-2 text-sm font-medium text-yellow-400"
            : "filter-chip flex items-center gap-2 rounded-lg border border-white/10 bg-slate-800/50 px-4 py-2 text-sm font-medium text-slate-400 hover:text-white";
          return;
        }

        button.className = isActive
          ? "filter-chip flex items-center gap-2 rounded-lg border border-red-500/30 bg-red-500/20 px-4 py-2 text-sm font-medium text-red-400"
          : "filter-chip flex items-center gap-2 rounded-lg border border-white/10 bg-slate-800/50 px-4 py-2 text-sm font-medium text-slate-400 hover:text-white";
      });
    }

    function openPanel() {
      document.getElementById("detailBackdrop").classList.remove("hidden");
      document.getElementById("detailPanel").classList.remove("translate-x-full");
      document.body.classList.add("panel-open");
    }

    function closePanel() {
      document.getElementById("detailBackdrop").classList.add("hidden");
      document.getElementById("detailPanel").classList.add("translate-x-full");
      document.body.classList.remove("panel-open");
      selectedItem = null;
    }

    function applyDetailTabStyles() {
      const originalButton = document.getElementById("detailTabOriginal");
      const anomalyButton = document.getElementById("detailTabAnomaly");
      const overlay = document.getElementById("detailAnomalyOverlay");

      if (!originalButton || !anomalyButton || !overlay) return;

      const hasAnomaly = selectedItem && getNormalizedStatus(selectedItem.status, selectedItem.confidence) !== "SAFE";
      const showAnomaly = detailTab === "anomaly" && hasAnomaly;

      originalButton.className = detailTab === "original"
        ? "flex-1 px-4 py-3 text-sm font-semibold transition-all bg-sky-500/20 text-sky-400 border-b-2 border-sky-500"
        : "flex-1 px-4 py-3 text-sm font-semibold transition-all text-slate-400 hover:text-white hover:bg-white/5";

      anomalyButton.className = detailTab === "anomaly"
        ? "flex-1 px-4 py-3 text-sm font-semibold transition-all bg-sky-500/20 text-sky-400 border-b-2 border-sky-500"
        : "flex-1 px-4 py-3 text-sm font-semibold transition-all text-slate-400 hover:text-white hover:bg-white/5";

      overlay.classList.toggle("hidden", !showAnomaly);
    }

    function populateDetail(detail) {
      const normalizedStatus = getNormalizedStatus(detail.verdict, Number(detail.score || 0) * 100);
      const config = resolveStatus(normalizedStatus);
      const unavailable = normalizedStatus === "NOT_APPLICABLE" || normalizedStatus === "UNABLE_TO_EVALUATE";
      const confidence = unavailable ? null : Math.round((Number(detail.score) || 0) * 100);

      let parsedJson = null;
      try {
        parsedJson = detail.apiRaw ? JSON.parse(detail.apiRaw) : null;
      } catch (error) {
        parsedJson = null;
      }

      document.getElementById("detailFileName").textContent = detail.originalName || "-";
      document.getElementById("detailOriginalImage").src = detail.publicUrl || "";
      document.getElementById("detailOriginalImage").alt = detail.originalName || "";
      document.getElementById("detailStatusLabel").className = "rounded-full border px-4 py-2 text-sm font-semibold " + config.badgeClass;
      document.getElementById("detailStatusLabel").textContent = config.label;
      document.getElementById("detailConfidenceText").textContent = confidence === null ? "-" : confidence + "%";
      document.getElementById("detailConfidenceBar").className = "h-full " + config.progressClass;
      document.getElementById("detailConfidenceBar").style.transform = "translateX(-" + (100 - (confidence || 0)) + "%)";
      document.getElementById("detailConfidenceHint").textContent = getConfidenceHint(confidence);
      document.getElementById("detailExplanation").textContent = unavailable
        ? (readReason(parsedJson) || getExplanation(normalizedStatus))
        : (parsedJson && parsedJson.explanation ? parsedJson.explanation : getExplanation(normalizedStatus));
      document.getElementById("detailApiProvider").textContent = detail.apiProvider || "-";
      document.getElementById("detailModelName").textContent = parsedJson && parsedJson.modelName ? parsedJson.modelName : "AI Image Detection";
      document.getElementById("detailResponseTime").textContent = parsedJson && parsedJson.responseTime ? parsedJson.responseTime + "ms" : "-";
      document.getElementById("detailTimestamp").textContent = formatDateTime(detail.regDt);

      const skinOverlay = document.getElementById("detailOverlaySkin");
      const mouthOverlay = document.getElementById("detailOverlayMouth");
      const skinLabel = document.getElementById("detailLabelSkin");
      const mouthLabel = document.getElementById("detailLabelMouth");
      const anomalyBadge = document.getElementById("detailAnomalyBadge");
      const hasAnomaly = normalizedStatus !== "SAFE";
      const highRisk = normalizedStatus === "HIGH_RISK";

      if (skinOverlay) skinOverlay.classList.toggle("hidden", !highRisk);
      if (mouthOverlay) mouthOverlay.classList.toggle("hidden", !highRisk);
      if (skinLabel) skinLabel.classList.toggle("hidden", !highRisk);
      if (mouthLabel) mouthLabel.classList.toggle("hidden", !highRisk);
      if (anomalyBadge) anomalyBadge.classList.toggle("hidden", !hasAnomaly);

      detailTab = "original";
      applyDetailTabStyles();
    }

    function openDetail(id) {
      selectedItem = rawHistory.find(function(item) { return String(item.id) === String(id); }) || null;
      if (!selectedItem) return;

      fetch(contextPath + "/api/v1/verifications/" + encodeURIComponent(id))
        .then(function(response) {
          if (!response.ok) {
            throw new Error("상세 요청에 실패했습니다.");
          }
          return response.json();
        })
        .then(function(payload) {
          const detail = payload && payload.data ? payload.data : payload;
          populateDetail(detail);
          openPanel();
        })
        .catch(function() {
          alert(TEXT.detailLoadFailed);
        });
    }

    function openDeleteModal() {
      if (!selectedItem) return;
      deleteTargetId = selectedItem.id;
      document.getElementById("deleteMessage").textContent = "'" + selectedItem.fileName + "' " + TEXT.deleteConfirmSuffix;
      document.getElementById("deleteModal").classList.remove("hidden");
    }

    function prepareDelete(id) {
      selectedItem = rawHistory.find(function(item) { return String(item.id) === String(id); }) || null;
      if (!selectedItem) return;
      openDeleteModal();
    }

    function closeDeleteModal() {
      document.getElementById("deleteModal").classList.add("hidden");
      deleteTargetId = null;
    }

    function deleteItem() {
      if (!deleteTargetId) return;
      fetch(contextPath + "/api/v1/verifications/" + encodeURIComponent(deleteTargetId), {
        method: "DELETE"
      })
        .then(function(response) {
          if (!response.ok) {
            throw new Error("삭제에 실패했습니다.");
          }
          window.location.reload();
        })
        .catch(function() {
          alert(TEXT.deleteFailed);
        });
    }

    document.getElementById("searchInput").addEventListener("input", renderGrid);
    document.querySelectorAll(".filter-chip").forEach(function(button) {
      button.addEventListener("click", function() {
        activeFilter = button.dataset.filter;
        updateFilterButtons();
        renderGrid();
      });
    });

    document.getElementById("detailBackdrop").addEventListener("click", closePanel);
    document.getElementById("detailCloseButton").addEventListener("click", closePanel);
    document.getElementById("detailTabOriginal").addEventListener("click", function() {
      detailTab = "original";
      applyDetailTabStyles();
    });
    document.getElementById("detailTabAnomaly").addEventListener("click", function() {
      detailTab = "anomaly";
      applyDetailTabStyles();
    });
    document.getElementById("detailDeleteButton").addEventListener("click", openDeleteModal);
    document.getElementById("deleteCancelIcon").addEventListener("click", closeDeleteModal);
    document.getElementById("deleteCancelButton").addEventListener("click", closeDeleteModal);
    document.getElementById("deleteConfirmButton").addEventListener("click", deleteItem);

    updateFilterButtons();
    renderGrid();
    lucide.createIcons();
  </script>
<% } %>
</body>
</html>

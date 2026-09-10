<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  체크리스트 기준 주석: 구현(검증기록): 검증기록 목록과 상세 조회 진입 화면을 구성한다.
--%>
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
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
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

    .mode-tab {
      display: inline-flex;
      flex: 1 1 0;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      border-bottom: 2px solid transparent;
      padding: 0.95rem 0.75rem;
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

    .detail-heatmap-stage {
      position: relative;
      display: flex;
      min-height: 320px;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      border-radius: 1rem;
      background: #020617;
    }

    .detail-image-wrap {
      position: relative;
      display: inline-block;
      max-width: 100%;
      line-height: 0;
    }

    .detail-image-wrap img {
      display: block;
      max-height: 66vh;
      max-width: 100%;
      object-fit: contain;
      border-radius: 0.9rem;
    }

    .detail-heatmap-layer {
      position: absolute;
      inset: 0;
      height: 100%;
      width: 100%;
      object-fit: contain;
      opacity: 0.55;
      mix-blend-mode: screen;
      pointer-events: none;
    }

    .detail-region-layer {
      position: absolute;
      inset: 0;
      pointer-events: none;
    }

    .detail-region-box {
      position: absolute;
      border: 3px solid rgba(248, 113, 113, 1);
      border-radius: 0.75rem;
      background: rgba(220, 38, 38, 0.34);
      box-shadow:
        0 0 0 2px rgba(255, 255, 255, 0.18),
        0 0 34px rgba(248, 113, 113, 0.56),
        inset 0 0 22px rgba(127, 29, 29, 0.34);
      pointer-events: auto;
    }

    .detail-region-box::after {
      content: attr(data-label);
      position: absolute;
      left: 0;
      bottom: calc(100% + 0.5rem);
      max-width: 13rem;
      border: 1px solid rgba(248, 113, 113, 0.45);
      border-radius: 0.75rem;
      background: rgba(15, 23, 42, 0.96);
      padding: 0.42rem 0.62rem;
      color: #fecaca;
      font-size: 0.75rem;
      font-weight: 800;
      line-height: 1.15rem;
      opacity: 0;
      pointer-events: none;
      white-space: nowrap;
      transform: translateY(4px);
      transition: opacity 140ms ease, transform 140ms ease;
    }

    .detail-region-box:hover::after {
      opacity: 1;
      transform: translateY(0);
    }

    .detail-notice {
      border: 1px solid rgba(251, 191, 36, 0.22);
      background: rgba(251, 191, 36, 0.08);
      color: #fde68a;
    }

    .detail-analysis-row {
      display: grid;
      grid-template-columns: 1fr 1.15fr 2fr auto;
      gap: 0.8rem;
      align-items: center;
      border: 1px solid rgba(148, 163, 184, 0.14);
      border-radius: 1rem;
      background: rgba(15, 23, 42, 0.72);
      padding: 0.9rem;
    }

    .detail-risk-badge {
      display: inline-flex;
      min-width: 4.1rem;
      justify-content: center;
      border-radius: 999px;
      border: 1px solid transparent;
      padding: 0.32rem 0.65rem;
      font-size: 0.75rem;
      font-weight: 800;
      white-space: nowrap;
    }

    .detail-risk-high {
      border-color: rgba(248, 113, 113, 0.35);
      background: rgba(239, 68, 68, 0.16);
      color: #fca5a5;
    }

    .detail-risk-medium {
      border-color: rgba(251, 146, 60, 0.35);
      background: rgba(249, 115, 22, 0.14);
      color: #fdba74;
    }

    .detail-risk-low {
      border-color: rgba(96, 165, 250, 0.3);
      background: rgba(59, 130, 246, 0.12);
      color: #93c5fd;
    }

    .detail-risk-unknown {
      border-color: rgba(148, 163, 184, 0.25);
      background: rgba(100, 116, 139, 0.14);
      color: #cbd5e1;
    }

    @media (max-width: 760px) {
      .detail-analysis-row {
        grid-template-columns: 1fr;
        gap: 0.5rem;
      }

      .detail-analysis-heading {
        display: none;
      }
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
    <aside id="detailPanel" class="fixed bottom-0 right-0 top-0 z-50 w-full translate-x-full overflow-y-auto border-l border-white/10 bg-slate-900 transition-transform duration-300 md:w-1/2">
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
            <button type="button" id="detailTabOriginal" class="mode-tab active">
              <i data-lucide="eye" class="h-4 w-4"></i>
              원본
            </button>
            <button type="button" id="detailTabAnomaly" class="mode-tab">
              <i data-lucide="scan-search" class="h-4 w-4"></i>
              특이점 보기
            </button>
            <button type="button" id="detailTabOverlay" class="mode-tab">
              <i data-lucide="layers" class="h-4 w-4"></i>
              오버레이
            </button>
          </div>
          <div class="p-4">
            <div id="detailImageStage" class="detail-heatmap-stage">
              <div class="detail-image-wrap">
                <img id="detailOriginalImage" src="" alt="">
                <img id="detailHeatmapImage" src="" alt="" class="detail-heatmap-layer hidden">
                <div id="detailRegionLayer" class="detail-region-layer hidden"></div>
              </div>
            </div>
            <div id="detailTabNotice" class="detail-notice mt-4 hidden rounded-2xl p-4 text-sm leading-6"></div>
          </div>
        </div>

        <div class="rounded-2xl border border-white/10 bg-slate-800/40 p-4">
          <div class="mb-2 flex items-center gap-2 text-sm font-semibold text-white">
            <i data-lucide="activity" class="h-4 w-4 text-amber-300"></i>
            픽셀 패턴 이상 감지
          </div>
          <p id="detailHeatmapDescription" class="text-sm leading-6 text-slate-300">
            IMD 히트맵은 주변 픽셀 패턴, 압축 흔적, 질감 차이를 기준으로 참고 신호를 시각화합니다.
          </p>
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

        <div class="rounded-3xl border border-white/10 bg-slate-800/40 p-6">
          <h3 class="mb-3 text-lg font-semibold text-white">분석 설명</h3>
          <p id="detailExplanation" class="leading-7 text-slate-300"></p>
          <div class="detail-notice mt-4 rounded-2xl p-4 text-sm leading-6">
            이 결과는 원본 여부를 확정하는 값이 아니라, 이미지에서 주변 픽셀 패턴과 다르게 감지된 영역을 보여주는 참고용 분석입니다. 압축, 보정, 필터, 캡처, 재업로드 이미지에서도 비슷한 패턴이 나타날 수 있습니다.
          </div>
        </div>

        <section class="rounded-3xl border border-cyan-500/20 bg-slate-800/40 p-6">
          <div class="mb-4 flex items-center gap-2">
            <i data-lucide="sparkles" class="h-5 w-5 text-cyan-300"></i>
            <h3 class="text-lg font-semibold text-white">Gemini 상세 해설</h3>
          </div>
          <div id="historyAiExplanationLoading" class="flex items-center gap-2 text-sm text-slate-400">
            <i data-lucide="loader-circle" class="h-4 w-4 animate-spin"></i>
            저장된 해설을 확인하고 있습니다.
          </div>
          <p id="historyAiExplanationEmpty" class="hidden text-sm leading-6 text-slate-400">
            아직 생성된 상세 해설이 없습니다. 분석 결과 화면에서 한 번 생성하면 여기에 저장됩니다.
          </p>
          <div id="historyAiExplanationResult" class="hidden space-y-4">
            <p id="historyAiSummary" class="font-semibold leading-7 text-cyan-100"></p>
            <p id="historyAiExplanation" class="text-sm leading-7 text-slate-300"></p>
            <div class="border-t border-white/10 pt-4">
              <div class="mb-1 text-xs font-semibold text-slate-500">확인 방법</div>
              <p id="historyAiActionGuide" class="text-sm leading-6 text-slate-300"></p>
            </div>
            <p id="historyAiMeta" class="text-xs text-slate-500"></p>
          </div>
        </section>

        <section class="rounded-3xl border border-emerald-500/20 bg-slate-800/40 p-6">
          <div class="mb-4 flex items-center gap-2">
            <i data-lucide="scan-search" class="h-5 w-5 text-emerald-300"></i>
            <h3 class="text-lg font-semibold text-white">AI 이미지 2차 검증</h3>
          </div>
          <div id="historyImageReviewLoading" class="flex items-center gap-2 text-sm text-slate-400">
            <i data-lucide="loader-circle" class="h-4 w-4 animate-spin"></i>
            저장된 2차 검증을 확인하고 있습니다.
          </div>
          <p id="historyImageReviewEmpty" class="hidden text-sm leading-6 text-slate-400">
            아직 생성된 이미지 2차 검증 결과가 없습니다. 분석 결과 화면에서 한 번 검토하면 여기에 저장됩니다.
          </p>
          <div id="historyImageReviewResult" class="hidden space-y-4">
            <div class="flex items-center justify-between gap-3">
              <span class="text-xs font-semibold text-slate-500">교차 검증 결과</span>
              <span id="historyImageReviewStatus" class="rounded-full border px-3 py-1 text-xs font-semibold"></span>
            </div>
            <p id="historyImageReviewConclusion" class="font-semibold leading-7 text-emerald-100"></p>
            <p id="historyImageReviewSummary" class="text-sm leading-7 text-slate-300"></p>
            <ul id="historyImageReviewIndicators" class="space-y-2 text-sm leading-6 text-slate-300"></ul>
            <p id="historyImageReviewMeta" class="border-t border-white/10 pt-4 text-xs text-slate-500"></p>
          </div>
        </section>

        <section class="rounded-3xl border border-amber-500/20 bg-slate-800/40 p-6">
          <div class="mb-4 flex items-center gap-2">
            <i data-lucide="flag" class="h-5 w-5 text-amber-300"></i>
            <h3 class="text-lg font-semibold text-white">오탐 신고 및 재검토 요청</h3>
          </div>
          <div id="historyReviewRequestLoading" class="flex items-center gap-2 text-sm text-slate-400">
            <i data-lucide="loader-circle" class="h-4 w-4 animate-spin"></i>
            재검토 요청 상태를 확인하고 있습니다.
          </div>
          <div id="historyReviewRequestError" class="hidden text-sm leading-6 text-rose-300" role="alert"></div>

          <form id="historyReviewRequestForm" class="hidden space-y-4">
            <div>
              <label for="historyReviewRequestType" class="mb-2 block text-sm font-semibold text-slate-300">요청 유형</label>
              <select id="historyReviewRequestType" class="min-h-11 w-full rounded-xl border border-white/10 bg-slate-950/80 px-3 text-sm text-white outline-none transition focus:border-amber-300/60 focus:ring-2 focus:ring-amber-300/20">
                <option value="RECHECK">판정 재검토</option>
                <option value="FALSE_POSITIVE">오탐 신고: 실제인데 조작으로 판정</option>
                <option value="FALSE_NEGATIVE">미탐 신고: 조작인데 실제로 판정</option>
              </select>
            </div>
            <div>
              <div class="mb-2 flex items-center justify-between gap-3">
                <label for="historyReviewRequestReason" class="text-sm font-semibold text-slate-300">요청 사유</label>
                <span id="historyReviewRequestReasonCount" class="text-xs text-slate-500">0 / 1000</span>
              </div>
              <textarea id="historyReviewRequestReason" maxlength="1000" rows="4" required class="w-full resize-y rounded-xl border border-white/10 bg-slate-950/80 px-3 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-slate-600 focus:border-amber-300/60 focus:ring-2 focus:ring-amber-300/20" placeholder="판정이 잘못되었다고 생각하는 이유를 작성해 주세요."></textarea>
            </div>
            <button id="historyReviewRequestSubmitButton" type="submit" class="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-amber-300 px-4 py-2.5 text-sm font-bold text-slate-950 transition hover:bg-amber-200 disabled:cursor-wait disabled:opacity-60">
              <i data-lucide="send" class="h-4 w-4"></i>
              <span>재검토 요청 접수</span>
            </button>
          </form>

          <div id="historyReviewRequestResult" class="hidden space-y-4">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-xs text-slate-500">요청 유형</p>
                <p id="historyReviewRequestTypeText" class="mt-1 font-semibold text-white"></p>
              </div>
              <span id="historyReviewRequestStatus" class="rounded-full border px-3 py-1 text-xs font-semibold"></span>
            </div>
            <div>
              <p class="text-xs text-slate-500">요청 사유</p>
              <p id="historyReviewRequestReasonText" class="mt-1 whitespace-pre-line text-sm leading-6 text-slate-300"></p>
            </div>
            <div id="historyReviewerNoteWrap" class="hidden rounded-2xl border border-white/10 bg-slate-950/50 p-4">
              <p class="text-xs text-slate-500">검토자 답변</p>
              <p id="historyReviewerNoteText" class="mt-1 whitespace-pre-line text-sm leading-6 text-slate-300"></p>
            </div>
            <p id="historyReviewRequestMeta" class="border-t border-white/10 pt-4 text-xs text-slate-500"></p>
          </div>
        </section>

        <div class="rounded-3xl border border-white/10 bg-slate-800/40 p-6">
          <h3 class="mb-4 text-lg font-semibold text-white">상세 정보</h3>
          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <div class="mb-1 text-xs text-slate-500">최종 분석 제공자</div>
              <div id="detailApiProvider" class="font-medium text-white">Reality Defender</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-slate-500">최종 분석 모델</div>
              <div id="detailModelName" class="font-medium text-white">Reality Defender</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-slate-500">히트맵 시각화</div>
              <div id="detailHeatmapModel" class="font-medium text-white">IMD ManTraNet</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-slate-500">표시 영역 수</div>
              <div id="detailRegionCount" class="font-medium text-white">0개</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-slate-500">히트맵 상태</div>
              <div id="detailHeatmapStatus" class="font-medium text-white">응답 없음</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-slate-500">&#xBD84;&#xC11D; &#xC77C;&#xC2DC;</div>
              <div id="detailTimestamp" class="font-medium text-white">-</div>
            </div>
          </div>
        </div>

        <div class="rounded-3xl border border-white/10 bg-slate-800/40 p-6">
          <h3 class="mb-4 text-lg font-semibold text-white">분석 상세 근거</h3>
          <div class="detail-analysis-heading grid grid-cols-[1fr_1.15fr_2fr_auto] gap-3 px-3 pb-2 text-xs font-semibold text-slate-500">
            <span>분석 항목</span>
            <span>감지 결과</span>
            <span>설명</span>
            <span>위험도</span>
          </div>
          <div id="detailAnalysisRows" class="space-y-3"></div>
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
    let detailAnalysisState = {
      heatmapUrl: "",
      heatmapUrls: [],
      heatmapIndex: 0,
      regions: [],
      heatmapLoadFailed: false,
      status: "SAFE",
      confidence: 0
    };

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

    Object.assign(TEXT, {
      safe: "낮은 의심",
      suspect: "주의 필요",
      risk: "추가 확인 필요",
      noHistory: "검증기록이 없습니다.",
      noFilteredHistory: "조건에 맞는 검증기록이 없습니다.",
      confidence: "Reality Defender 점수",
      detail: "상세보기",
      detailLoadFailed: "상세 정보를 불러오지 못했습니다.",
      deleteFailed: "검증기록 삭제에 실패했습니다.",
      confidenceVeryHigh: "강한 조작 의심 신호가 감지된 상태입니다.",
      confidenceHigh: "추가 확인이 필요한 신호가 있습니다.",
      confidenceMedium: "일부 참고 신호가 있어 함께 확인하는 것이 좋습니다.",
      confidenceLow: "강한 조작 의심 신호는 크지 않습니다.",
      unavailable: "분석 제한",
      unavailableHint: "분석 기준을 충분히 만족하지 못해 점수를 제공하지 못했습니다.",
      explanationUnavailable: "현재 이미지는 분석 기준을 충분히 만족하지 못해 결과 해석에 제한이 있습니다.",
      explanationRisk: "이미지 내 일부 영역에서 비정상적인 패턴이 감지되었습니다. 추가 확인이 필요한 참고 신호로 볼 수 있습니다.",
      explanationSuspect: "일부 영역에서 주변과 다른 픽셀 패턴 차이가 관찰될 수 있습니다. 이 결과만으로 조작 여부를 단정할 수는 없습니다.",
      explanationSafe: "강한 조작 의심 신호는 크지 않습니다. 다만 원본 출처와 촬영 맥락을 함께 확인하는 것이 좋습니다.",
      deleteConfirmSuffix: "결과를 삭제하시겠습니까? 삭제한 결과는 복구할 수 없습니다.",
      tabOriginal: "원본 보기",
      tabAnomaly: "특이점 보기",
      anomalyDetected: "픽셀 패턴 차이 감지",
      anomalyHint: "표시 가능한 세부 의심 영역을 참고용으로 보여줍니다."
    });

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

    function getDetailedAnalysisMessage(result) {
      const status = getNormalizedStatus(result && result.verdict, Number(result && result.score || 0) * 100);
      if (status === "NOT_APPLICABLE" || status === "UNABLE_TO_EVALUATE") {
        return "이미지가 딥페이크 분석 기준을 충분히 충족하지 못해 판정을 보류했습니다. 얼굴이 너무 작거나 여러 명이 포함된 경우, 모델이 안정적으로 비교할 기준을 확보하지 못할 수 있습니다. 가능한 경우 더 선명한 원본 이미지로 다시 확인하는 것이 좋습니다.";
      }
      if (status === "HIGH_RISK") {
        return "분석 결과, 이미지 내 여러 영역에서 비정상적인 패턴이 감지되었습니다. 특정 영역의 픽셀 구조, 경계선, 질감 차이가 주변과 다르게 나타날 가능성이 있습니다. 히트맵에서 밝게 표시되는 부분을 중심으로 원본 여부를 추가 확인하는 것이 좋습니다.";
      }
      if (status === "SUSPECT") {
        return "분석 결과, 일부 영역에서 일반적인 이미지와 다른 픽셀 패턴 또는 질감 차이가 감지되었습니다. 이 결과는 조작을 확정하는 의미가 아니라, 추가 확인이 필요한 참고 신호입니다. 얼굴, 배경 경계선, 그림자, 압축 흔적이 강하게 나타나는 영역을 중심으로 확인해주세요.";
      }
      return "분석 결과, 이미지 전체에서 강한 조작 흔적은 확인되지 않았습니다. 다만 AI 분석 결과는 100% 확정값이 아니므로, 중요한 용도로 사용할 경우 원본 출처와 촬영 맥락을 함께 확인하는 것이 좋습니다. 픽셀 패턴, 압축 흔적, 질감 차이를 기준으로 확인했을 때 조작 가능성은 낮은 편입니다.";
    }

    function getDetailedAnalysisMessage(detail) {
      const status = getNormalizedStatus(detail.verdict, Number(detail.score || 0) * 100);
      const confidence = Math.round((Number(detail.score) || 0) * 100);

      if (status === "NOT_APPLICABLE" || status === "UNABLE_TO_EVALUATE") {
        return "현재 이미지는 분석 기준을 충분히 만족하지 못해 결과 해석에 제한이 있습니다. Reality Defender 응답을 기준으로 최종 상태를 표시했으며, 히트맵 또는 영역 데이터가 제공된 경우에만 참고 시각화를 표시합니다. 가능한 경우 더 선명한 원본 이미지와 출처 정보를 함께 확인하는 것이 좋습니다.";
      }
      if (status === "HIGH_RISK" || status === "FAKE" || confidence >= 70) {
        return "분석 결과, 이미지 내 일부 영역에서 비정상적인 패턴이 감지되었습니다. 이 결과는 조작 여부를 확정하는 것이 아니라, 추가 확인이 필요한 참고 신호로 볼 수 있습니다. 특히 히트맵에서 강조되는 부분과 픽셀 패턴 차이가 나타나는 영역을 중심으로 원본 여부를 함께 검토하는 것이 좋습니다.";
      }
      if (status === "SUSPECT" || status === "SUSPICIOUS" || confidence >= 35) {
        return "분석 결과, 일부 영역에서 주변과 다른 픽셀 패턴 차이가 관찰될 수 있습니다. 다만 이 결과만으로 조작 여부를 단정할 수 없으며, 추가 확인이 필요한 참고용 신호로 해석하는 것이 적절합니다. 원본 출처, 촬영 맥락, 재저장 여부를 함께 확인하는 것이 좋습니다.";
      }
      return "분석 결과, 강한 조작 의심 신호는 크지 않습니다. 다만 AI 분석은 참고용 결과이므로 원본 출처와 촬영 맥락을 함께 확인하는 것이 좋습니다. 히트맵이나 영역 데이터가 제공된 경우에도 최종 판정 기준은 Reality Defender 결과를 우선합니다.";
    }

    function normalizeImageData(imageData) {
      if (!imageData) return "";
      if (typeof imageData === "string") return resolveImageUrl(imageData);
      const data = imageData.data || imageData.base64 || imageData.url;
      if (!data) return "";
      const textData = String(data).trim();
      if (textData.startsWith("data:")) return textData;
      if (isImageUrlLike(textData)) return resolveImageUrl(textData);
      if (imageData.url) return resolveImageUrl(data);
      const type = imageData.type || imageData.mimeType || "image/png";
      return "data:" + type + ";base64," + textData.replace(/\s/g, "");
    }

    function isImageUrlLike(value) {
      return value.startsWith("http://")
        || value.startsWith("https://")
        || value.startsWith("/")
        || value.startsWith("./")
        || value.startsWith("../")
        || value.startsWith("blob:");
    }

    function resolveImageUrl(url) {
      if (!url) return "";
      const value = String(url).trim();
      if (!value) return "";
      if (value.startsWith("data:") || value.startsWith("http://") || value.startsWith("https://") || value.startsWith("blob:")) return value;
      if (value.startsWith("//")) return window.location.protocol + value;
      if (value.startsWith("/")) return contextPath && !value.startsWith(contextPath + "/") ? contextPath + value : value;
      return contextPath + "/" + value.replace(/^\.?\//, "");
    }

    function getHeatmapUrl(result) {
      return getHeatmapUrls(result)[0] || "";
    }

    function getHeatmapUrls(result) {
      const nested = result && result.imdHeatmapResult ? result.imdHeatmapResult : {};
      const candidates = [
        result && result.heatmapUrl,
        result && result.heatmap_url,
        result && result.overlayUrl,
        result && result.overlay_url,
        result && result.imdHeatmapUrl,
        result && result.imd_heatmap_url,
        result && result.visualizationUrl,
        result && result.visualization_url,
        nested.heatmapUrl,
        nested.heatmap_url,
        nested.overlayUrl,
        nested.overlay_url,
        nested.imdHeatmapUrl,
        nested.imd_heatmap_url,
        nested.visualizationUrl,
        nested.visualization_url
      ].map(normalizeImageData).filter(Boolean);

      const imageDataCandidates = [
        nested.processedHeatmap,
        nested.heatmap,
        result && result.processedHeatmap,
        result && result.heatmap,
        nested.rawHeatmap,
        result && result.rawHeatmap,
        nested.overlayHeatmap,
        nested.overlay,
        result && result.overlayHeatmap,
        result && result.overlay
      ].map(normalizeImageData).filter(Boolean);

      return Array.from(new Set(candidates.concat(imageDataCandidates)));
    }

    function getAnomalyRegions(result) {
      const nested = result && result.imdHeatmapResult ? result.imdHeatmapResult : {};
      const candidates = [
        nested.suspiciousRegions,
        nested.anomalyRegions,
        nested.regions,
        nested.boxes,
        nested.bboxes,
        result && result.suspiciousRegions,
        result && result.anomalyRegions,
        result && result.regions,
        result && result.boxes,
        result && result.bboxes,
        result && result.boundingBoxes,
        result && result.bounding_box,
        result && result.boundingBoxes
      ];
      const found = candidates.find(Array.isArray) || [];
      return found.map(function(region) {
        const box = Array.isArray(region) ? { x: region[0], y: region[1], width: region[2], height: region[3] } : region || {};
        const x = Number(box.x ?? box.left ?? box.l ?? 0);
        const y = Number(box.y ?? box.top ?? box.t ?? 0);
        const width = Number(box.width ?? box.w ?? box.right ?? 0);
        const height = Number(box.height ?? box.h ?? box.bottom ?? 0);
        const confidenceRaw = Number(box.confidence ?? box.score ?? box.intensity ?? 1);
        return {
          x: Number.isFinite(x) ? x : 0,
          y: Number.isFinite(y) ? y : 0,
          width: Number.isFinite(width) ? width : 0,
          height: Number.isFinite(height) ? height : 0,
          confidence: Number.isFinite(confidenceRaw) ? confidenceRaw : 1,
          label: box.label || box.name || "픽셀 패턴 이상 영역"
        };
      }).filter(function(region) {
        return region.width > 0 && region.height > 0;
      });
    }

    function getHistoryPrimaryRiskLevel() {
      const status = detailAnalysisState.status;
      const confidence = detailAnalysisState.confidence || 0;
      if (status === "HIGH_RISK" || status === "FAKE" || confidence >= 70) return "높음";
      if (status === "SUSPECT" || status === "SUSPICIOUS" || confidence >= 35) return "중간";
      return "낮음";
    }

    function getHistorySecondaryRiskLevel() {
      const primary = getHistoryPrimaryRiskLevel();
      if (primary === "높음") return (detailAnalysisState.confidence || 0) >= 85 ? "높음" : "중간";
      return primary;
    }

    function getDetailRiskBadgeClass(level) {
      if (level === "높음") return "detail-risk-high";
      if (level === "중간") return "detail-risk-medium";
      if (level === "낮음") return "detail-risk-low";
      return "detail-risk-unknown";
    }

    function getHistoryDetailedAnalysisRows() {
      const hasHeatmap = Boolean(detailAnalysisState.heatmapUrl);
      const primaryRisk = getHistoryPrimaryRiskLevel();
      const secondaryRisk = getHistorySecondaryRiskLevel();
      const confidence = detailAnalysisState.confidence || 0;

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

    function renderDetailAnalysisRows() {
      const container = document.getElementById("detailAnalysisRows");
      if (!container) return;
      container.innerHTML = getHistoryDetailedAnalysisRows().map(function(row) {
        return ""
          + '<div class="detail-analysis-row">'
          + '  <div class="font-semibold text-white">' + escapeHtml(row.item) + '</div>'
          + '  <div class="text-sm text-slate-200">' + escapeHtml(row.detected) + '</div>'
          + '  <div class="text-sm leading-6 text-slate-400">' + escapeHtml(row.description) + '</div>'
          + '  <div><span class="detail-risk-badge ' + getDetailRiskBadgeClass(row.risk) + '">' + escapeHtml(row.risk) + '</span></div>'
          + '</div>';
      }).join("");
    }

    function renderRegionBoxes(regions) {
      const layer = document.getElementById("detailRegionLayer");
      const image = document.getElementById("detailOriginalImage");
      layer.innerHTML = "";
      layer.classList.toggle("hidden", !regions.length);
      regions.forEach(function(region) {
        const normalized = region.x <= 1 && region.y <= 1 && region.width <= 1 && region.height <= 1;
        const naturalWidth = image.naturalWidth || 1;
        const naturalHeight = image.naturalHeight || 1;
        const x = normalized ? region.x : region.x / naturalWidth;
        const y = normalized ? region.y : region.y / naturalHeight;
        const width = normalized ? region.width : region.width / naturalWidth;
        const height = normalized ? region.height : region.height / naturalHeight;
        const box = document.createElement("div");
        box.className = "detail-region-box";
        box.title = region.label;
        box.dataset.label = region.label;
        box.style.left = (Math.max(0, Math.min(1, x)) * 100) + "%";
        box.style.top = (Math.max(0, Math.min(1, y)) * 100) + "%";
        box.style.width = (Math.max(0, Math.min(1, width)) * 100) + "%";
        box.style.height = (Math.max(0, Math.min(1, height)) * 100) + "%";
        layer.appendChild(box);
      });
    }

    function renderHeatmapOverlay() {
      const heatmapImage = document.getElementById("detailHeatmapImage");
      heatmapImage.classList.add("hidden");
      heatmapImage.style.opacity = "0";
      const urls = detailAnalysisState.heatmapUrls || [];
      const url = urls[detailAnalysisState.heatmapIndex] || detailAnalysisState.heatmapUrl;
      if (!url) return;
      heatmapImage.src = url;
      heatmapImage.style.opacity = "0.56";
      heatmapImage.classList.remove("hidden");
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
      const overlayButton = document.getElementById("detailTabOverlay");
      const heatmapImage = document.getElementById("detailHeatmapImage");
      const notice = document.getElementById("detailTabNotice");

      if (!originalButton || !anomalyButton || !overlayButton || !heatmapImage || !notice) return;

      const tabs = { original: originalButton, anomaly: anomalyButton, overlay: overlayButton };
      Object.keys(tabs).forEach(function(key) {
        tabs[key].classList.toggle("active", detailTab === key);
      });

      heatmapImage.classList.add("hidden");
      heatmapImage.removeAttribute("src");
      heatmapImage.style.opacity = "0";
      document.getElementById("detailRegionLayer").classList.add("hidden");
      notice.classList.add("hidden");
      notice.textContent = "";

      if (detailTab === "anomaly") {
        renderRegionBoxes(detailAnalysisState.regions);
        if (!detailAnalysisState.regions.length) {
          notice.textContent = detailAnalysisState.heatmapUrl
            ? "표시 가능한 세부 의심 영역은 없습니다. 이 경우 전체 히트맵을 통해 참고용으로 확인할 수 있습니다."
            : "표시 가능한 세부 의심 영역은 없습니다. IMD 히트맵 이미지도 응답에 포함되지 않았습니다.";
          notice.classList.remove("hidden");
        }
      }

      if (detailTab === "overlay") {
        if (detailAnalysisState.heatmapUrl && !detailAnalysisState.heatmapLoadFailed) {
          renderHeatmapOverlay();
          if (!detailAnalysisState.regions.length) {
            notice.textContent = "표시 가능한 세부 영역은 없지만, 전체 히트맵 참고 이미지는 표시됩니다.";
            notice.classList.remove("hidden");
          }
        } else {
          notice.textContent = detailAnalysisState.heatmapLoadFailed
            ? "히트맵 이미지를 불러오지 못했습니다. 파일 경로 또는 S3 URL을 확인해주세요."
            : "IMD 히트맵 이미지가 응답에 포함되지 않았습니다. 백엔드 응답의 heatmapUrl 필드를 확인해주세요.";
          notice.classList.remove("hidden");
        }
      }

      document.getElementById("detailHeatmapStatus").textContent = detailAnalysisState.heatmapLoadFailed
        ? "로딩 실패"
        : (detailAnalysisState.heatmapUrl ? "표시 가능" : "응답 없음");
      lucide.createIcons();
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
      let parsedAnalysisJson = null;
      try {
        parsedAnalysisJson = detail.analysisJson ? JSON.parse(detail.analysisJson) : null;
      } catch (error) {
        parsedAnalysisJson = null;
      }
      const detailAnalysisData = Object.assign({}, parsedJson || {}, parsedAnalysisJson || {});

      document.getElementById("detailFileName").textContent = detail.originalName || "-";
      document.getElementById("detailOriginalImage").src = detail.publicUrl || "";
      document.getElementById("detailOriginalImage").alt = detail.originalName || "";
      document.getElementById("detailHeatmapImage").src = "";
      document.getElementById("detailHeatmapImage").onerror = function() {
        const urls = detailAnalysisState.heatmapUrls || [];
        if (detailTab === "overlay" && detailAnalysisState.heatmapIndex < urls.length - 1) {
          detailAnalysisState.heatmapIndex += 1;
          renderHeatmapOverlay();
          return;
        }
        detailAnalysisState.heatmapLoadFailed = true;
        this.classList.add("hidden");
        applyDetailTabStyles();
      };
      document.getElementById("detailStatusLabel").className = "rounded-full border px-4 py-2 text-sm font-semibold " + config.badgeClass;
      document.getElementById("detailStatusLabel").textContent = config.label;
      document.getElementById("detailConfidenceText").textContent = confidence === null ? "-" : confidence + "%";
      document.getElementById("detailConfidenceBar").className = "h-full " + config.progressClass;
      document.getElementById("detailConfidenceBar").style.transform = "translateX(-" + (100 - (confidence || 0)) + "%)";
      document.getElementById("detailConfidenceHint").textContent = getConfidenceHint(confidence);
      document.getElementById("detailExplanation").textContent = getDetailedAnalysisMessage(detail);
      detailAnalysisState = {
        heatmapUrl: getHeatmapUrl(detailAnalysisData),
        heatmapUrls: getHeatmapUrls(detailAnalysisData),
        heatmapIndex: 0,
        regions: getAnomalyRegions(detailAnalysisData),
        heatmapLoadFailed: false,
        status: normalizedStatus,
        confidence: confidence || 0
      };
      document.getElementById("detailApiProvider").textContent = "Reality Defender";
      document.getElementById("detailModelName").textContent = (detailAnalysisData && (detailAnalysisData.modelName || detailAnalysisData.model || (detailAnalysisData.realityResult && (detailAnalysisData.realityResult.modelName || detailAnalysisData.realityResult.model)))) || "Reality Defender";
      document.getElementById("detailHeatmapModel").textContent = (detailAnalysisData && detailAnalysisData.imdHeatmapResult && (detailAnalysisData.imdHeatmapResult.modelName || detailAnalysisData.imdHeatmapResult.model)) || "IMD / ManTraNet";
      document.getElementById("detailRegionCount").textContent = detailAnalysisState.regions.length + "개";
      document.getElementById("detailHeatmapStatus").textContent = detailAnalysisState.heatmapUrl ? "표시 가능" : "응답 없음";
      document.getElementById("detailTimestamp").textContent = formatDateTime(detail.regDt);
      renderDetailAnalysisRows();

      detailTab = "original";
      applyDetailTabStyles();
    }

    function resetSavedAiResults() {
      ["historyAiExplanationResult", "historyAiExplanationEmpty", "historyImageReviewResult", "historyImageReviewEmpty"]
        .forEach(function(id) { document.getElementById(id).classList.add("hidden"); });
      ["historyAiExplanationLoading", "historyImageReviewLoading"]
        .forEach(function(id) {
          document.getElementById(id).classList.remove("hidden");
          document.getElementById(id).classList.add("flex");
        });
    }

    function finishSavedAiLoading(loadingId) {
      const loading = document.getElementById(loadingId);
      loading.classList.add("hidden");
      loading.classList.remove("flex");
    }

    function renderSavedAiExplanation(data) {
      finishSavedAiLoading("historyAiExplanationLoading");
      if (!data) {
        document.getElementById("historyAiExplanationEmpty").classList.remove("hidden");
        return;
      }

      document.getElementById("historyAiSummary").textContent = data.summary || "";
      document.getElementById("historyAiExplanation").textContent = data.explanation || "";
      document.getElementById("historyAiActionGuide").textContent = data.actionGuide || "";
      const meta = [data.model, Number.isFinite(data.totalTokens) ? "생성 시 총 " + data.totalTokens + " 토큰" : ""]
        .filter(Boolean);
      document.getElementById("historyAiMeta").textContent = meta.join(" · ");
      document.getElementById("historyAiExplanationResult").classList.remove("hidden");
    }

    function renderSavedImageReview(data) {
      finishSavedAiLoading("historyImageReviewLoading");
      if (!data) {
        document.getElementById("historyImageReviewEmpty").classList.remove("hidden");
        return;
      }

      const statusMap = {
        AGREES: ["1차 판독과 일치", "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"],
        DISAGREES: ["판독 결과 불일치", "border-rose-500/30 bg-rose-500/10 text-rose-300"],
        INCONCLUSIVE: ["판단 보류", "border-amber-500/30 bg-amber-500/10 text-amber-300"]
      };
      const status = statusMap[String(data.crossCheckStatus || "INCONCLUSIVE").toUpperCase()] || statusMap.INCONCLUSIVE;
      const badge = document.getElementById("historyImageReviewStatus");
      badge.textContent = status[0];
      badge.className = "rounded-full border px-3 py-1 text-xs font-semibold " + status[1];
      document.getElementById("historyImageReviewConclusion").textContent = data.combinedConclusion || "";
      document.getElementById("historyImageReviewSummary").textContent = data.visualSummary || "";

      const indicators = document.getElementById("historyImageReviewIndicators");
      indicators.innerHTML = "";
      (Array.isArray(data.visualIndicators) ? data.visualIndicators : []).forEach(function(item) {
        const li = document.createElement("li");
        li.className = "flex gap-2";
        const marker = document.createElement("span");
        marker.className = "mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-300";
        const text = document.createElement("span");
        text.textContent = item;
        li.appendChild(marker);
        li.appendChild(text);
        indicators.appendChild(li);
      });
      const meta = [data.model, data.confidenceLevel ? "신뢰 수준 " + data.confidenceLevel : "",
        Number.isFinite(data.totalTokens) ? "생성 시 총 " + data.totalTokens + " 토큰" : ""]
        .filter(Boolean);
      document.getElementById("historyImageReviewMeta").textContent = meta.join(" · ");
      document.getElementById("historyImageReviewResult").classList.remove("hidden");
    }

    async function loadSavedAiResults(id) {
      resetSavedAiResults();
      const requests = [
        ["/explanation", renderSavedAiExplanation, "historyAiExplanationLoading", "historyAiExplanationEmpty"],
        ["/image-review", renderSavedImageReview, "historyImageReviewLoading", "historyImageReviewEmpty"]
      ];

      await Promise.all(requests.map(async function(request) {
        try {
          const response = await fetch(
            contextPath + "/api/v1/ai/verifications/" + encodeURIComponent(id) + request[0],
            { method: "GET", credentials: "same-origin" }
          );
          const payload = await response.json();
          if (!selectedItem || String(selectedItem.id) !== String(id)) return;
          request[1](response.ok && payload.success ? payload.data : null);
        } catch (error) {
          if (!selectedItem || String(selectedItem.id) !== String(id)) return;
          finishSavedAiLoading(request[2]);
          document.getElementById(request[3]).classList.remove("hidden");
        }
      }));
      lucide.createIcons();
    }

    function resetHistoryReviewRequest() {
      ["historyReviewRequestForm", "historyReviewRequestResult", "historyReviewRequestError"]
        .forEach(function(id) { document.getElementById(id).classList.add("hidden"); });
      const loading = document.getElementById("historyReviewRequestLoading");
      loading.classList.remove("hidden");
      loading.classList.add("flex");
      document.getElementById("historyReviewRequestReason").value = "";
      document.getElementById("historyReviewRequestReasonCount").textContent = "0 / 1000";
      document.getElementById("historyReviewRequestType").value = "RECHECK";
    }

    function finishHistoryReviewRequestLoading() {
      const loading = document.getElementById("historyReviewRequestLoading");
      loading.classList.add("hidden");
      loading.classList.remove("flex");
    }

    function historyReviewRequestTypeLabel(requestType) {
      const labels = {
        FALSE_POSITIVE: "오탐 신고: 실제인데 조작으로 판정",
        FALSE_NEGATIVE: "미탐 신고: 조작인데 실제로 판정",
        RECHECK: "판정 재검토"
      };
      return labels[String(requestType || "RECHECK").toUpperCase()] || labels.RECHECK;
    }

    function renderHistoryReviewRequest(data) {
      finishHistoryReviewRequestLoading();
      document.getElementById("historyReviewRequestForm").classList.add("hidden");
      document.getElementById("historyReviewRequestError").classList.add("hidden");

      const statusMap = {
        PENDING: ["접수 완료", "border-amber-500/30 bg-amber-500/10 text-amber-300"],
        REVIEWING: ["검토 중", "border-sky-500/30 bg-sky-500/10 text-sky-300"],
        COMPLETED: ["검토 완료", "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"],
        REJECTED: ["요청 반려", "border-rose-500/30 bg-rose-500/10 text-rose-300"]
      };
      const status = statusMap[String(data.status || "PENDING").toUpperCase()] || statusMap.PENDING;
      const badge = document.getElementById("historyReviewRequestStatus");
      badge.textContent = status[0];
      badge.className = "rounded-full border px-3 py-1 text-xs font-semibold " + status[1];

      document.getElementById("historyReviewRequestTypeText").textContent = historyReviewRequestTypeLabel(data.requestType);
      document.getElementById("historyReviewRequestReasonText").textContent = data.reason || "작성된 요청 사유가 없습니다.";
      const reviewerNote = String(data.reviewerNote || "").trim();
      document.getElementById("historyReviewerNoteText").textContent = reviewerNote;
      document.getElementById("historyReviewerNoteWrap").classList.toggle("hidden", reviewerNote.length === 0);

      const meta = [];
      if (data.id != null) meta.push("요청 번호 " + data.id);
      if (data.regDt) meta.push("접수 " + data.regDt);
      if (data.updDt && data.updDt !== data.regDt) meta.push("변경 " + data.updDt);
      document.getElementById("historyReviewRequestMeta").textContent = meta.join(" · ");
      document.getElementById("historyReviewRequestResult").classList.remove("hidden");
    }

    function showHistoryReviewRequestForm() {
      finishHistoryReviewRequestLoading();
      document.getElementById("historyReviewRequestResult").classList.add("hidden");
      document.getElementById("historyReviewRequestForm").classList.remove("hidden");
    }

    function showHistoryReviewRequestError(message) {
      finishHistoryReviewRequestLoading();
      const errorBox = document.getElementById("historyReviewRequestError");
      errorBox.textContent = message;
      errorBox.classList.remove("hidden");
    }

    async function loadSavedReviewRequest(id) {
      resetHistoryReviewRequest();
      try {
        const response = await fetch(contextPath + "/api/v1/review-requests", {
          method: "GET",
          credentials: "same-origin"
        });
        const payload = await response.json();
        if (!selectedItem || String(selectedItem.id) !== String(id)) return;
        if (!response.ok || !payload.success) {
          throw new Error(payload.error && payload.error.message || "재검토 요청 상태를 불러오지 못했습니다.");
        }
        const requests = Array.isArray(payload.data) ? payload.data : [];
        const savedRequest = requests.find(function(item) {
          return String(item.verificationId) === String(id);
        });
        if (savedRequest) renderHistoryReviewRequest(savedRequest);
        else showHistoryReviewRequestForm();
      } catch (error) {
        if (!selectedItem || String(selectedItem.id) !== String(id)) return;
        showHistoryReviewRequestError(error.message || "재검토 요청 상태를 불러오지 못했습니다.");
      }
      lucide.createIcons();
    }

    async function submitHistoryReviewRequest(event) {
      event.preventDefault();
      if (!selectedItem) return;
      const requestId = selectedItem.id;
      const reasonInput = document.getElementById("historyReviewRequestReason");
      const reason = reasonInput.value.trim();
      if (!reason) {
        showHistoryReviewRequestError("재검토 요청 사유를 입력해 주세요.");
        reasonInput.focus();
        return;
      }

      const button = document.getElementById("historyReviewRequestSubmitButton");
      button.disabled = true;
      button.innerHTML = '<i data-lucide="loader-circle" class="h-4 w-4 animate-spin"></i><span>접수 중</span>';
      document.getElementById("historyReviewRequestError").classList.add("hidden");
      lucide.createIcons();
      try {
        const response = await fetch(
          contextPath + "/api/v1/verifications/" + encodeURIComponent(requestId) + "/review-requests",
          {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              requestType: document.getElementById("historyReviewRequestType").value,
              reason: reason
            })
          }
        );
        const payload = await response.json();
        if (!response.ok || !payload.success || !payload.data) {
          throw new Error(payload.error && payload.error.message || "재검토 요청을 접수하지 못했습니다.");
        }
        if (selectedItem && String(selectedItem.id) === String(requestId)) renderHistoryReviewRequest(payload.data);
      } catch (error) {
        if (selectedItem && String(selectedItem.id) === String(requestId)) {
          showHistoryReviewRequestError(error.message || "재검토 요청을 접수하지 못했습니다.");
        }
      } finally {
        button.disabled = false;
        button.innerHTML = '<i data-lucide="send" class="h-4 w-4"></i><span>재검토 요청 접수</span>';
        lucide.createIcons();
      }
    }

    function openDetail(id) {
      selectedItem = rawHistory.find(function(item) { return String(item.id) === String(id); }) || null;
      if (!selectedItem) return;

      resetSavedAiResults();
      resetHistoryReviewRequest();

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
          loadSavedAiResults(id);
          loadSavedReviewRequest(id);
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
    document.getElementById("detailTabOverlay").addEventListener("click", function() {
      detailTab = "overlay";
      applyDetailTabStyles();
    });
    document.getElementById("detailDeleteButton").addEventListener("click", openDeleteModal);
    document.getElementById("historyReviewRequestForm").addEventListener("submit", submitHistoryReviewRequest);
    document.getElementById("historyReviewRequestReason").addEventListener("input", function() {
      document.getElementById("historyReviewRequestReasonCount").textContent = this.value.length + " / 1000";
    });
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

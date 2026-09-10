<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  String contextPath = request.getContextPath();
  String userName = (String) session.getAttribute("USER_NAME");
  Object userId = session.getAttribute("USER_ID");
  boolean isAuthenticated = (userId != null);
  request.setAttribute("activePage", "home");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DeepScan</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700;800&display=swap" rel="stylesheet">
  <style>
    html { scroll-behavior: smooth; }
    body {
      font-family: "Pretendard", "Noto Sans KR", sans-serif;
      background: #020617;
    }
    .glass-panel {
      background: rgba(15, 23, 42, 0.5);
      backdrop-filter: blur(20px);
    }
    .float-orb {
      animation: orbFloat 18s ease-in-out infinite;
      will-change: transform;
    }
    .float-orb.orb-delay-1 {
      animation-duration: 22s;
      animation-delay: -4s;
    }
    .float-orb.orb-delay-2 {
      animation-duration: 28s;
      animation-delay: -8s;
    }
    .reveal {
      opacity: 0;
      transform: translateY(28px) scale(0.985);
      transition:
        opacity 0.7s ease,
        transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
      transition-delay: var(--reveal-delay, 0ms);
      will-change: opacity, transform;
    }
    .reveal.reveal-visible {
      opacity: 1;
      transform: translateY(0) scale(1);
    }
    .reveal-x {
      opacity: 0;
      transform: translateX(24px);
      transition:
        opacity 0.7s ease,
        transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
      transition-delay: var(--reveal-delay, 0ms);
    }
    .reveal-x.reveal-visible {
      opacity: 1;
      transform: translateX(0);
    }
    .hero-pop {
      opacity: 0;
      transform: translateY(24px) scale(0.96);
      animation: heroPop 0.8s cubic-bezier(0.22, 1, 0.36, 1) forwards;
      animation-delay: var(--hero-delay, 0ms);
    }
    .particle-hero {
      min-height: min(860px, 100vh);
      isolation: isolate;
      background:
        radial-gradient(circle at 50% 46%, rgba(14, 165, 233, 0.11), transparent 34%),
        linear-gradient(180deg, #020617 0%, #020617 72%, #07111f 100%);
    }
    .particle-hero::after {
      content: "";
      position: absolute;
      inset: auto 0 0;
      height: 30%;
      pointer-events: none;
      background: linear-gradient(180deg, transparent, #020617);
      z-index: 1;
    }
    .particle-canvas {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      opacity: 0.96;
      pointer-events: none;
    }
    .hero-vignette {
      position: absolute;
      inset: 0;
      z-index: 1;
      pointer-events: none;
      background:
        linear-gradient(90deg, rgba(2, 6, 23, 0.92), transparent 25%, transparent 75%, rgba(2, 6, 23, 0.92)),
        radial-gradient(circle at center, transparent 12%, rgba(2, 6, 23, 0.18) 53%, rgba(2, 6, 23, 0.85) 100%);
    }
    .hero-copy-shadow {
      text-shadow: 0 4px 36px rgba(2, 6, 23, 0.95);
    }
    .hero-status {
      box-shadow: inset 0 1px rgba(255,255,255,0.08), 0 18px 55px rgba(2, 6, 23, 0.4);
    }
    .card-hover-lift {
      transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
    }
    .card-hover-lift:hover {
      transform: translateY(-6px);
      box-shadow: 0 24px 60px rgba(14, 165, 233, 0.16);
    }
    .loading-stage {
      position: relative;
      overflow: hidden;
    }
    .hero-visual-grid {
      background-image:
        linear-gradient(rgba(148, 163, 184, 0.08) 1px, transparent 1px),
        linear-gradient(90deg, rgba(148, 163, 184, 0.08) 1px, transparent 1px);
      background-size: 32px 32px;
      mask-image: radial-gradient(circle at center, black 28%, transparent 88%);
    }
    .hero-visual-orb {
      animation: heroOrb 9s ease-in-out infinite;
    }
    .hero-visual-orb.delay-1 {
      animation-delay: -3s;
    }
    .hero-visual-orb.delay-2 {
      animation-delay: -6s;
    }
    .loading-ring {
      width: 88px;
      height: 88px;
      border-radius: 9999px;
      border: 8px solid rgba(255, 255, 255, 0.08);
      border-top-color: #38bdf8;
      border-right-color: #22d3ee;
      animation: spin 1s linear infinite;
      box-shadow: 0 0 32px rgba(14, 165, 233, 0.28);
    }
    .loading-stage .loading-ring {
      display: none;
    }
    .loading-stage .text-center p:last-child {
      display: none;
    }
    .loading-stage .text-center p:first-child {
      font-size: 1.875rem;
      line-height: 2.25rem;
    }
    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
    @keyframes orbFloat {
      0%, 100% { transform: translate3d(0, 0, 0) scale(1) rotate(0deg); }
      33% { transform: translate3d(0, -28px, 0) scale(1.06) rotate(8deg); }
      66% { transform: translate3d(0, 22px, 0) scale(0.96) rotate(-6deg); }
    }
    @keyframes heroPop {
      from {
        opacity: 0;
        transform: translateY(24px) scale(0.96);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }
    @keyframes heroOrb {
      0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
      50% { transform: translate3d(0, -16px, 0) scale(1.06); }
    }
    @media (max-width: 767px) {
      .particle-hero { min-height: 760px; }
      .hero-vignette {
        background: radial-gradient(circle at center, transparent 5%, rgba(2, 6, 23, 0.34) 54%, rgba(2, 6, 23, 0.92) 100%);
      }
    }
    @media (prefers-reduced-motion: reduce) {
      .hero-pop { animation: none; opacity: 1; transform: none; }
      .float-orb, .hero-visual-orb { animation: none; }
    }
  </style>
</head>
<body class="min-h-screen bg-slate-950 text-white overflow-x-hidden">
  <div id="toastRoot" class="pointer-events-none fixed left-1/2 top-6 z-[100] hidden w-full max-w-md -translate-x-1/2 px-4">
    <div id="toastCard" class="rounded-2xl border border-white/10 bg-slate-900/95 px-6 py-4 shadow-2xl backdrop-blur-xl transition-all">
      <div class="flex items-start gap-3">
        <div id="toastIconWrap" class="mt-0.5 flex h-10 w-10 items-center justify-center rounded-xl bg-sky-500/15 text-sky-400">
          <i id="toastIcon" data-lucide="check-circle" class="h-5 w-5"></i>
        </div>
        <div class="min-w-0 flex-1">
          <p id="toastTitle" class="font-semibold text-white">알림</p>
          <p id="toastDescription" class="mt-1 text-sm text-slate-300"></p>
        </div>
      </div>
    </div>
  </div>
  <div class="min-h-screen bg-slate-950 text-white">
    <%@ include file="common/dashboard-nav.jspf" %>

    <section class="particle-hero relative flex items-center overflow-hidden px-4 pb-16 pt-32">
      <canvas id="particleCanvas" class="particle-canvas" aria-hidden="true"></canvas>
      <div class="hero-vignette"></div>

      <div class="relative z-10 mx-auto w-full max-w-7xl text-center">
        <div class="text-center">
          <div class="hero-pop mb-8 inline-flex items-center gap-2 rounded-full border border-sky-300/20 bg-slate-950/45 px-5 py-2 backdrop-blur-xl hero-status" style="--hero-delay: 80ms;">
            <span class="relative flex h-2.5 w-2.5">
              <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-cyan-300 opacity-60"></span>
              <span class="relative inline-flex h-2.5 w-2.5 rounded-full bg-cyan-300"></span>
            </span>
            <span class="text-sm font-medium tracking-wide text-slate-200">AI 기반 이미지 검증 플랫폼</span>
          </div>
          <h1 class="hero-pop hero-copy-shadow mx-auto max-w-5xl text-5xl font-extrabold leading-[1.08] tracking-[-0.045em] text-white md:text-7xl lg:text-[5.5rem]" style="--hero-delay: 180ms;">
            보이지 않는 조작까지,<br>
            <span class="bg-gradient-to-r from-sky-300 via-cyan-300 to-blue-400 bg-clip-text text-transparent">AI가 정밀하게 검증합니다</span>
          </h1>
          <p class="hero-pop hero-copy-shadow mx-auto mt-8 max-w-2xl text-lg leading-8 text-slate-300 md:text-xl" style="--hero-delay: 280ms;">
            이미지 속 미세한 흔적을 다각도로 분석해<br class="hidden sm:block">
            딥페이크 가능성과 판단 근거를 빠르게 제공합니다.
          </p>
          <div class="hero-pop mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row" style="--hero-delay: 380ms;">
            <a href="#upload" class="group inline-flex items-center gap-3 rounded-full bg-white px-8 py-4 text-base font-bold text-slate-950 shadow-2xl shadow-sky-950/60 transition hover:-translate-y-0.5 hover:bg-sky-50">
              이미지 검증하기
              <i data-lucide="arrow-right" class="h-5 w-5 transition-transform group-hover:translate-x-1"></i>
            </a>
            <a href="#features" class="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-8 py-4 text-base font-semibold text-white backdrop-blur-md transition hover:border-white/30 hover:bg-white/10">서비스 알아보기</a>
          </div>
          <div class="hero-pop mx-auto mt-16 grid max-w-3xl grid-cols-3 divide-x divide-white/10 border-t border-white/10 pt-7" style="--hero-delay: 480ms;">
            <div><strong class="block text-xl text-white md:text-2xl">98.7%</strong><span class="mt-1 block text-xs text-slate-500 md:text-sm">탐지 정확도</span></div>
            <div><strong class="block text-xl text-white md:text-2xl">1.2초</strong><span class="mt-1 block text-xs text-slate-500 md:text-sm">평균 분석 시간</span></div>
            <div><strong class="block text-xl text-white md:text-2xl">10,000+</strong><span class="mt-1 block text-xs text-slate-500 md:text-sm">검증된 이미지</span></div>
          </div>
        </div>

        <div class="hidden max-w-6xl mx-auto mb-16 reveal" style="--reveal-delay: 120ms;" data-reveal>
          <div class="relative card-hover-lift">
            <div class="absolute inset-0 bg-gradient-to-r from-sky-600/20 via-cyan-500/20 to-blue-600/20 rounded-[2rem] blur-3xl"></div>
            <main class="relative h-[320px] md:h-[440px] glass-panel border border-white/10 rounded-[2rem] loading-stage overflow-hidden">
              <div class="hero-visual-grid absolute inset-0"></div>
              <div class="absolute left-[14%] top-[18%] h-28 w-28 rounded-full bg-sky-500/20 blur-2xl hero-visual-orb"></div>
              <div class="absolute right-[16%] top-[20%] h-24 w-24 rounded-full bg-cyan-400/20 blur-2xl hero-visual-orb delay-1"></div>
              <div class="absolute bottom-[20%] left-[24%] h-32 w-32 rounded-full bg-blue-500/20 blur-2xl hero-visual-orb delay-2"></div>
              <div class="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(56,189,248,0.10),rgba(2,6,23,0)_60%)] pointer-events-none z-10"></div>
              <div class="relative z-20 flex h-full items-center justify-center px-8">
                <div class="w-full max-w-4xl">
                  <div class="grid gap-6 md:grid-cols-[1.2fr_0.8fr] md:items-center">
                    <div class="space-y-5">
                      <div class="inline-flex items-center gap-2 rounded-full border border-sky-400/20 bg-sky-500/10 px-4 py-2 text-sm font-medium text-sky-300">
                        <i data-lucide="scan-face" class="h-4 w-4"></i>
                        AI Vision Monitor
                      </div>
                      <div>
                        <h3 class="mb-3 text-3xl font-bold tracking-tight text-white md:text-4xl">딥페이크 탐지 흐름을 한눈에 확인하세요</h3>
                        <p class="max-w-xl text-base leading-7 text-slate-300 md:text-lg">이미지 업로드부터 분석 결과 확인까지 이어지는 검증 과정을 시각 요소 중심으로 정리했습니다.</p>
                      </div>
                      <div class="flex flex-wrap gap-3 text-sm text-slate-300">
                        <div class="rounded-full border border-white/10 bg-white/5 px-4 py-2">실시간 분석 상태</div>
                        <div class="rounded-full border border-white/10 bg-white/5 px-4 py-2">신뢰도 점수 안내</div>
                        <div class="rounded-full border border-white/10 bg-white/5 px-4 py-2">근거 시각화</div>
                      </div>
                    </div>
                    <div class="rounded-[1.75rem] border border-white/10 bg-slate-950/55 p-5 shadow-2xl shadow-sky-900/20">
                      <div class="mb-4 flex items-center justify-between">
                        <div>
                          <p class="text-xs uppercase tracking-[0.24em] text-slate-500">Analysis Preview</p>
                          <p class="mt-1 text-lg font-semibold text-white">DeepScan Engine</p>
                        </div>
                        <div class="flex gap-2">
                          <span class="h-2.5 w-2.5 rounded-full bg-sky-400"></span>
                          <span class="h-2.5 w-2.5 rounded-full bg-cyan-400"></span>
                          <span class="h-2.5 w-2.5 rounded-full bg-blue-400"></span>
                        </div>
                      </div>
                      <div class="space-y-3">
                        <div class="rounded-2xl border border-white/10 bg-white/5 p-4">
                          <div class="mb-2 flex items-center justify-between text-sm">
                            <span class="text-slate-400">검증 진행률</span>
                            <span class="font-medium text-sky-300">92%</span>
                          </div>
                          <div class="h-2.5 rounded-full bg-slate-800">
                            <div class="h-2.5 w-[92%] rounded-full bg-gradient-to-r from-sky-500 to-cyan-400"></div>
                          </div>
                        </div>
                        <div class="grid grid-cols-2 gap-3">
                          <div class="rounded-2xl border border-white/10 bg-white/5 p-4">
                            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Confidence</p>
                            <p class="mt-2 text-2xl font-bold text-white">98.7%</p>
                          </div>
                          <div class="rounded-2xl border border-white/10 bg-white/5 p-4">
                            <p class="text-xs uppercase tracking-[0.2em] text-slate-500">Status</p>
                            <p class="mt-2 text-2xl font-bold text-cyan-300">Stable</p>
                          </div>
                        </div>
                        <div class="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-slate-300">
                          핵심 의심 구역, 메타데이터 단서, 모델 판정 근거를 함께 제공합니다.
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </main>
          </div>
        </div>

        <div class="hidden grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
          <div class="relative group reveal" style="--reveal-delay: 0ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-sky-500 to-blue-500 rounded-3xl blur-xl opacity-50 group-hover:opacity-75 transition-opacity"></div>
            <div class="relative glass-panel p-8 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift">
              <div class="text-5xl font-bold mb-3 bg-gradient-to-r from-sky-500 to-blue-500 bg-clip-text text-transparent">98.7%</div>
              <div class="text-slate-400 font-medium">탐지 정확도</div>
            </div>
          </div>
          <div class="relative group reveal" style="--reveal-delay: 90ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-cyan-500 to-teal-500 rounded-3xl blur-xl opacity-50 group-hover:opacity-75 transition-opacity"></div>
            <div class="relative glass-panel p-8 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift">
              <div class="text-5xl font-bold mb-3 bg-gradient-to-r from-cyan-500 to-teal-500 bg-clip-text text-transparent">1.2초</div>
              <div class="text-slate-400 font-medium">평균 분석 시간</div>
            </div>
          </div>
          <div class="relative group reveal" style="--reveal-delay: 180ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-blue-500 to-indigo-500 rounded-3xl blur-xl opacity-50 group-hover:opacity-75 transition-opacity"></div>
            <div class="relative glass-panel p-8 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift">
              <div class="text-5xl font-bold mb-3 bg-gradient-to-r from-blue-500 to-indigo-500 bg-clip-text text-transparent">10,000+</div>
              <div class="text-slate-400 font-medium">검증된 이미지</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="upload" class="py-32 px-4 relative">
      <div class="max-w-6xl mx-auto">
        <div class="text-center mb-16 reveal" data-reveal>
          <h2 class="text-5xl md:text-6xl font-bold mb-6">
            검증 <span class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-transparent">시작하기</span>
          </h2>
          <p class="text-xl text-slate-400 max-w-2xl mx-auto">이미지를 업로드하거나 URL을 입력하여 즉시 AI 분석을 받아보세요.</p>
        </div>

        <div class="relative group reveal" style="--reveal-delay: 120ms;" data-reveal>
          <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-20 group-hover:opacity-30 transition-opacity"></div>
          <div class="relative glass-panel rounded-3xl p-8 border border-white/10 card-hover-lift">
            <div class="flex gap-3 mb-8">
              <button type="button" id="fileTab" onclick="setUploadMethod('file')" class="flex-1 flex items-center justify-center gap-3 px-6 py-4 rounded-2xl transition-all font-semibold bg-gradient-to-r from-sky-600 to-cyan-600 text-white shadow-lg shadow-sky-500/50">
                <i data-lucide="upload" class="w-5 h-5"></i>
                파일 업로드              </button>
              <button type="button" id="urlTab" onclick="setUploadMethod('url')" class="flex-1 flex items-center justify-center gap-3 px-6 py-4 rounded-2xl transition-all font-semibold bg-white/5 text-slate-400 hover:bg-white/10">
                <i data-lucide="link" class="w-5 h-5"></i>
                URL 입력
              </button>
            </div>

            <div id="fileUploadPanel">
              <div id="dropZone" class="border-2 border-dashed rounded-2xl p-16 text-center transition-all border-white/20 hover:border-white/30 bg-white/5">
                <div id="fileEmptyState">
                  <div class="relative inline-block mb-6">
                    <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-full blur-xl opacity-50"></div>
                    <div class="relative w-20 h-20 bg-gradient-to-br from-sky-500 to-cyan-500 rounded-full flex items-center justify-center mx-auto">
                      <i data-lucide="upload" class="w-10 h-10 text-white"></i>
                    </div>
                  </div>
                  <p class="text-2xl mb-3 font-semibold">이미지를 드래그하거나 업로드</p>
                  <p class="text-slate-400 mb-6">또는</p>
                  <label class="inline-block">
                    <input id="fileInput" type="file" accept="image/*" class="hidden">
                    <span class="px-8 py-3 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white rounded-full cursor-pointer inline-block transition-all shadow-lg shadow-sky-500/50 font-semibold">파일 선택</span>
                  </label>
                  <p class="text-sm text-slate-500 mt-6">JPG, PNG 지원 (최대 10MB)</p>
                </div>

                <div id="filePreviewState" class="hidden space-y-6">
                  <div class="relative inline-block">
                    <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-2xl blur-xl opacity-50"></div>
                    <img id="filePreviewImage" src="" alt="Preview" class="relative max-h-80 mx-auto rounded-2xl shadow-2xl">
                  </div>
                  <p id="fileNameLabel" class="text-slate-400"></p>
                  <button type="button" onclick="clearFileSelection()" class="text-sky-400 hover:text-sky-300 font-medium">다른 파일 선택</button>
                </div>
              </div>
            </div>

            <div id="urlUploadPanel" class="hidden">
              <div class="border-2 border-dashed border-white/20 rounded-2xl p-16 text-center">
                <div class="relative inline-block mb-6">
                  <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-full blur-xl opacity-50"></div>
                  <div class="relative w-20 h-20 bg-gradient-to-br from-sky-500 to-cyan-500 rounded-full flex items-center justify-center mx-auto">
                    <i data-lucide="link" class="w-10 h-10 text-white"></i>
                  </div>
                </div>
                <p class="text-2xl mb-6 font-semibold">이미지 URL 입력</p>
                <input id="imageUrlInput" type="url" placeholder="https://example.com/image.jpg" class="w-full px-6 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 mb-6 backdrop-blur-xl">
                <div id="urlPreviewWrap" class="hidden mt-8">
                  <div class="relative inline-block">
                    <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-2xl blur-xl opacity-50"></div>
                    <img id="urlPreviewImage" src="" alt="Preview" class="relative max-h-80 mx-auto rounded-2xl shadow-2xl">
                  </div>
                </div>
                <p class="text-sm text-slate-500 mt-6">이미지 파일의 직접 링크를 입력하세요.</p>
              </div>
            </div>

            <div id="errorBox" class="hidden mt-6 p-4 bg-red-500/10 border border-red-500/50 rounded-xl text-red-400 text-center"></div>

            <button id="analyzeButton" type="button" onclick="handleAnalyze()" class="hidden w-full mt-8 px-8 py-5 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white rounded-2xl transition-all items-center justify-center gap-3 shadow-2xl shadow-sky-500/50 font-bold text-lg">
              <i data-lucide="zap" class="w-6 h-6"></i>
              이미지 분석하기
            </button>
          </div>
        </div>
      </div>
    </section>

    <section id="features" class="py-32 px-4 relative">
      <div class="max-w-7xl mx-auto">
        <div class="text-center mb-20 reveal" data-reveal>
          <h2 class="text-5xl md:text-6xl font-bold mb-6">
            강력한            <span class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-transparent">기능</span>
          </h2>
          <p class="text-xl text-slate-400 max-w-2xl mx-auto">조작된 콘텐츠로부터 사용자를 보호하는 첨단 AI 기능</p>
        </div>

        <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div class="relative group reveal" style="--reveal-delay: 0ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-sky-500 to-blue-500 rounded-3xl blur-xl opacity-0 group-hover:opacity-50 transition-opacity"></div>
            <div class="relative glass-panel p-8 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift h-full">
              <div class="w-14 h-14 bg-gradient-to-r from-sky-500 to-blue-500 rounded-2xl flex items-center justify-center mb-6"><i data-lucide="brain" class="w-7 h-7 text-white"></i></div>
              <h3 class="text-xl font-bold mb-3">AI 탐지</h3>
              <p class="text-slate-400 leading-relaxed">대규모 이미지 데이터로 학습한 모델이 미세한 조작 흔적까지 정밀하게 감지합니다.</p>
            </div>
          </div>
          <div class="relative group reveal" style="--reveal-delay: 70ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-cyan-500 to-teal-500 rounded-3xl blur-xl opacity-0 group-hover:opacity-50 transition-opacity"></div>
            <div class="relative glass-panel p-8 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift h-full">
              <div class="w-14 h-14 bg-gradient-to-r from-cyan-500 to-teal-500 rounded-2xl flex items-center justify-center mb-6"><i data-lucide="zap" class="w-7 h-7 text-white"></i></div>
              <h3 class="text-xl font-bold mb-3">즉시 결과</h3>
              <p class="text-slate-400 leading-relaxed">빠른 분석과 함께 신뢰도 점수, 판정 결과, 핵심 근거를 즉시 제공합니다.</p>
            </div>
          </div>
          <div class="relative group reveal" style="--reveal-delay: 140ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-blue-500 to-indigo-500 rounded-3xl blur-xl opacity-0 group-hover:opacity-50 transition-opacity"></div>
            <div class="relative glass-panel p-8 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift h-full">
              <div class="w-14 h-14 bg-gradient-to-r from-blue-500 to-indigo-500 rounded-2xl flex items-center justify-center mb-6"><i data-lucide="eye" class="w-7 h-7 text-white"></i></div>
              <h3 class="text-xl font-bold mb-3">시각적 분석</h3>
              <p class="text-slate-400 leading-relaxed">의심 영역을 오버레이 방식으로 표시해 어떤 부분이 문제인지 바로 확인할 수 있습니다.</p>
            </div>
          </div>
          <div class="relative group reveal" style="--reveal-delay: 210ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-green-500 to-emerald-500 rounded-3xl blur-xl opacity-0 group-hover:opacity-50 transition-opacity"></div>
            <div class="relative glass-panel p-8 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift h-full">
              <div class="w-14 h-14 bg-gradient-to-r from-green-500 to-emerald-500 rounded-2xl flex items-center justify-center mb-6"><i data-lucide="fingerprint" class="w-7 h-7 text-white"></i></div>
              <h3 class="text-xl font-bold mb-3">메타데이터 확인</h3>
              <p class="text-slate-400 leading-relaxed">파일 정보와 생성 흔적을 함께 확인해 이미지 신뢰도를 다각도로 점검합니다.</p>
            </div>
          </div>
          <div class="relative group reveal" style="--reveal-delay: 280ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-orange-500 to-red-500 rounded-3xl blur-xl opacity-0 group-hover:opacity-50 transition-opacity"></div>
            <div class="relative glass-panel p-8 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift h-full">
              <div class="w-14 h-14 bg-gradient-to-r from-orange-500 to-red-500 rounded-2xl flex items-center justify-center mb-6"><i data-lucide="shield" class="w-7 h-7 text-white"></i></div>
              <h3 class="text-xl font-bold mb-3">보안 및 프라이버시</h3>
              <p class="text-slate-400 leading-relaxed">업로드 데이터는 안전하게 처리되며, 분석 과정에서 개인정보 보호를 우선합니다.</p>
            </div>
          </div>
          <div class="relative group reveal" style="--reveal-delay: 350ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-indigo-500 to-violet-500 rounded-3xl blur-xl opacity-0 group-hover:opacity-50 transition-opacity"></div>
            <div class="relative glass-panel p-8 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift h-full">
              <div class="w-14 h-14 bg-gradient-to-r from-indigo-500 to-violet-500 rounded-2xl flex items-center justify-center mb-6"><i data-lucide="globe" class="w-7 h-7 text-white"></i></div>
              <h3 class="text-xl font-bold mb-3">API 연동</h3>
              <p class="text-slate-400 leading-relaxed">탐지 기능을 외부 서비스와 연결해 자체 워크플로우에 통합할 수 있습니다.</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="py-32 px-4 relative">
      <div class="max-w-5xl mx-auto">
        <div class="text-center mb-20 reveal" data-reveal>
          <h2 class="text-5xl md:text-6xl font-bold mb-6">
            이용
            <span class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-transparent">방법</span>
          </h2>
          <p class="text-xl text-slate-400">간단한 3단계로 이미지를 검증해보세요</p>
        </div>

        <div class="space-y-12">
          <div class="relative group reveal" style="--reveal-delay: 0ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-xl opacity-0 group-hover:opacity-30 transition-opacity"></div>
            <div class="relative glass-panel p-10 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift flex items-start gap-8">
              <div class="flex-shrink-0"><div class="text-6xl font-bold text-white/10">01</div></div>
              <div class="flex-1">
                <div class="flex items-center gap-4 mb-4">
                  <div class="w-12 h-12 bg-gradient-to-r from-sky-500 to-cyan-500 rounded-xl flex items-center justify-center"><i data-lucide="upload" class="w-6 h-6 text-white"></i></div>
                  <h3 class="text-2xl font-bold">이미지 업로드</h3>
                </div>
                <p class="text-slate-400 text-lg leading-relaxed">이미지를 업로드하거나 URL을 입력하세요. 일반적인 이미지 형식을 폭넓게 지원합니다.</p>
              </div>
            </div>
          </div>
          <div class="relative group reveal" style="--reveal-delay: 100ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-xl opacity-0 group-hover:opacity-30 transition-opacity"></div>
            <div class="relative glass-panel p-10 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift flex items-start gap-8">
              <div class="flex-shrink-0"><div class="text-6xl font-bold text-white/10">02</div></div>
              <div class="flex-1">
                <div class="flex items-center gap-4 mb-4">
                  <div class="w-12 h-12 bg-gradient-to-r from-sky-500 to-cyan-500 rounded-xl flex items-center justify-center"><i data-lucide="brain" class="w-6 h-6 text-white"></i></div>
                  <h3 class="text-2xl font-bold">AI 분석</h3>
                </div>
                <p class="text-slate-400 text-lg leading-relaxed">AI가 조작 패턴, 불일치 신호, 딥페이크 징후를 자동으로 분석합니다.</p>
              </div>
            </div>
          </div>
          <div class="relative group reveal" style="--reveal-delay: 200ms;" data-reveal>
            <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-xl opacity-0 group-hover:opacity-30 transition-opacity"></div>
            <div class="relative glass-panel p-10 rounded-3xl border border-white/10 hover:border-white/20 card-hover-lift flex items-start gap-8">
              <div class="flex-shrink-0"><div class="text-6xl font-bold text-white/10">03</div></div>
              <div class="flex-1">
                <div class="flex items-center gap-4 mb-4">
                  <div class="w-12 h-12 bg-gradient-to-r from-sky-500 to-cyan-500 rounded-xl flex items-center justify-center"><i data-lucide="check-circle" class="w-6 h-6 text-white"></i></div>
                  <h3 class="text-2xl font-bold">결과 확인</h3>
                </div>
                <p class="text-slate-400 text-lg leading-relaxed">신뢰도 점수와 시각적 근거를 포함한 상세 결과를 바로 확인하세요.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="py-32 px-4 relative">
      <div class="max-w-4xl mx-auto text-center reveal" data-reveal>
        <div class="relative group">
          <div class="absolute inset-0 bg-gradient-to-r from-sky-600 via-cyan-600 to-blue-600 rounded-3xl blur-3xl opacity-40"></div>
          <div class="relative bg-gradient-to-r from-sky-600/20 via-cyan-600/20 to-blue-600/20 backdrop-blur-xl p-16 rounded-3xl border border-white/20">
            <h2 class="text-5xl md:text-6xl font-bold mb-6">지금 검증하시겠어요?</h2>
            <p class="text-xl text-slate-300 mb-10 max-w-2xl mx-auto">딥페이크와 조작된 콘텐츠로부터 스스로를 보호할 수 있도록 지금 바로 분석을 시작해보세요.</p>
            <a href="#upload" class="inline-flex items-center gap-3 px-10 py-5 bg-white text-slate-900 hover:bg-slate-100 rounded-full transition-all shadow-2xl font-bold text-lg">
              지금 분석 시작하기
              <i data-lucide="arrow-right" class="w-6 h-6"></i>
            </a>
          </div>
        </div>
      </div>
    </section>

    <footer class="py-12 px-4 border-t border-white/10">
      <div class="max-w-7xl mx-auto">
        <div class="flex flex-col md:flex-row justify-between items-center gap-6">
          <div class="flex items-center gap-3">
            <img src="<%= contextPath %>/resources/image/deepscan-mark.svg?v=30" alt="" class="h-10 w-10" width="40" height="40">
            <span class="text-xl font-bold"><span class="text-white">Deep</span><span class="text-cyan-400">Scan</span></span>
          </div>
          <div class="flex gap-8 text-sm text-slate-400">
            <button type="button" onclick="goPage('<%= contextPath %>/news')" class="hover:text-white transition-colors">뉴스</button>
            <button type="button" onclick="goPage('<%= contextPath %>/community')" class="hover:text-white transition-colors">커뮤니티</button>
            <button type="button" onclick="goPage('<%= contextPath %>/report')" class="hover:text-white transition-colors">신고하기</button>
            <button type="button" onclick="goPage('<%= contextPath %>/faq')" class="hover:text-white transition-colors">FAQ</button>
          </div>
          <p class="text-sm text-slate-500">© 2026 DeepScan. 디지털 이미지의 신뢰를 지킵니다.</p>
        </div>
      </div>
    </footer>
  </div>

  <script>
    const contextPath = "<%= contextPath %>";
    let selectedFile = null;
    let previewUrl = "";
    let objectUrl = "";

    function goPage(path) {
      window.location.href = path;
    }

    function showToast(options) {
      const root = document.getElementById("toastRoot");
      const iconWrap = document.getElementById("toastIconWrap");
      const icon = document.getElementById("toastIcon");
      const title = document.getElementById("toastTitle");
      const description = document.getElementById("toastDescription");
      const type = options && options.type ? options.type : "success";

      title.textContent = options && options.title ? options.title : "알림";
      description.textContent = options && options.description ? options.description : "";

      if (type === "info") {
        iconWrap.className = "mt-0.5 flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-500/15 text-cyan-400";
        icon.setAttribute("data-lucide", "log-out");
      } else {
        iconWrap.className = "mt-0.5 flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/15 text-emerald-400";
        icon.setAttribute("data-lucide", "check-circle");
      }

      root.classList.remove("hidden");
      lucide.createIcons();

      window.clearTimeout(window.__appToastTimer);
      window.__appToastTimer = window.setTimeout(function () {
        root.classList.add("hidden");
      }, 2600);
    }

    async function performLogout() {
      try {
        await fetch(contextPath + "/api/v1/auth/logout", {
          method: "POST",
          credentials: "same-origin"
        });
      } finally {
        sessionStorage.setItem("appToast", JSON.stringify({
          type: "info",
          title: "로그아웃되었습니다",
          description: "안전하게 로그아웃되었습니다."
        }));
        window.location.href = contextPath + "/";
      }
    }

    function setUploadMethod(method) {
      const fileTab = document.getElementById("fileTab");
      const urlTab = document.getElementById("urlTab");
      const filePanel = document.getElementById("fileUploadPanel");
      const urlPanel = document.getElementById("urlUploadPanel");
      const urlInput = document.getElementById("imageUrlInput");

      if (method === "file") {
        fileTab.className = "flex-1 flex items-center justify-center gap-3 px-6 py-4 rounded-2xl transition-all font-semibold bg-gradient-to-r from-sky-600 to-cyan-600 text-white shadow-lg shadow-sky-500/50";
        urlTab.className = "flex-1 flex items-center justify-center gap-3 px-6 py-4 rounded-2xl transition-all font-semibold bg-white/5 text-slate-400 hover:bg-white/10";
        filePanel.classList.remove("hidden");
        urlPanel.classList.add("hidden");
        urlInput.value = "";
        hideUrlPreview();
      } else {
        urlTab.className = "flex-1 flex items-center justify-center gap-3 px-6 py-4 rounded-2xl transition-all font-semibold bg-gradient-to-r from-sky-600 to-cyan-600 text-white shadow-lg shadow-sky-500/50";
        fileTab.className = "flex-1 flex items-center justify-center gap-3 px-6 py-4 rounded-2xl transition-all font-semibold bg-white/5 text-slate-400 hover:bg-white/10";
        urlPanel.classList.remove("hidden");
        filePanel.classList.add("hidden");
        clearFileSelection();
      }

      setError("");
      updateAnalyzeVisibility();
    }

    function setError(message) {
      const errorBox = document.getElementById("errorBox");
      if (!message) {
        errorBox.classList.add("hidden");
        errorBox.textContent = "";
        return;
      }

      errorBox.textContent = message;
      errorBox.classList.remove("hidden");
    }

    function handleFileSelect(file) {
      setError("");
      if (!file) {
        return;
      }
      if (!file.type.startsWith("image/")) {
        setError("사진 파일만 업로드할 수 있습니다. JPG, PNG, WEBP 형식의 이미지를 선택해주세요.");
        return;
      }
      if (file.size > 10 * 1024 * 1024) {
        setError("사진 파일 크기는 10MB 이하여야 합니다.");
        return;
      }

      selectedFile = file;
      previewUrl = "";
      hideUrlPreview();

      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
      objectUrl = URL.createObjectURL(file);
      document.getElementById("filePreviewImage").src = objectUrl;
      document.getElementById("fileNameLabel").textContent = file.name;
      document.getElementById("fileEmptyState").classList.add("hidden");
      document.getElementById("filePreviewState").classList.remove("hidden");
      updateAnalyzeVisibility();
    }

    function clearFileSelection() {
      selectedFile = null;
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
        objectUrl = "";
      }
      document.getElementById("fileInput").value = "";
      document.getElementById("filePreviewImage").src = "";
      document.getElementById("fileNameLabel").textContent = "";
      document.getElementById("filePreviewState").classList.add("hidden");
      document.getElementById("fileEmptyState").classList.remove("hidden");
      updateAnalyzeVisibility();
    }

    function hideUrlPreview() {
      const urlPreviewImage = document.getElementById("urlPreviewImage");
      previewUrl = "";
      document.getElementById("urlPreviewWrap").classList.add("hidden");
      urlPreviewImage.removeAttribute("src");
    }

    function updateAnalyzeVisibility() {
      const button = document.getElementById("analyzeButton");
      const shouldShow = Boolean(selectedFile || previewUrl);
      button.classList.toggle("hidden", !shouldShow);
      button.classList.toggle("flex", shouldShow);
    }

    async function buildFileFromUrl(url) {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error("이미지 URL을 불러올 수 없습니다. 주소가 올바른지 확인해주세요.");
      }

      const blob = await response.blob();
      if (!blob.type.startsWith("image/")) {
        throw new Error("URL 주소가 이미지 파일을 가리키지 않습니다. 실제 이미지 URL을 입력해주세요.");
      }

      const fileName = url.split("/").pop() || "url-image";
      return new File([blob], fileName, { type: blob.type || "image/jpeg" });
    }

    async function handleAnalyze() {
      if (!selectedFile && !previewUrl) {
        return;
      }

      setError("");
      const analyzeButton = document.getElementById("analyzeButton");
      const originalButtonHtml = analyzeButton.innerHTML;
      analyzeButton.disabled = true;
      analyzeButton.innerHTML = '<i data-lucide="loader-2" class="w-6 h-6 animate-spin"></i>분석 요청 중...';
      lucide.createIcons();

      try {
        let fileToUpload = selectedFile;
        if (!fileToUpload && previewUrl) {
          fileToUpload = await buildFileFromUrl(previewUrl);
        }

        const formData = new FormData();
        formData.append("file", fileToUpload);

        const response = await fetch(contextPath + "/api/v1/verifications", {
          method: "POST",
          credentials: "same-origin",
          body: formData
        });

        const data = await response.json().catch(function () {
          return {};
        });

        if (!response.ok || data.success === false) {
          throw new Error(
            (data && data.error && data.error.message)
            || data.message
            || "분석 요청에 실패했습니다."
          );
        }

        const payload = data && data.data ? data.data : data;
        const verificationId = payload.id || payload.verificationId;
        if (!verificationId) {
          throw new Error("분석 결과 ID를 확인할 수 없습니다.");
        }

        window.location.href = contextPath + "/analyzing?id=" + encodeURIComponent(verificationId);
      } catch (error) {
        setError(error.message || "분석 요청에 실패했습니다.");
      } finally {
        analyzeButton.disabled = false;
        analyzeButton.innerHTML = originalButtonHtml;
        lucide.createIcons();
      }
    }

    function initParticleHero() {
      const canvas = document.getElementById("particleCanvas");
      if (!canvas) return;

      const context = canvas.getContext("2d");
      const hero = canvas.parentElement;
      const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      const palette = [[56, 189, 248], [34, 211, 238], [99, 102, 241], [244, 114, 182]];
      let width = 0;
      let height = 0;
      let pixelRatio = 1;
      let animationFrame = 0;
      let pointerX = 0;
      let pointerY = 0;
      let targetPointerX = 0;
      let targetPointerY = 0;
      let particles = [];

      function createParticles() {
        const compact = width < 720;
        const count = compact ? 620 : Math.min(1450, Math.floor(width * 1.15));
        const goldenAngle = Math.PI * (3 - Math.sqrt(5));
        particles = Array.from({ length: count }, function (_, index) {
          const normalized = (index + 0.5) / count;
          const y = 1 - normalized * 2;
          const radius = Math.sqrt(1 - y * y);
          const theta = goldenAngle * index;
          const noise = 0.88 + Math.random() * 0.22;
          return {
            x: Math.cos(theta) * radius * noise,
            y: y * noise,
            z: Math.sin(theta) * radius * noise,
            size: 0.55 + Math.random() * 1.35,
            phase: Math.random() * Math.PI * 2,
            color: palette[index % palette.length]
          };
        });
      }

      function resizeCanvas() {
        const bounds = hero.getBoundingClientRect();
        width = Math.max(1, Math.round(bounds.width));
        height = Math.max(1, Math.round(bounds.height));
        pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
        canvas.width = Math.round(width * pixelRatio);
        canvas.height = Math.round(height * pixelRatio);
        canvas.style.width = width + "px";
        canvas.style.height = height + "px";
        context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
        createParticles();
      }

      function render(time) {
        const seconds = time * 0.001;
        pointerX += (targetPointerX - pointerX) * 0.035;
        pointerY += (targetPointerY - pointerY) * 0.035;
        context.clearRect(0, 0, width, height);

        const compact = width < 720;
        const scale = Math.min(width * (compact ? 0.46 : 0.31), height * (compact ? 0.28 : 0.36));
        const centerX = width * 0.5 + pointerX * 24;
        const centerY = height * (compact ? 0.43 : 0.46) + pointerY * 14;
        const rotationY = (reduceMotion ? 0.45 : seconds * 0.16) + pointerX * 0.28;
        const rotationX = -0.12 + pointerY * 0.16 + (reduceMotion ? 0 : Math.sin(seconds * 0.23) * 0.08);
        const cosY = Math.cos(rotationY);
        const sinY = Math.sin(rotationY);
        const cosX = Math.cos(rotationX);
        const sinX = Math.sin(rotationX);

        particles.forEach(function (particle) {
          const wave = reduceMotion ? 0 : Math.sin(seconds * 1.2 + particle.phase + particle.y * 4) * 0.055;
          const px = particle.x * (1 + wave);
          const py = particle.y * (0.7 + wave * 0.5);
          const pz = particle.z * (0.72 + wave);
          const rotatedX = px * cosY - pz * sinY;
          const rotatedZ = px * sinY + pz * cosY;
          const rotatedY = py * cosX - rotatedZ * sinX;
          const depth = py * sinX + rotatedZ * cosX;
          const perspective = 1.08 + depth * 0.22;
          const screenX = centerX + rotatedX * scale * 1.36 * perspective;
          const screenY = centerY + rotatedY * scale * perspective;
          const alpha = Math.max(0.16, Math.min(0.92, 0.48 + depth * 0.34));
          const color = particle.color;

          context.beginPath();
          context.fillStyle = "rgba(" + color[0] + "," + color[1] + "," + color[2] + "," + alpha + ")";
          context.arc(screenX, screenY, particle.size * perspective, 0, Math.PI * 2);
          context.fill();
        });

        if (!reduceMotion) animationFrame = window.requestAnimationFrame(render);
      }

      hero.addEventListener("pointermove", function (event) {
        const bounds = hero.getBoundingClientRect();
        targetPointerX = ((event.clientX - bounds.left) / bounds.width - 0.5) * 2;
        targetPointerY = ((event.clientY - bounds.top) / bounds.height - 0.5) * 2;
      }, { passive: true });
      hero.addEventListener("pointerleave", function () {
        targetPointerX = 0;
        targetPointerY = 0;
      });
      document.addEventListener("visibilitychange", function () {
        if (reduceMotion) return;
        if (document.hidden) window.cancelAnimationFrame(animationFrame);
        else animationFrame = window.requestAnimationFrame(render);
      });
      window.addEventListener("resize", resizeCanvas, { passive: true });

      resizeCanvas();
      if (reduceMotion) render(0);
      else animationFrame = window.requestAnimationFrame(render);
    }

    document.addEventListener("DOMContentLoaded", function () {
      try {
        const pendingToast = sessionStorage.getItem("appToast");
        if (pendingToast) {
          sessionStorage.removeItem("appToast");
          showToast(JSON.parse(pendingToast));
        }
      } catch (error) {
        sessionStorage.removeItem("appToast");
      }

      document.querySelectorAll(".logout-form").forEach(function (form) {
        form.addEventListener("submit", function (event) {
          event.preventDefault();
          performLogout();
        });
      });

      const dropZone = document.getElementById("dropZone");
      const fileInput = document.getElementById("fileInput");
      const urlInput = document.getElementById("imageUrlInput");
      const urlPreview = document.getElementById("urlPreviewImage");

      fileInput.addEventListener("change", function (event) {
        handleFileSelect(event.target.files && event.target.files[0]);
      });

      dropZone.addEventListener("dragover", function (event) {
        event.preventDefault();
        dropZone.className = "border-2 border-dashed rounded-2xl p-16 text-center transition-all border-sky-500 bg-sky-500/10";
      });

      dropZone.addEventListener("dragleave", function () {
        dropZone.className = "border-2 border-dashed rounded-2xl p-16 text-center transition-all border-white/20 hover:border-white/30 bg-white/5";
      });

      dropZone.addEventListener("drop", function (event) {
        event.preventDefault();
        dropZone.className = "border-2 border-dashed rounded-2xl p-16 text-center transition-all border-white/20 hover:border-white/30 bg-white/5";
        handleFileSelect(event.dataTransfer.files[0]);
      });

      urlInput.addEventListener("input", function () {
        const url = urlInput.value.trim();
        setError("");
        clearFileSelection();

        if (url && (url.startsWith("http://") || url.startsWith("https://"))) {
          previewUrl = url;
          urlPreview.src = url;
          document.getElementById("urlPreviewWrap").classList.remove("hidden");
        } else {
          hideUrlPreview();
        }

        updateAnalyzeVisibility();
      });

      urlPreview.addEventListener("error", function () {
        if (!previewUrl) {
          return;
        }
        setError("이미지 URL을 불러올 수 없습니다. 주소가 올바른지 확인해주세요.");
        hideUrlPreview();
        updateAnalyzeVisibility();
      });

      urlPreview.addEventListener("load", function () {
        if (!previewUrl) {
          return;
        }
        setError("");
        document.getElementById("urlPreviewWrap").classList.remove("hidden");
        updateAnalyzeVisibility();
      });

      const revealItems = Array.from(document.querySelectorAll("[data-reveal]"));
      if ("IntersectionObserver" in window) {
        const revealObserver = new IntersectionObserver(function (entries, observer) {
          entries.forEach(function (entry) {
            if (entry.isIntersecting) {
              entry.target.classList.add("reveal-visible");
              observer.unobserve(entry.target);
            }
          });
        }, {
          threshold: 0.16,
          rootMargin: "0px 0px -8% 0px"
        });

        revealItems.forEach(function (item) {
          revealObserver.observe(item);
        });
      } else {
        revealItems.forEach(function (item) {
          item.classList.add("reveal-visible");
        });
      }

      lucide.createIcons();
      updateAnalyzeVisibility();
      initParticleHero();
    });
  </script>
</body>
</html>





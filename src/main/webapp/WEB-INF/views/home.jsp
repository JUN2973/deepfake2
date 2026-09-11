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
    :root {
      color-scheme: dark;
      --page: #090b0e;
      --surface: #11151a;
      --surface-raised: #151a21;
      --line: #272d35;
      --muted: #929aa5;
      --accent: #3b82f6;
    }
    html { scroll-behavior: smooth; }
    body {
      font-family: "Pretendard", "Noto Sans KR", sans-serif;
      background: var(--page);
      color: #f3f4f6;
    }
    .surface {
      background: var(--surface);
      border: 1px solid var(--line);
      box-shadow: 0 18px 50px rgba(0, 0, 0, 0.22);
    }
    .eyebrow {
      color: #75a7ff;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.16em;
      text-transform: uppercase;
    }
    .reveal {
      opacity: 0;
      transform: translateY(18px);
      transition: opacity 0.55s ease, transform 0.55s cubic-bezier(0.22, 1, 0.36, 1);
      transition-delay: var(--reveal-delay, 0ms);
    }
    .reveal.reveal-visible { opacity: 1; transform: translateY(0); }
    .upload-dropzone { background: #0d1116; border-color: #343b45; }
    .upload-dropzone:hover { border-color: #4d8ff7; background: #101722; }
    .feature-row { border-top: 1px solid var(--line); }
    .feature-row:last-child { border-bottom: 1px solid var(--line); }
    .keep-words { word-break: keep-all; overflow-wrap: break-word; }
    .forensic-visual {
      position: absolute;
      top: 50%;
      left: max(-17rem, calc(50% - 58rem));
      width: min(68rem, 82vw);
      height: 100%;
      transform: translateY(-50%);
      opacity: 0.62;
      pointer-events: none;
      mask-image: linear-gradient(90deg, transparent 0%, #000 14%, #000 58%, rgba(0, 0, 0, 0.56) 76%, transparent 94%);
    }
    .forensic-visual::before {
      content: "";
      position: absolute;
      inset: 0;
      z-index: 4;
      background: radial-gradient(circle at 47% 45%, transparent 22%, rgba(9, 11, 14, 0.06) 55%, rgba(9, 11, 14, 0.9) 96%);
    }
    .forensic-visual::after {
      content: "";
      position: absolute;
      top: 18%;
      bottom: 18%;
      left: 38%;
      width: 1px;
      background: linear-gradient(180deg, transparent, rgba(120, 172, 255, 0.72), transparent);
      box-shadow: 0 0 28px rgba(59, 130, 246, 0.48);
      animation: spectralScan 9s cubic-bezier(0.45, 0, 0.55, 1) infinite;
    }
    .forensic-visual svg,
    .scan-sweep,
    .inspection-lens { display: none; }
    .optical-object {
      position: absolute;
      inset: 9% 12% 9% 7%;
      perspective: 900px;
      animation: opticalFloat 15s ease-in-out infinite alternate;
    }
    .optical-frame {
      position: absolute;
      width: 21rem;
      height: 27rem;
      overflow: hidden;
      border: 1px solid rgba(132, 161, 201, 0.24);
      border-radius: 1.4rem;
      background: linear-gradient(145deg, rgba(95, 129, 176, 0.11), rgba(13, 19, 27, 0.2) 46%, rgba(106, 146, 201, 0.06));
      box-shadow: inset 0 1px rgba(255,255,255,0.12), 0 28px 80px rgba(0,0,0,0.3);
      backdrop-filter: blur(2px);
    }
    .optical-frame::before {
      content: "";
      position: absolute;
      inset: 0;
      background:
        linear-gradient(128deg, transparent 0 38%, rgba(140, 184, 244, 0.14) 39%, transparent 41%),
        repeating-linear-gradient(0deg, transparent 0 23px, rgba(121, 157, 206, 0.07) 24px);
    }
    .optical-frame::after {
      content: "";
      position: absolute;
      inset: 17% 13%;
      border: 1px solid rgba(117, 167, 255, 0.2);
      border-radius: 50%;
      background: radial-gradient(circle, rgba(72, 125, 202, 0.16), transparent 61%);
    }
    .frame-one { left: 8%; top: 8%; transform: rotateY(24deg) rotateZ(-12deg); opacity: 0.38; }
    .frame-two { left: 19%; top: 14%; transform: rotateY(20deg) rotateZ(-4deg); opacity: 0.56; }
    .frame-three { left: 30%; top: 21%; transform: rotateY(15deg) rotateZ(5deg); opacity: 0.78; }
    .optical-lens {
      position: absolute;
      left: 30%;
      top: 29%;
      width: 16rem;
      height: 16rem;
      border: 1px solid rgba(143, 182, 235, 0.46);
      border-radius: 50%;
      background:
        radial-gradient(circle at 42% 38%, rgba(125, 176, 247, 0.16), transparent 23%),
        radial-gradient(circle, rgba(20, 43, 72, 0.28), rgba(5, 10, 16, 0.08) 68%);
      box-shadow: inset 0 0 0 12px rgba(95, 138, 198, 0.025), inset 0 0 45px rgba(79, 133, 211, 0.12), 0 24px 70px rgba(0,0,0,0.42);
    }
    .optical-lens::before,
    .optical-lens::after { content: ""; position: absolute; background: rgba(129, 173, 235, 0.22); }
    .optical-lens::before { left: 50%; top: 8%; bottom: 8%; width: 1px; }
    .optical-lens::after { left: 8%; right: 8%; top: 50%; height: 1px; }
    .crop-mark {
      position: absolute;
      width: 2.4rem;
      height: 2.4rem;
      border-color: rgba(116, 167, 240, 0.56);
    }
    .crop-a { left: 19%; top: 14%; border-left: 1px solid; border-top: 1px solid; }
    .crop-b { right: 18%; bottom: 15%; border-right: 1px solid; border-bottom: 1px solid; }
    @keyframes opticalFloat {
      from { transform: translate3d(-0.6rem, 0.8rem, 0) rotate(-0.7deg); }
      to { transform: translate3d(0.9rem, -0.7rem, 0) rotate(0.7deg); }
    }
    @keyframes spectralScan {
      0%, 100% { transform: translateX(-5rem); opacity: 0; }
      15%, 85% { opacity: 0.76; }
      50% { transform: translateX(18rem); opacity: 0.92; }
    }
    @media (max-width: 1023px) {
      .forensic-visual { left: -23rem; width: 62rem; opacity: 0.4; }
    }
    @media (max-width: 639px) {
      .forensic-visual { left: -27rem; top: 24rem; width: 58rem; height: 48rem; opacity: 0.32; }
    }
    @media (prefers-reduced-motion: reduce) {
      .reveal { opacity: 1; transform: none; transition: none; }
      .optical-object, .forensic-visual::after { animation: none; }
    }
  </style>
</head>
<body class="min-h-screen overflow-x-hidden bg-[#090b0e] text-white">
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
  <div class="min-h-screen bg-[#090b0e] text-white">
    <%@ include file="common/dashboard-nav.jspf" %>

    <main>
      <section id="upload" class="relative isolate overflow-hidden border-b border-[#272d35] px-4 pb-20 pt-32 md:pb-28 md:pt-40">
        <div class="forensic-visual" aria-hidden="true">
          <div class="optical-object">
            <div class="optical-frame frame-one"></div>
            <div class="optical-frame frame-two"></div>
            <div class="optical-frame frame-three"></div>
            <div class="optical-lens"></div>
            <div class="crop-mark crop-a"></div>
            <div class="crop-mark crop-b"></div>
          </div>
          <svg viewBox="0 0 760 760" role="presentation">
            <g class="forensic-lines">
              <path class="major" d="M378 68C231 68 139 180 139 348c0 168 91 307 239 344 148-37 239-176 239-344C617 180 525 68 378 68Z"/>
              <path d="M378 91c-130 0-215 103-215 261 0 149 82 273 215 314 133-41 215-165 215-314 0-158-85-261-215-261Z"/>
              <path d="M378 119c-111 0-188 89-188 236 0 132 73 241 188 281 115-40 188-149 188-281 0-147-77-236-188-236Z"/>
              <path class="quiet" d="M378 149c-96 0-161 79-161 209 0 112 63 207 161 245 98-38 161-133 161-245 0-130-65-209-161-209Z"/>
              <path d="M245 263c33-27 74-39 116-29M395 234c42-10 83 2 116 29"/>
              <path class="major" d="M252 293c31-27 73-27 105 0-32 19-74 19-105 0ZM399 293c32-27 74-27 106 0-32 19-74 19-106 0Z"/>
              <path d="M378 275c-8 62-19 113-36 154 21 18 50 20 72 0-17-41-28-92-36-154Z"/>
              <path class="quiet" d="M291 370c23 20 47 28 72 22M393 392c25 6 49-2 72-22M298 477c49 36 111 36 160 0M322 493c36 17 76 17 112 0"/>
              <path d="M213 333c-31 55-38 119-19 180M543 333c31 55 38 119 19 180"/>
              <path class="quiet" d="M179 223c55-16 105-16 151 1M426 224c46-17 96-17 151-1M163 407c63 22 117 23 163 4M430 411c46 19 100 18 163-4"/>
              <path d="M204 546c55-18 107-13 154 15M398 561c47-28 99-33 154-15"/>
              <path class="quiet" d="M118 348h520M139 407h478M160 467h436M193 527h370M238 587h280"/>
              <path class="quiet" d="M378 68v624M298 86c-22 163-22 365 0 565M458 86c22 163 22 365 0 565"/>
            </g>
            <g class="forensic-nodes" opacity="0.78">
              <circle cx="305" cy="292" r="2.8"/><circle cx="452" cy="292" r="2.8"/><circle cx="378" cy="429" r="2.8"/><circle cx="378" cy="489" r="2.8"/>
              <rect x="231" y="268" width="145" height="60" rx="5"/><rect x="326" y="395" width="104" height="72" rx="5"/>
            </g>
          </svg>
          <div class="scan-sweep"></div>
          <div class="inspection-lens"></div>
        </div>
        <div class="relative z-10 mx-auto grid max-w-7xl gap-12 lg:grid-cols-[0.88fr_1.12fr] lg:items-center lg:gap-20">
          <div class="reveal" data-reveal>
            <p class="eyebrow mb-5">Digital image verification</p>
            <h1 class="keep-words max-w-2xl text-5xl font-bold leading-[1.08] tracking-[-0.045em] text-white md:text-6xl">
              이미지의 조작 흔적을 확인하세요.
            </h1>
            <p class="keep-words mt-7 max-w-xl text-lg leading-8 text-[#a9b0ba]">
              픽셀 패턴과 메타데이터, 모델 판정을 종합해 의심 영역과 판단 근거를 하나의 리포트로 정리합니다.
            </p>
            <div class="mt-9 flex flex-wrap gap-x-6 gap-y-3 text-sm text-[#a9b0ba]">
              <span class="inline-flex items-center gap-2"><i data-lucide="layers-3" class="h-4 w-4 text-blue-400"></i>다중 신호 분석</span>
              <span class="inline-flex items-center gap-2"><i data-lucide="scan-search" class="h-4 w-4 text-blue-400"></i>의심 영역 시각화</span>
              <span class="inline-flex items-center gap-2"><i data-lucide="file-text" class="h-4 w-4 text-blue-400"></i>상세 리포트</span>
            </div>
            <p class="mt-10 border-l-2 border-[#3b82f6] pl-4 text-sm leading-6 text-[#7f8792]">
              분석 결과는 판단을 돕는 참고 자료이며, 원본 여부를 법적으로 확정하는 증명서는 아닙니다.
            </p>
          </div>

          <div class="surface reveal rounded-2xl p-4 sm:p-6" style="--reveal-delay: 90ms;" data-reveal>
            <div class="mb-5 flex items-center justify-between border-b border-[#272d35] pb-5">
              <div>
                <p class="text-sm font-semibold text-white">새 이미지 검증</p>
                <p class="mt-1 text-xs text-[#7f8792]">JPG, PNG, WEBP · 최대 10MB</p>
              </div>
              <span class="rounded-md border border-[#303844] bg-[#0d1116] px-2.5 py-1 text-xs font-medium text-[#929aa5]">안전한 업로드</span>
            </div>

            <div class="mb-5 grid grid-cols-2 gap-1 rounded-lg bg-[#0d1116] p-1">
              <button type="button" id="fileTab" onclick="setUploadMethod('file')" class="flex items-center justify-center gap-2 rounded-md bg-[#242b34] px-4 py-3 text-sm font-semibold text-white transition-colors">
                <i data-lucide="upload" class="h-4 w-4"></i>파일 업로드
              </button>
              <button type="button" id="urlTab" onclick="setUploadMethod('url')" class="flex items-center justify-center gap-2 rounded-md px-4 py-3 text-sm font-semibold text-[#929aa5] transition-colors hover:text-white">
                <i data-lucide="link" class="h-4 w-4"></i>URL 입력
              </button>
            </div>

            <div id="fileUploadPanel">
              <div id="dropZone" class="upload-dropzone rounded-xl border border-dashed px-6 py-14 text-center transition-colors sm:py-16">
                <div id="fileEmptyState">
                  <div class="mx-auto mb-5 flex h-12 w-12 items-center justify-center rounded-xl border border-[#343b45] bg-[#151a21] text-blue-400">
                    <i data-lucide="image-up" class="h-6 w-6"></i>
                  </div>
                  <p class="text-lg font-semibold text-white">검증할 이미지를 놓아주세요</p>
                  <p class="mt-2 text-sm text-[#7f8792]">드래그하거나 컴퓨터에서 파일을 선택할 수 있습니다.</p>
                  <label class="mt-6 inline-block">
                    <input id="fileInput" type="file" accept="image/*" class="hidden">
                    <span class="inline-block cursor-pointer rounded-lg bg-[#3b82f6] px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-[#4d8ff7]">파일 선택</span>
                  </label>
                </div>
                <div id="filePreviewState" class="hidden space-y-4">
                  <img id="filePreviewImage" src="" alt="선택한 이미지 미리보기" class="mx-auto max-h-72 rounded-lg border border-[#272d35] object-contain">
                  <p id="fileNameLabel" class="truncate text-sm text-[#929aa5]"></p>
                  <button type="button" onclick="clearFileSelection()" class="text-sm font-semibold text-blue-400 hover:text-blue-300">다른 파일 선택</button>
                </div>
              </div>
            </div>

            <div id="urlUploadPanel" class="hidden">
              <div class="upload-dropzone rounded-xl border border-dashed px-6 py-12 text-center">
                <div class="mx-auto mb-5 flex h-12 w-12 items-center justify-center rounded-xl border border-[#343b45] bg-[#151a21] text-blue-400">
                  <i data-lucide="link" class="h-6 w-6"></i>
                </div>
                <label for="imageUrlInput" class="mb-3 block text-left text-sm font-semibold text-white">이미지 주소</label>
                <input id="imageUrlInput" type="url" placeholder="https://example.com/image.jpg" class="w-full rounded-lg border border-[#343b45] bg-[#090b0e] px-4 py-3 text-white placeholder-[#59616c] outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20">
                <div id="urlPreviewWrap" class="hidden mt-6">
                  <img id="urlPreviewImage" src="" alt="URL 이미지 미리보기" class="mx-auto max-h-64 rounded-lg border border-[#272d35] object-contain">
                </div>
                <p class="mt-3 text-left text-xs text-[#7f8792]">공개적으로 접근할 수 있는 이미지 직접 링크를 입력해주세요.</p>
              </div>
            </div>

            <div id="errorBox" class="hidden mt-4 rounded-lg border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300"></div>
            <button id="analyzeButton" type="button" onclick="handleAnalyze()" class="hidden mt-5 w-full items-center justify-center gap-2 rounded-lg bg-[#3b82f6] px-6 py-3.5 text-base font-semibold text-white transition-colors hover:bg-[#4d8ff7] disabled:cursor-wait disabled:opacity-60">
              <i data-lucide="scan-line" class="h-5 w-5"></i>이미지 검증 시작
            </button>
          </div>
        </div>
      </section>

      <section id="features" class="px-4 py-24 md:py-32">
        <div class="mx-auto max-w-7xl">
          <div class="grid gap-12 lg:grid-cols-[0.8fr_1.2fr] lg:gap-24">
            <div class="reveal lg:sticky lg:top-32 lg:self-start" data-reveal>
              <p class="eyebrow mb-4">What you get</p>
              <h2 class="text-4xl font-bold tracking-[-0.035em] text-white md:text-5xl">판정만 보여주지 않습니다.</h2>
              <p class="mt-5 max-w-md text-base leading-7 text-[#929aa5]">결과를 이해하고 다음 행동을 판단할 수 있도록 서로 다른 분석 신호를 함께 제공합니다.</p>
              <a href="#upload" class="mt-8 inline-flex items-center gap-2 rounded-lg border border-[#343b45] px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:border-[#59616c] hover:bg-[#11151a]">이미지 검증하기<i data-lucide="arrow-up-right" class="h-4 w-4"></i></a>
            </div>

            <div>
              <article class="feature-row reveal grid gap-5 py-8 sm:grid-cols-[72px_1fr]" data-reveal>
                <span class="font-mono text-sm text-[#59616c]">01</span>
                <div><div class="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-blue-500/10 text-blue-400"><i data-lucide="scan-search" class="h-5 w-5"></i></div><h3 class="text-2xl font-semibold text-white">의심 영역 시각화</h3><p class="mt-3 leading-7 text-[#929aa5]">조작 가능성이 높은 위치를 이미지 위에 표시해 판정이 나온 이유를 직접 확인할 수 있습니다.</p></div>
              </article>
              <article class="feature-row reveal grid gap-5 py-8 sm:grid-cols-[72px_1fr]" style="--reveal-delay: 70ms;" data-reveal>
                <span class="font-mono text-sm text-[#59616c]">02</span>
                <div><div class="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-blue-500/10 text-blue-400"><i data-lucide="fingerprint" class="h-5 w-5"></i></div><h3 class="text-2xl font-semibold text-white">다각도 검증 근거</h3><p class="mt-3 leading-7 text-[#929aa5]">모델 판정과 파일 정보, 생성 흔적을 함께 살펴 단일 점수에 의존하지 않는 결과를 제공합니다.</p></div>
              </article>
              <article class="feature-row reveal grid gap-5 py-8 sm:grid-cols-[72px_1fr]" style="--reveal-delay: 140ms;" data-reveal>
                <span class="font-mono text-sm text-[#59616c]">03</span>
                <div><div class="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-blue-500/10 text-blue-400"><i data-lucide="file-check-2" class="h-5 w-5"></i></div><h3 class="text-2xl font-semibold text-white">보관 가능한 분석 기록</h3><p class="mt-3 leading-7 text-[#929aa5]">검증 이력을 다시 확인하고 필요한 경우 상세 결과를 리포트 형태로 정리할 수 있습니다.</p></div>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section class="border-y border-[#272d35] bg-[#0c0f13] px-4 py-20">
        <div class="reveal mx-auto flex max-w-7xl flex-col gap-8 md:flex-row md:items-center md:justify-between" data-reveal>
          <div><p class="eyebrow mb-3">Start verification</p><h2 class="text-3xl font-bold tracking-tight text-white md:text-4xl">확인이 필요한 이미지가 있나요?</h2><p class="mt-3 text-[#929aa5]">이미지를 올리면 분석부터 결과 정리까지 한 번에 진행됩니다.</p></div>
          <a href="#upload" class="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-white px-6 py-3 text-sm font-bold text-[#090b0e] transition-colors hover:bg-[#e5e7eb]">검증 시작하기<i data-lucide="arrow-up" class="h-4 w-4"></i></a>
        </div>
      </section>
    </main>

    <footer class="border-t border-[#272d35] px-4 py-10">
      <div class="max-w-7xl mx-auto">
        <div class="flex flex-col md:flex-row justify-between items-center gap-6">
          <div class="flex items-center gap-3">
            <img src="<%= contextPath %>/resources/image/deepscan-mark.svg?v=30" alt="" class="h-10 w-10" width="40" height="40">
            <span class="text-xl font-bold"><span class="text-white">Deep</span><span class="text-blue-400">Scan</span></span>
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
        fileTab.className = "flex items-center justify-center gap-2 rounded-md bg-[#242b34] px-4 py-3 text-sm font-semibold text-white transition-colors";
        urlTab.className = "flex items-center justify-center gap-2 rounded-md px-4 py-3 text-sm font-semibold text-[#929aa5] transition-colors hover:text-white";
        filePanel.classList.remove("hidden");
        urlPanel.classList.add("hidden");
        urlInput.value = "";
        hideUrlPreview();
      } else {
        urlTab.className = "flex items-center justify-center gap-2 rounded-md bg-[#242b34] px-4 py-3 text-sm font-semibold text-white transition-colors";
        fileTab.className = "flex items-center justify-center gap-2 rounded-md px-4 py-3 text-sm font-semibold text-[#929aa5] transition-colors hover:text-white";
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
        dropZone.className = "upload-dropzone rounded-xl border border-dashed border-blue-500 bg-blue-500/5 px-6 py-14 text-center transition-colors sm:py-16";
      });

      dropZone.addEventListener("dragleave", function () {
        dropZone.className = "upload-dropzone rounded-xl border border-dashed px-6 py-14 text-center transition-colors sm:py-16";
      });

      dropZone.addEventListener("drop", function (event) {
        event.preventDefault();
        dropZone.className = "upload-dropzone rounded-xl border border-dashed px-6 py-14 text-center transition-colors sm:py-16";
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





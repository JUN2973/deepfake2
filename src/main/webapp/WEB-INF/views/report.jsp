<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  체크리스트 기준 주석: 구현(신고): 신고 안내, 지도 API 호출, 경찰서 마커 표시 화면을 구성한다.
--%>

<%--
  발표용 설명: 신고 안내 화면입니다.
  Kakao Map API로 주변 경찰서 위치를 표시하고 사용자가 신고 절차를 확인할 수 있게 합니다.
--%>
<%
  String contextPath = request.getContextPath();
  request.setAttribute("activePage", "report");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>신고하기 - DeepScan</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <script id="kakaoMapSdk" type="text/javascript" src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=7860854fd6e8b88d76dd14c9f65cb678&autoload=false"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700;800&display=swap" rel="stylesheet">
  <style>
    body {
      font-family: "Pretendard", "Noto Sans KR", sans-serif;
      background: #020617;
    }

    .station-locator-layout {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      gap: 20px;
    }

    .station-action-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      min-height: 48px;
      padding: 0 22px;
      border-radius: 999px;
      background: linear-gradient(135deg, #0ea5e9, #22d3ee);
      color: #fff;
      font-weight: 700;
      box-shadow: 0 18px 32px rgba(14, 165, 233, 0.24);
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }

    .station-action-button:hover {
      transform: translateY(-1px);
      box-shadow: 0 22px 40px rgba(14, 165, 233, 0.3);
    }

    .station-outline-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      min-height: 48px;
      padding: 0 22px;
      border-radius: 999px;
      color: #e2e8f0;
      border: 1px solid rgba(148, 163, 184, 0.22);
      background: rgba(255, 255, 255, 0.04);
      transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
    }

    .station-outline-button:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(148, 163, 184, 0.34);
      color: #fff;
    }

    .station-search-row {
      display: flex;
      align-items: center;
      gap: 12px;
      width: 100%;
      max-width: 480px;
    }

    .station-search-input {
      flex: 1;
      min-height: 48px;
      padding: 0 18px;
      border-radius: 999px;
      border: 1px solid rgba(148, 163, 184, 0.22);
      background: rgba(255, 255, 255, 0.04);
      color: #fff;
      outline: none;
      transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
    }

    .station-search-input::placeholder {
      color: #94a3b8;
    }

    .station-search-input:focus {
      border-color: rgba(56, 189, 248, 0.42);
      box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.15);
      background: rgba(255, 255, 255, 0.06);
    }

    .station-panel {
      position: relative;
      background: rgba(15, 23, 42, 0.55);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 24px;
      backdrop-filter: blur(18px);
      overflow: hidden;
    }

    .station-map {
      width: 100%;
      height: 420px;
      border-radius: 20px;
      overflow: hidden;
      border: 1px solid rgba(255, 255, 255, 0.08);
      background: rgba(2, 6, 23, 0.85);
    }

    .station-map-empty {
      display: flex;
      align-items: center;
      justify-content: center;
      flex-direction: column;
      gap: 10px;
      height: 100%;
      color: #94a3b8;
      background: radial-gradient(circle at top, rgba(14, 165, 233, 0.12), transparent 52%), rgba(2, 6, 23, 0.92);
    }

    .station-info-card {
      margin-top: 16px;
      padding: 18px 20px;
      border-radius: 20px;
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid rgba(255, 255, 255, 0.08);
    }

    .station-list-panel {
      max-height: 510px;
      overflow-y: auto;
    }

    .crime-stat-card {
      position: relative;
      overflow: hidden;
      transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease, background-color 0.24s ease;
    }

    .crime-stat-card::after {
      content: "";
      position: absolute;
      inset: 0;
      opacity: 0;
      transition: opacity 0.24s ease;
      pointer-events: none;
    }

    .crime-stat-card:hover {
      transform: translateY(-4px);
    }

    .crime-stat-card:hover::after {
      opacity: 1;
    }

    .crime-stat-card-red:hover {
      border-color: rgba(248, 113, 113, 0.55);
      box-shadow: 0 20px 44px rgba(239, 68, 68, 0.16);
      background-color: rgba(127, 29, 29, 0.18);
    }

    .crime-stat-card-red::after {
      background: radial-gradient(circle at top left, rgba(248, 113, 113, 0.18), transparent 55%);
    }

    .crime-stat-card-blue:hover {
      border-color: rgba(96, 165, 250, 0.55);
      box-shadow: 0 20px 44px rgba(59, 130, 246, 0.16);
      background-color: rgba(30, 64, 175, 0.18);
    }

    .crime-stat-card-blue::after {
      background: radial-gradient(circle at top left, rgba(96, 165, 250, 0.18), transparent 55%);
    }

    .crime-stat-card-purple:hover {
      border-color: rgba(192, 132, 252, 0.55);
      box-shadow: 0 20px 44px rgba(168, 85, 247, 0.16);
      background-color: rgba(88, 28, 135, 0.18);
    }

    .crime-stat-card-purple::after {
      background: radial-gradient(circle at top left, rgba(192, 132, 252, 0.18), transparent 55%);
    }

    .crime-info-card {
      transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease, background-color 0.24s ease;
    }

    .crime-info-card:hover {
      transform: translateY(-3px);
      border-color: rgba(125, 211, 252, 0.26);
      box-shadow: 0 18px 36px rgba(14, 165, 233, 0.12);
      background-color: rgba(14, 165, 233, 0.06);
    }

    .crime-warning-card {
      transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease, background-color 0.24s ease;
    }

    .crime-warning-card:hover {
      transform: translateY(-3px);
      border-color: rgba(250, 204, 21, 0.52);
      box-shadow: 0 18px 36px rgba(234, 179, 8, 0.14);
      background-color: rgba(250, 204, 21, 0.14);
    }

    .station-list-panel::-webkit-scrollbar {
      width: 10px;
    }

    .station-list-panel::-webkit-scrollbar-thumb {
      background: rgba(100, 116, 139, 0.8);
      border-radius: 9999px;
      border: 2px solid transparent;
      background-clip: padding-box;
    }

    .station-item {
      width: 100%;
      display: block;
      text-align: left;
      padding: 18px;
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 18px;
      background: rgba(255, 255, 255, 0.03);
      transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
    }

    .station-item:hover,
    .station-item.is-selected {
      border-color: rgba(56, 189, 248, 0.42);
      box-shadow: 0 14px 32px rgba(14, 165, 233, 0.14);
      transform: translateY(-1px);
    }

    .station-rank {
      width: 32px;
      height: 32px;
      border-radius: 9999px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 700;
      flex-shrink: 0;
    }

    .station-rank.is-top {
      background: linear-gradient(135deg, #0ea5e9, #22d3ee);
      color: #fff;
    }

    .station-rank:not(.is-top) {
      background: rgba(255, 255, 255, 0.08);
      color: #cbd5e1;
    }

    .custom-overlay {
      background: rgba(2, 6, 23, 0.94);
      color: #fff;
      padding: 12px 14px;
      border-radius: 14px;
      border: 1px solid rgba(56, 189, 248, 0.35);
      box-shadow: 0 14px 30px rgba(2, 8, 23, 0.44);
      font-size: 12px;
      line-height: 1.5;
      min-width: 200px;
    }

    @media (min-width: 1024px) {
      .station-locator-layout {
        grid-template-columns: minmax(0, 1.25fr) minmax(340px, 0.75fr);
      }
    }
  </style>
</head>
<body class="ds-page report-page min-h-screen bg-slate-950 text-white">
  <div id="submittedView" class="report-submitted hidden min-h-screen bg-slate-950 flex items-center justify-center p-4">
    <div class="text-center">
      <div class="relative inline-block mb-6">
        <div class="absolute inset-0 bg-gradient-to-r from-green-600 to-emerald-600 rounded-full blur-xl opacity-75"></div>
        <div class="relative w-20 h-20 bg-gradient-to-br from-green-500 to-emerald-500 rounded-full flex items-center justify-center mx-auto">
          <i data-lucide="check-circle" class="w-12 h-12 text-white"></i>
        </div>
      </div>
      <h2 class="text-3xl font-bold text-white mb-4">신고가 접수되었습니다</h2>
      <p class="text-slate-400 mb-2">빠른 시일 내에 검토하겠습니다.</p>
      <p class="text-slate-500 text-sm">잠시 후 홈으로 이동합니다.</p>
    </div>
  </div>

  <div id="mainView" class="min-h-screen bg-slate-950 text-white">
    <%@ include file="common/dashboard-nav.jspf" %>

    <div class="pt-24 pb-12 px-4">
      <div class="max-w-6xl mx-auto">
        <div class="report-hero text-center mb-12">
          <div class="relative inline-block mb-6">
            <div class="report-hero-glow absolute inset-0 bg-gradient-to-r from-red-600 to-orange-600 rounded-2xl blur-xl opacity-75"></div>
            <div class="report-hero-mark relative w-16 h-16 bg-gradient-to-br from-red-500 to-orange-500 rounded-2xl flex items-center justify-center mx-auto">
              <i data-lucide="alert-triangle" class="w-8 h-8 text-white"></i>
            </div>
          </div>
          <h1 class="text-4xl md:text-5xl font-bold mb-4">딥페이크 신고</h1>
          <p class="text-slate-400 max-w-2xl mx-auto">
            딥페이크, 허위정보, 사이버 범죄를 발견하셨나요?<br>
            즉시 사이버수사기관에 신고하여 안전한 온라인 환경을 만들어 주세요.
          </p>
        </div>

        <div class="grid lg:grid-cols-2 gap-8 mb-12">
          <div class="relative group h-full">
            <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-0 group-hover:opacity-20 transition-opacity"></div>
            <div class="report-panel relative h-full bg-slate-900/50 backdrop-blur-xl rounded-2xl p-8 border border-white/10 hover:border-white/20 transition-all flex flex-col">
            <div class="flex items-center gap-3 mb-6">
              <i data-lucide="shield" class="w-8 h-8 text-sky-400"></i>
              <h2 class="text-2xl font-bold">경찰청 사이버안전국</h2>
            </div>

            <div class="space-y-6 flex-1">
              <div class="flex items-start gap-4">
                <div class="w-12 h-12 bg-sky-500/20 rounded-lg flex items-center justify-center flex-shrink-0 border border-sky-500/30">
                  <i data-lucide="phone" class="w-6 h-6 text-sky-400"></i>
                </div>
                <div class="flex-1">
                  <h3 class="text-lg font-semibold mb-2">전화 신고</h3>
                  <p class="text-3xl text-sky-400 mb-2 font-bold">182</p>
                  <p class="text-sm text-slate-400">
                    24시간 운영 (국번 없이 182)<br>
                    사이버범죄 신고 및 상담
                  </p>
                </div>
              </div>

              <div class="flex items-start gap-4">
                <div class="w-12 h-12 bg-cyan-500/20 rounded-lg flex items-center justify-center flex-shrink-0 border border-cyan-500/30">
                  <i data-lucide="globe" class="w-6 h-6 text-cyan-400"></i>
                </div>
                <div class="flex-1">
                  <h3 class="text-lg font-semibold mb-2">온라인 신고</h3>
                  <a href="https://ecrm.cyber.go.kr" target="_blank" rel="noopener noreferrer" class="text-cyan-400 hover:text-cyan-300 underline break-all transition-colors">
                    https://ecrm.cyber.go.kr
                  </a>
                  <p class="text-sm text-slate-400 mt-2">
                    경찰청 사이버범죄 신고 시스템<br>
                    온라인으로 편리하게 신고
                  </p>
                </div>
              </div>

              <div class="flex items-start gap-4">
                <div class="w-12 h-12 bg-green-500/20 rounded-lg flex items-center justify-center flex-shrink-0 border border-green-500/30">
                  <i data-lucide="building-2" class="w-6 h-6 text-green-400"></i>
                </div>
                <div class="flex-1">
                  <h3 class="text-lg font-semibold mb-2">방문 신고</h3>
                  <p class="text-sm text-slate-400">
                    가까운 경찰서 또는 사이버수사대<br>
                    직접 방문 신고가 가능합니다.
                  </p>
                </div>
              </div>
            </div>

            <div class="mt-6 p-4 bg-yellow-500/10 border border-yellow-500/30 rounded-lg">
              <p class="text-sm text-yellow-400"><strong>긴급한 경우:</strong> 112로 신고하세요.</p>
            </div>
          </div>
          </div>

          <div class="relative group h-full">
            <div class="absolute inset-0 bg-gradient-to-r from-cyan-600 to-blue-600 rounded-3xl blur-2xl opacity-0 group-hover:opacity-20 transition-opacity"></div>
            <div class="report-panel relative h-full bg-slate-900/50 backdrop-blur-xl rounded-2xl p-8 border border-white/10 hover:border-white/20 transition-all flex flex-col">
            <div class="flex items-center gap-3 mb-6">
              <i data-lucide="eye" class="w-8 h-8 text-cyan-400"></i>
              <h2 class="text-2xl font-bold">딥페이크 예방 가이드</h2>
            </div>

            <div class="space-y-6 flex-1">
              <div class="relative group/card">
                <div class="absolute inset-0 bg-gradient-to-r from-cyan-500 to-sky-500 rounded-2xl blur-xl opacity-0 group-hover/card:opacity-30 transition-opacity"></div>
                <div class="report-guide-card relative p-4 bg-cyan-500/10 border border-cyan-500/30 hover:border-cyan-300/50 rounded-lg transition-all">
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-10 h-10 bg-cyan-600 rounded-full flex items-center justify-center flex-shrink-0">
                    <i data-lucide="check-circle" class="w-5 h-5 text-white"></i>
                  </div>
                  <h3 class="text-lg font-semibold">진위 확인 방법</h3>
                </div>
                <ul class="space-y-2 text-sm text-slate-400">
                  <li class="flex items-start gap-2"><span class="text-cyan-400 mt-1">&#8226;</span><span>얼굴 경계선, 그림자, 빛 반사의 자연스러움을 확인</span></li>
                  <li class="flex items-start gap-2"><span class="text-cyan-400 mt-1">&#8226;</span><span>눈 깜빡임, 입술 움직임의 부자연스러움 체크</span></li>
                  <li class="flex items-start gap-2"><span class="text-cyan-400 mt-1">&#8226;</span><span>배경과 인물의 화질이 다른지 확인</span></li>
                </ul>
              </div>
              </div>

              <div class="relative group/card">
                <div class="absolute inset-0 bg-gradient-to-r from-blue-500 to-indigo-500 rounded-2xl blur-xl opacity-0 group-hover/card:opacity-30 transition-opacity"></div>
                <div class="report-guide-card relative p-4 bg-blue-500/10 border border-blue-500/30 hover:border-blue-300/50 rounded-lg transition-all">
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center flex-shrink-0">
                    <i data-lucide="lock" class="w-5 h-5 text-white"></i>
                  </div>
                  <h3 class="text-lg font-semibold">개인정보 보호</h3>
                </div>
                <ul class="space-y-2 text-sm text-slate-400">
                  <li class="flex items-start gap-2"><span class="text-blue-400 mt-1">&#8226;</span><span>SNS에 얼굴 사진을 무분별하게 게시하지 않기</span></li>
                  <li class="flex items-start gap-2"><span class="text-blue-400 mt-1">&#8226;</span><span>프로필 사진 공개 범위 제한 설정</span></li>
                  <li class="flex items-start gap-2"><span class="text-blue-400 mt-1">&#8226;</span><span>워터마크나 서명 추가 고려</span></li>
                </ul>
              </div>
              </div>

              <div class="relative group/card">
                <div class="absolute inset-0 bg-gradient-to-r from-green-500 to-emerald-500 rounded-2xl blur-xl opacity-0 group-hover/card:opacity-30 transition-opacity"></div>
                <div class="report-guide-card relative p-4 bg-green-500/10 border border-green-500/30 hover:border-green-300/50 rounded-lg transition-all">
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-10 h-10 bg-green-600 rounded-full flex items-center justify-center flex-shrink-0">
                    <i data-lucide="users" class="w-5 h-5 text-white"></i>
                  </div>
                  <h3 class="text-lg font-semibold">공유 전 확인</h3>
                </div>
                <ul class="space-y-2 text-sm text-slate-400">
                  <li class="flex items-start gap-2"><span class="text-green-400 mt-1">&#8226;</span><span>출처가 불분명한 이미지/영상 공유 자제</span></li>
                  <li class="flex items-start gap-2"><span class="text-green-400 mt-1">&#8226;</span><span>팩트체크 사이트를 통한 진위 확인</span></li>
                  <li class="flex items-start gap-2"><span class="text-green-400 mt-1">&#8226;</span><span>의심스러운 콘텐츠는 즉시 신고</span></li>
                </ul>
              </div>
              </div>

              <div class="relative group/card">
                <div class="absolute inset-0 bg-gradient-to-r from-orange-500 to-red-500 rounded-2xl blur-xl opacity-0 group-hover/card:opacity-30 transition-opacity"></div>
                <div class="report-guide-card relative p-4 bg-orange-500/10 border border-orange-500/30 hover:border-orange-300/50 rounded-lg transition-all">
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-10 h-10 bg-orange-600 rounded-full flex items-center justify-center flex-shrink-0">
                    <i data-lucide="alert-circle" class="w-5 h-5 text-white"></i>
                  </div>
                  <h3 class="text-lg font-semibold">피해 발생 시</h3>
                </div>
                <ul class="space-y-2 text-sm text-slate-400">
                  <li class="flex items-start gap-2"><span class="text-orange-400 mt-1">&#8226;</span><span>증거자료 확보 (스크린샷, URL 저장)</span></li>
                  <li class="flex items-start gap-2"><span class="text-orange-400 mt-1">&#8226;</span><span>즉시 경찰서(182) 또는 온라인 신고</span></li>
                  <li class="flex items-start gap-2"><span class="text-orange-400 mt-1">&#8226;</span><span>플랫폼에 콘텐츠 삭제 요청</span></li>
                </ul>
              </div>
              </div>
            </div>

            <div class="mt-6 p-4 bg-gradient-to-r from-sky-500/10 to-cyan-500/10 border border-sky-500/30 rounded-lg">
              <p class="text-sm text-slate-300 text-center">
                <strong class="text-sky-400">TIP:</strong> DeepScan을 사용하여 의심스러운 이미지를 사전에 검증해보세요.
              </p>
            </div>
          </div>
          </div>
        </div>

        <div class="relative group mb-12">
          <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-0 group-hover:opacity-20 transition-opacity"></div>
          <div class="report-panel relative bg-slate-900/50 backdrop-blur-xl rounded-2xl p-8 border border-white/10 hover:border-white/20 transition-all">
          <div class="flex items-center gap-3 mb-6">
            <i data-lucide="trending-up" class="w-8 h-8 text-sky-400"></i>
            <h2 class="text-2xl font-bold">딥페이크 범죄 통계</h2>
          </div>

          <div class="grid md:grid-cols-3 gap-6 mb-8">
            <div class="crime-stat-card crime-stat-card-red bg-red-500/10 p-6 rounded-xl border border-red-500/30">
              <div class="text-3xl text-red-400 mb-2 font-bold">+273%</div>
              <p class="text-slate-400">2023년 대비<br>딥페이크 범죄 증가율</p>
            </div>
            <div class="crime-stat-card crime-stat-card-blue bg-blue-500/10 p-6 rounded-xl border border-blue-500/30">
              <div class="text-3xl text-blue-400 mb-2 font-bold">89%</div>
              <p class="text-slate-400">성범죄 관련<br>딥페이크 비율</p>
            </div>
            <div class="crime-stat-card crime-stat-card-purple bg-purple-500/10 p-6 rounded-xl border border-purple-500/30">
              <div class="text-3xl text-purple-400 mb-2 font-bold">10대</div>
              <p class="text-slate-400">가장 많은<br>피해 연령대</p>
            </div>
          </div>

          <div class="space-y-4">
            <div class="crime-info-card p-5 bg-white/5 rounded-lg border border-white/10">
              <div class="flex items-start gap-4">
                <i data-lucide="info" class="w-6 h-6 text-sky-400 flex-shrink-0 mt-1"></i>
                <div>
                  <h4 class="text-lg font-semibold mb-2">주요 범죄 유형</h4>
                  <div class="grid md:grid-cols-2 gap-3 text-sm text-slate-400">
                    <div class="flex items-center gap-2"><span class="w-2 h-2 bg-red-500 rounded-full"></span><span>성적 합성물 제작 및 유포 (67%)</span></div>
                    <div class="flex items-center gap-2"><span class="w-2 h-2 bg-orange-500 rounded-full"></span><span>명의도용 및 사기 (18%)</span></div>
                    <div class="flex items-center gap-2"><span class="w-2 h-2 bg-yellow-500 rounded-full"></span><span>허위정보 유포 (10%)</span></div>
                    <div class="flex items-center gap-2"><span class="w-2 h-2 bg-blue-500 rounded-full"></span><span>기타 사이버 범죄 (5%)</span></div>
                  </div>
                </div>
              </div>
            </div>

            <div class="crime-warning-card p-5 bg-yellow-500/10 rounded-lg border border-yellow-500/30">
              <div class="flex items-start gap-4">
                <i data-lucide="alert-triangle" class="w-6 h-6 text-yellow-400 flex-shrink-0 mt-1"></i>
                <div>
                  <h4 class="text-lg font-semibold mb-2">경고: 처벌 강화</h4>
                  <p class="text-sm text-slate-400 leading-relaxed">
                    2024년부터 딥페이크 성범죄에 대한 처벌 규정이 강화되었습니다.
                    <strong class="text-yellow-400">제작·유포·소지 모두 처벌 대상이며, 최대 징역 7년 또는 5천만원 이하의 벌금</strong>이 적용될 수 있습니다.
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div class="mt-6 flex justify-center">
            <button type="button" onclick="goPage('<%= contextPath %>/news')" class="group px-6 py-3 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white rounded-lg transition-all shadow-lg shadow-sky-500/50 font-semibold inline-flex items-center gap-2">
              관련 뉴스 보러가기
              <i data-lucide="arrow-right" class="w-4 h-4 group-hover:translate-x-1 transition-transform"></i>
            </button>
          </div>
        </div>
        </div>

        <div class="relative group mb-12">
          <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-0 group-hover:opacity-20 transition-opacity"></div>
          <div class="report-panel relative bg-slate-900/50 backdrop-blur-xl rounded-2xl p-8 border border-white/10 hover:border-white/20 transition-all">
          <h3 class="text-2xl font-bold mb-3">가까운 경찰서 찾기</h3>
          <p class="text-slate-400 mb-6">직접 방문 신고가 필요하면 가까운 경찰서를 확인하고, 지도와 목록에서 바로 전화 정보까지 보세요.</p>
          <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between mb-6">
            <div class="station-search-row">
              <input
                id="stationSearchInput"
                type="text"
                class="station-search-input"
                placeholder="경찰서 이름 또는 구 이름 검색"
                onkeydown="handleStationSearchKeydown(event)"
              >
              <button type="button" onclick="searchPoliceStation()" class="station-action-button">
                <i data-lucide="search"></i>
                검색
              </button>
            </div>
            <div class="flex flex-col sm:flex-row gap-3">
              <button type="button" onclick="showNearbyPoliceStations()" class="station-action-button">
                <i data-lucide="navigation"></i>
                내 위치 기준 찾기
              </button>
              <button type="button" onclick="resetPoliceStations()" class="station-outline-button">
                <i data-lucide="refresh-cw"></i>
                목록 초기화
              </button>
            </div>
          </div>
          <div class="station-locator-layout">
            <div class="station-panel p-5">
              <div id="policeStationMap" class="station-map">
                <div class="station-map-empty">
                  <i data-lucide="map" class="w-8 h-8"></i>
                  <span>지도를 불러오는 중입니다...</span>
                </div>
              </div>
              <div class="station-info-card">
                <div class="flex items-start justify-between gap-4">
                  <div>
                    <p class="text-xs uppercase tracking-[0.24em] text-sky-300/80 mb-2">선택 경찰서</p>
                    <h4 id="selectedStationName" class="text-xl font-bold text-white mb-2">경찰서를 선택해 주세요</h4>
                    <p id="selectedStationAddress" class="text-sm text-slate-400">오른쪽 목록에서 경찰서를 선택하면 지도 위치와 연락처를 함께 확인할 수 있습니다.</p>
                  </div>
                  <a id="selectedStationPhone" href="#" class="hidden shrink-0 inline-flex items-center gap-2 rounded-full border border-sky-400/30 bg-sky-500/10 px-4 py-2 text-sm font-semibold text-sky-300 hover:bg-sky-500/20">
                    <i data-lucide="phone" class="w-4 h-4"></i>
                    <span></span>
                  </a>
                </div>
                <p id="stationStatus" class="mt-4 text-sm text-slate-400">지도를 불러오는 중입니다...</p>
              </div>
            </div>
            <div class="station-panel p-5">
              <h4 id="stationListTitle" class="text-xl font-bold text-white mb-5 flex items-center gap-2">
                <i data-lucide="map-pin" class="w-5 h-5 text-sky-400"></i>
                <span>서울 주요 경찰서</span>
              </h4>
              <div id="stationList" class="station-list-panel space-y-3"></div>
            </div>
          </div>
        </div>
        </div>

        <div class="relative group">
          <div class="absolute inset-0 bg-gradient-to-r from-cyan-600 to-sky-600 rounded-3xl blur-2xl opacity-0 group-hover:opacity-20 transition-opacity"></div>
          <div class="report-panel relative bg-slate-900/50 backdrop-blur-xl rounded-2xl p-8 border border-white/10 hover:border-white/20 transition-all">
          <h3 class="text-xl font-bold mb-6">신고 시 유의사항</h3>
          <div class="grid md:grid-cols-2 gap-6">
            <div>
              <h4 class="text-cyan-400 mb-3 font-semibold">신고 전 준비사항</h4>
              <ul class="space-y-2 text-sm text-slate-400">
                <li class="flex items-start gap-2"><span class="text-cyan-400 mt-1">&#8226;</span><span>피해 사실을 입증할 수 있는 증거자료 (스크린샷, URL 등)</span></li>
                <li class="flex items-start gap-2"><span class="text-cyan-400 mt-1">&#8226;</span><span>발생 일시와 경위에 대한 상세한 기록</span></li>
                <li class="flex items-start gap-2"><span class="text-cyan-400 mt-1">&#8226;</span><span>가해자 정보 (ID, 연락처 등)</span></li>
                <li class="flex items-start gap-2"><span class="text-cyan-400 mt-1">&#8226;</span><span>피해 내용 및 피해 규모</span></li>
              </ul>
            </div>

            <div>
              <h4 class="text-sky-400 mb-3 font-semibold">처리 절차</h4>
              <ul class="space-y-2 text-sm text-slate-400">
                <li class="flex items-start gap-2"><span class="text-sky-400 mt-1">1.</span><span>신고 접수 및 사건 번호 부여</span></li>
                <li class="flex items-start gap-2"><span class="text-sky-400 mt-1">2.</span><span>담당 수사관 배정 및 사실 확인</span></li>
                <li class="flex items-start gap-2"><span class="text-sky-400 mt-1">3.</span><span>증거 수집 및 피해자 진술 청취</span></li>
                <li class="flex items-start gap-2"><span class="text-sky-400 mt-1">4.</span><span>수사 결과에 따른 사건 처리</span></li>
              </ul>
            </div>
          </div>
        </div>
        </div>
      </div>
    </div>
  </div>

  <script>
    const POLICE_STATIONS = [
      { id: 1, name: "서울청 사이버안전과", address: "서울특별시 서대문구 통일로 97", phone: "02-700-4400", district: "서대문구", lat: 37.5707, lng: 126.9666 },
      { id: 2, name: "종로경찰서", address: "서울특별시 종로구 율곡로 46", phone: "02-2148-0112", district: "종로구", lat: 37.5761, lng: 126.9853 },
      { id: 3, name: "남대문경찰서", address: "서울특별시 중구 세종대로 135", phone: "02-778-0112", district: "중구", lat: 37.5656, lng: 126.9769 },
      { id: 4, name: "강남경찰서", address: "서울특별시 강남구 테헤란로 113길 23", phone: "02-3497-0112", district: "강남구", lat: 37.5046, lng: 127.0474 },
      { id: 5, name: "서초경찰서", address: "서울특별시 서초구 반포대로 179", phone: "02-3477-0112", district: "서초구", lat: 37.4954, lng: 127.0087 },
      { id: 6, name: "송파경찰서", address: "서울특별시 송파구 중대로 213", phone: "02-2147-0112", district: "송파구", lat: 37.5078, lng: 127.1003 },
      { id: 7, name: "강동경찰서", address: "서울특별시 강동구 천호대로 1073", phone: "02-2204-0112", district: "강동구", lat: 37.5384, lng: 127.1256 },
      { id: 8, name: "영등포경찰서", address: "서울특별시 영등포구 국회대로 22길 2", phone: "02-2670-0112", district: "영등포구", lat: 37.5265, lng: 126.9007 },
      { id: 9, name: "마포경찰서", address: "서울특별시 마포구 마포대로 183", phone: "02-700-0112", district: "마포구", lat: 37.5509, lng: 126.9541 },
      { id: 10, name: "용산경찰서", address: "서울특별시 용산구 원효로 395", phone: "02-2080-0112", district: "용산구", lat: 37.5371, lng: 126.9676 }
    ];
    const DEFAULT_CENTER = { lat: 37.5665, lng: 126.9780 };
    let kakaoMap = null;
    let infoOverlay = null;
    let mapMarkers = [];
    let currentLocationMarker = null;
    let selectedStationId = null;
    let sortByNearby = false;
    let currentLocation = null;

    function goPage(path) {
      window.location.href = path;
    }

    function setStationStatus(message) {
      const status = document.getElementById("stationStatus");
      if (status) {
        status.textContent = message;
      }
    }

    function getStationById(id) {
      return POLICE_STATIONS.find(function (station) {
        return station.id === id;
      }) || null;
    }

    function buildOverlayContent(station) {
      return ""
        + '<div class="custom-overlay">'
        +   '<div style="font-weight:700; margin-bottom:4px;">' + station.name + "</div>"
        +   '<div style="color:#cbd5e1;">' + station.address + "</div>"
        +   '<div style="color:#7dd3fc; margin-top:6px;">' + station.phone + "</div>"
        + "</div>";
    }

    function toRadians(value) {
      return value * Math.PI / 180;
    }

    function calculateDistanceKm(lat1, lng1, lat2, lng2) {
      const earthRadius = 6371;
      const dLat = toRadians(lat2 - lat1);
      const dLng = toRadians(lng2 - lng1);
      const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
        Math.sin(dLng / 2) * Math.sin(dLng / 2);
      const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      return earthRadius * c;
    }

    function getVisibleStations() {
      const stations = POLICE_STATIONS.map(function (station) {
        const result = Object.assign({}, station);
        if (currentLocation) {
          result.distance = calculateDistanceKm(currentLocation.lat, currentLocation.lng, station.lat, station.lng);
        }
        return result;
      });

      if (sortByNearby && currentLocation) {
        stations.sort(function (a, b) {
          return a.distance - b.distance;
        });
      }

      return stations;
    }

    function updateSelectedStationCard(station) {
      const name = document.getElementById("selectedStationName");
      const address = document.getElementById("selectedStationAddress");
      const phoneLink = document.getElementById("selectedStationPhone");

      if (!name || !address || !phoneLink) {
        return;
      }

      if (!station) {
        name.textContent = "경찰서를 선택해 주세요";
        address.textContent = "오른쪽 목록에서 경찰서를 선택하면 지도 위치와 연락처를 한 번에 확인할 수 있습니다.";
        phoneLink.classList.add("hidden");
        phoneLink.setAttribute("href", "#");
        phoneLink.querySelector("span").textContent = "";
        return;
      }

      name.textContent = station.name;
      address.textContent = station.address;
      phoneLink.classList.remove("hidden");
      phoneLink.setAttribute("href", "tel:" + station.phone);
      phoneLink.querySelector("span").textContent = station.phone;
    }

    function getStationSearchQuery() {
      const input = document.getElementById("stationSearchInput");
      return input ? input.value.trim().toLowerCase() : "";
    }

    function handleStationSearchKeydown(event) {
      if (event.key === "Enter") {
        event.preventDefault();
        searchPoliceStation();
      }
    }

    function searchPoliceStation() {
      const query = getStationSearchQuery();

      if (!query) {
        setStationStatus("경찰서명, 구 이름, 주소 일부를 입력해 주세요.");
        return;
      }

      const station = POLICE_STATIONS.find(function (item) {
        return item.name.toLowerCase().includes(query)
          || item.district.toLowerCase().includes(query)
          || item.address.toLowerCase().includes(query);
      });

      if (!station) {
        setStationStatus("입력한 검색어와 일치하는 경찰서를 찾지 못했습니다.");
        return;
      }

      sortByNearby = false;
      selectPoliceStation(station.id, true);
      setStationStatus(station.name + " 위치로 지도를 이동했습니다.");
    }

    function renderStationList() {
      const stations = getVisibleStations();
      const list = document.getElementById("stationList");
      const title = document.getElementById("stationListTitle");

      if (!list || !title) {
        return;
      }

      title.querySelector("span").textContent = sortByNearby && currentLocation
        ? "내 주변 가까운 경찰서"
        : "서울 주요 경찰서";

      list.innerHTML = stations.map(function (station, index) {
        const isSelected = station.id === selectedStationId;
        const isTop = index < 3;
        const distanceHtml = typeof station.distance === "number"
          ? '<span class="text-sm text-sky-300 font-medium">' + station.distance.toFixed(1) + 'km</span>'
          : '';

        return ''
          + '<button type="button" class="station-item' + (isSelected ? ' is-selected' : '') + '" onclick="selectPoliceStation(' + station.id + ')">'
          +   '<div class="flex items-start gap-4">'
          +     '<div class="station-rank' + (isTop ? ' is-top' : '') + '">' + (index + 1) + '</div>'
          +     '<div class="min-w-0 flex-1">'
          +       '<div class="text-lg font-semibold text-white mb-2">' + station.name + '</div>'
          +       '<div class="text-sm text-slate-400 mb-3">' + station.address + '</div>'
          +       '<div class="flex flex-wrap items-center gap-4">'
          +         '<a href="tel:' + station.phone + '" onclick="event.stopPropagation()" class="inline-flex items-center gap-1 text-sky-300 hover:text-sky-200 text-sm">'
          +           '<i data-lucide="phone" class="w-4 h-4"></i>' + station.phone
          +         '</a>'
          +         distanceHtml
          +       '</div>'
          +     '</div>'
          +   '</div>'
          + '</button>';
      }).join("");

      lucide.createIcons();
    }

    function clearMarkers() {
      mapMarkers.forEach(function (entry) {
        entry.marker.setMap(null);
      });
      mapMarkers = [];
    }

    function renderMapMarkers() {
      if (!kakaoMap || typeof kakao === "undefined" || !kakao.maps) {
        return;
      }

      clearMarkers();

      POLICE_STATIONS.forEach(function (station) {
        const position = new kakao.maps.LatLng(station.lat, station.lng);
        const marker = new kakao.maps.Marker({
          map: kakaoMap,
          position: position
        });

        kakao.maps.event.addListener(marker, "click", function () {
          selectPoliceStation(station.id, true);
        });

        mapMarkers.push({ id: station.id, marker: marker });
      });
    }

    function fitMapToStations() {
      if (!kakaoMap || typeof kakao === "undefined" || !kakao.maps) {
        return;
      }

      const bounds = new kakao.maps.LatLngBounds();
      POLICE_STATIONS.forEach(function (station) {
        bounds.extend(new kakao.maps.LatLng(station.lat, station.lng));
      });
      if (currentLocation) {
        bounds.extend(new kakao.maps.LatLng(currentLocation.lat, currentLocation.lng));
      }
      kakaoMap.setBounds(bounds);
    }

    function updateCurrentLocationMarker() {
      if (!kakaoMap || typeof kakao === "undefined" || !kakao.maps) {
        return;
      }

      if (!currentLocation) {
        if (currentLocationMarker) {
          currentLocationMarker.setMap(null);
          currentLocationMarker = null;
        }
        return;
      }

      const position = new kakao.maps.LatLng(currentLocation.lat, currentLocation.lng);
      if (!currentLocationMarker) {
        currentLocationMarker = new kakao.maps.Marker({
          map: kakaoMap,
          position: position
        });
      } else {
        currentLocationMarker.setPosition(position);
        currentLocationMarker.setMap(kakaoMap);
      }
    }

    function selectPoliceStation(id, centerMap) {
      selectedStationId = id;
      const station = getStationById(id);
      updateSelectedStationCard(station);

      if (station && kakaoMap && infoOverlay) {
        const position = new kakao.maps.LatLng(station.lat, station.lng);
        infoOverlay.setContent(buildOverlayContent(station));
        infoOverlay.setPosition(position);
        infoOverlay.setMap(kakaoMap);

        if (centerMap !== false) {
          kakaoMap.setCenter(position);
        }
      }

      if (station) {
        setStationStatus(station.name + " 정보를 표시하고 있습니다.");
      }

      renderStationList();
    }

    function initializePoliceMap() {
      const container = document.getElementById("policeStationMap");
      if (!container || typeof kakao === "undefined" || !kakao.maps) {
        setStationStatus("카카오맵을 불러오지 못했습니다.");
        return;
      }

      kakaoMap = new kakao.maps.Map(container, {
        center: new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng),
        level: 8
      });

      infoOverlay = new kakao.maps.CustomOverlay({
        yAnchor: 1.55
      });

      renderMapMarkers();
      updateCurrentLocationMarker();
      fitMapToStations();

      if (!selectedStationId) {
        selectedStationId = POLICE_STATIONS[0].id;
      }

      selectPoliceStation(selectedStationId, false);
      setStationStatus("서울 주요 경찰서 지도를 불러왔습니다.");
    }

    function bootKakaoMap() {
      if (window.kakao && window.kakao.maps && typeof window.kakao.maps.load === "function") {
        window.kakao.maps.load(function () {
          initializePoliceMap();
        });
      } else {
        setStationStatus("카카오맵 SDK 로드 중입니다...");
      }
    }

    function resetPoliceStations() {
      sortByNearby = false;
      currentLocation = null;
      const input = document.getElementById("stationSearchInput");
      if (input) {
        input.value = "";
      }
      updateCurrentLocationMarker();
      fitMapToStations();
      if (!selectedStationId) {
        selectedStationId = POLICE_STATIONS[0].id;
      }
      selectPoliceStation(selectedStationId, false);
      renderStationList();
    }

    function showNearbyPoliceStations() {
      if (!navigator.geolocation) {
        setStationStatus("현재 위치 기능을 사용할 수 없는 환경입니다.");
        return;
      }

      setStationStatus("현재 위치를 확인하는 중입니다...");

      navigator.geolocation.getCurrentPosition(function (position) {
        currentLocation = {
          lat: position.coords.latitude,
          lng: position.coords.longitude
        };
        sortByNearby = true;
        updateCurrentLocationMarker();
        fitMapToStations();

        const nearestStation = getVisibleStations()[0];
        if (nearestStation) {
          selectedStationId = nearestStation.id;
          selectPoliceStation(selectedStationId, true);
        setStationStatus("현재 위치 기준으로 " + nearestStation.name + " 가 가장 가까운 경찰서입니다.");
        }
        renderStationList();
      }, function () {
        setStationStatus("위치 권한이 거부되었거나 현재 위치를 가져오지 못했습니다.");
      });
    }

    function initializePoliceStations() {
      renderStationList();
      updateSelectedStationCard(null);
      setStationStatus("카카오맵을 준비하는 중입니다...");
      bootKakaoMap();
    }

    function submitMockReport() {
      document.getElementById("mainView").classList.add("hidden");
      document.getElementById("submittedView").classList.remove("hidden");
      lucide.createIcons();
      setTimeout(function () {
        goPage("<%= contextPath %>/");
      }, 3000);
    }

    function normalizeReportCopy() {
      document.title = "신고하기 - DeepScan";

      const submittedTitle = document.querySelector("#submittedView h2");
      if (submittedTitle) {
        submittedTitle.textContent = "신고가 접수되었습니다";
      }

      const submittedTexts = document.querySelectorAll("#submittedView p");
      if (submittedTexts[0]) {
        submittedTexts[0].textContent = "빠른 시일 내에 검토하겠습니다.";
      }
      if (submittedTexts[1]) {
        submittedTexts[1].textContent = "잠시 후 홈으로 이동합니다.";
      }

      const heroTitle = document.querySelector("#mainView .text-center h1");
      if (heroTitle) {
        heroTitle.textContent = "딥페이크 신고";
      }

      const heroDesc = document.querySelector("#mainView .text-center p");
      if (heroDesc) {
        heroDesc.innerHTML = "딥페이크, 허위정보, 사이버 범죄를 발견하셨나요?<br>즉시 사이버수사기관에 신고하여 안전한 온라인 환경을 만들어 주세요.";
      }
    }

    lucide.createIcons();
    document.addEventListener("DOMContentLoaded", function () {
      normalizeReportCopy();
      initializePoliceStations();
    });

    (function () {
      const sdk = document.getElementById("kakaoMapSdk");
      if (!sdk) {
        return;
      }

      sdk.addEventListener("load", function () {
        if (!kakaoMap) {
          bootKakaoMap();
        }
      });

      sdk.addEventListener("error", function () {
        setStationStatus("카카오맵 SDK 로드에 실패했습니다. 앱 키 또는 도메인 설정을 확인해 주세요.");
      });
    })();
  </script>
</body>
</html>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  // JSP가 현재 애플리케이션의 context path를 구해 JS와 링크에서 공통으로 사용한다.
  // 배포 경로가 바뀌어도 API 호출 주소가 깨지지 않게 하기 위한 값이다.
  String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>회원가입 - DeepScan</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <script src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
  <style>
    body {
      font-family: "Pretendard", sans-serif;
      background: #020617;
    }
  </style>
</head>
<body class="min-h-screen bg-slate-950 text-white relative overflow-x-hidden">
  <!-- 회원가입 화면의 배경 효과 영역이다. 실제 회원가입 로직과는 분리되어 있다. -->
  <div class="absolute inset-0 pointer-events-none">
    <div class="absolute top-20 left-1/4 w-96 h-96 bg-sky-600/30 rounded-full blur-3xl"></div>
    <div class="absolute bottom-20 right-1/4 w-96 h-96 bg-cyan-600/30 rounded-full blur-3xl"></div>
  </div>

  <!-- 로그인 화면으로 돌아가는 버튼이다. contextPath를 붙여 배포 경로 차이를 흡수한다. -->
  <nav class="fixed top-0 left-0 right-0 z-50 h-16 border-b border-white/10 bg-slate-950/80 backdrop-blur-xl">
    <div class="mx-auto flex h-full max-w-7xl items-center px-4 sm:px-6 lg:px-8">
      <button type="button" onclick="goPage('<%= contextPath %>/login')" class="inline-flex h-10 items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 text-sm font-semibold text-white shadow-lg shadow-black/20 transition-all hover:bg-white/15">
        <i data-lucide="arrow-left" class="w-4 h-4"></i>
        로그인으로
      </button>
    </div>
  </nav>

  <main class="w-full max-w-2xl mx-auto relative z-10 pt-24 pb-12 px-4">
    <section class="text-center mb-8">
      <div class="relative inline-flex h-20 w-20 items-center justify-center mb-6">
        <div class="absolute inset-2 bg-cyan-500/25 blur-2xl"></div>
        <img src="<%= contextPath %>/resources/image/deepscan-mark.svg?v=30" alt="DeepScan" class="relative h-16 w-16" width="64" height="64">
      </div>
      <h1 class="text-4xl font-bold mb-3">
        <span class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-transparent">회원가입</span>
      </h1>
      <p class="text-slate-400">DeepScan과 함께 안전한 검증을 시작해 보세요.</p>
    </section>

    <section class="relative group">
      <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-20 group-hover:opacity-30 transition-opacity"></div>
      <div class="relative bg-slate-900/50 backdrop-blur-xl rounded-3xl p-8 border border-white/10">
        <!-- id="signupForm"은 아래 JS에서 최종 회원가입 submit 이벤트를 연결하는 기준이다. -->
        <form id="signupForm" class="space-y-6" autocomplete="off">
          <div>
            <label for="name" class="block text-sm text-slate-300 mb-2 font-medium">이름</label>
            <div class="relative">
              <i data-lucide="user" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
              <input id="name" type="text" placeholder="이름을 입력해 주세요." required class="w-full pl-12 pr-4 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 transition-all backdrop-blur-xl">
            </div>
          </div>

          <div>
            <label for="email" class="block text-sm text-slate-300 mb-2 font-medium">이메일</label>
            <div class="grid grid-cols-1 sm:grid-cols-[1fr_auto_auto] gap-2 mb-2">
              <div class="relative">
                <i data-lucide="mail" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input id="email" type="email" placeholder="your@email.com" required autocomplete="off" autocapitalize="off" spellcheck="false" class="w-full pl-12 pr-12 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 disabled:opacity-50 transition-all backdrop-blur-xl">
                <i id="emailVerifiedIcon" data-lucide="check-circle" class="hidden absolute right-4 top-1/2 -translate-y-1/2 w-5 h-5 text-green-400"></i>
              </div>
              <button id="checkEmailButton" type="button" onclick="handleCheckEmail()" class="px-5 py-4 bg-white/10 hover:bg-white/20 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl transition-all font-medium whitespace-nowrap">
                중복확인
              </button>
              <button id="sendCodeButton" type="button" onclick="handleSendVerificationCode()" disabled class="px-6 py-4 bg-white/10 hover:bg-white/20 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl transition-all font-medium whitespace-nowrap">
                인증번호 받기
              </button>
            </div>

            <div id="verifyCodeWrap" class="hidden mt-3 grid grid-cols-1 sm:grid-cols-[1fr_auto] gap-2">
              <div class="relative">
                <input id="verifyCode" type="text" placeholder="인증번호 6자리" maxlength="6" inputmode="numeric" class="w-full px-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
                <div id="timerWrap" class="hidden absolute right-4 top-1/2 -translate-y-1/2 flex items-center gap-1 text-sky-400 text-sm">
                  <i data-lucide="clock" class="w-4 h-4"></i>
                  <span id="timerText"></span>
                </div>
              </div>
              <button type="button" onclick="handleVerifyCode()" class="px-6 py-3 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 rounded-xl transition-all font-medium">
                확인
              </button>
            </div>
          </div>

          <div>
            <label for="password" class="block text-sm text-slate-300 mb-2 font-medium">비밀번호</label>
            <div class="relative">
              <i data-lucide="lock" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
              <input id="password" type="password" placeholder="비밀번호를 입력해 주세요." required class="w-full pl-12 pr-4 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 transition-all backdrop-blur-xl">
            </div>
          </div>

          <div>
            <label for="confirmPassword" class="block text-sm text-slate-300 mb-2 font-medium">비밀번호 확인</label>
            <div class="relative">
              <i data-lucide="lock" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
              <input id="confirmPassword" type="password" placeholder="비밀번호를 다시 입력해 주세요." required class="w-full pl-12 pr-4 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 transition-all backdrop-blur-xl">
            </div>
          </div>

          <div>
            <label for="phone" class="block text-sm text-slate-300 mb-2 font-medium">전화번호</label>
            <div class="relative">
              <i data-lucide="phone" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
              <input id="phone" type="tel" placeholder="010-1234-5678" required maxlength="13" class="w-full pl-12 pr-4 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 transition-all backdrop-blur-xl">
            </div>
          </div>

          <div>
            <label for="address" class="block text-sm text-slate-300 mb-2 font-medium">주소</label>
            <div class="grid grid-cols-1 sm:grid-cols-[1fr_auto] gap-2 mb-3">
              <div class="relative">
                <i data-lucide="map-pin" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input id="address" type="text" placeholder="주소 검색" readonly required class="w-full pl-12 pr-4 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 cursor-pointer backdrop-blur-xl" onclick="openAddressModal()">
              </div>
              <button type="button" onclick="openAddressModal()" class="px-6 py-4 bg-white/10 hover:bg-white/20 rounded-xl transition-all font-medium whitespace-nowrap flex items-center justify-center gap-2">
                <i data-lucide="search" class="w-4 h-4"></i>
                검색
              </button>
            </div>
            <input id="detailAddress" type="text" placeholder="상세 주소" class="hidden w-full px-4 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 transition-all backdrop-blur-xl">
          </div>

          <div id="errorBox" class="hidden p-4 bg-red-500/10 border border-red-500/50 rounded-xl flex items-start gap-3">
            <i data-lucide="alert-circle" class="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5"></i>
            <p id="errorText" class="text-red-400 text-sm"></p>
          </div>

          <div id="successBox" class="hidden p-4 bg-green-500/10 border border-green-500/50 rounded-xl flex items-start gap-3">
            <i data-lucide="check-circle-2" class="w-5 h-5 text-green-400 flex-shrink-0 mt-0.5"></i>
            <p id="successText" class="text-green-300 text-sm"></p>
          </div>

          <button id="submitBtn" type="submit" class="w-full py-4 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 disabled:from-slate-700 disabled:to-slate-700 disabled:cursor-not-allowed text-white rounded-xl transition-all shadow-2xl shadow-sky-500/50 font-semibold text-lg">
            회원가입
          </button>
        </form>
      </div>
    </section>
  </main>

  <!-- 다음 주소검색 위젯을 띄우는 모달이다. 주소 선택 결과는 address/detailAddress/zonecode에 반영된다. -->
  <div id="addressModal" class="hidden fixed inset-0 z-50 items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" onclick="closeAddressModal()"></div>
    <div class="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl overflow-hidden">
      <div class="flex items-center justify-between p-4 border-b border-slate-200">
        <h2 class="text-lg text-slate-900 font-semibold">주소 검색</h2>
        <button type="button" onclick="closeAddressModal()" class="p-2 hover:bg-slate-100 rounded-lg transition-colors">
          <i data-lucide="x" class="w-5 h-5 text-slate-600"></i>
        </button>
      </div>
      <div id="postcodeWrap" class="h-[500px] max-h-[70vh] overflow-auto bg-white"></div>
    </div>
  </div>

  <!-- 인증번호 발송 성공 후 사용자에게 메일 확인을 안내하는 모달이다. -->
  <div id="verificationCodeModal" class="hidden fixed inset-0 z-50">
    <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" onclick="closeVerificationCodeModal()"></div>
    <div class="fixed inset-0 flex items-center justify-center p-4">
      <div class="w-full max-w-md relative">
        <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-30"></div>
        <div class="relative bg-slate-900/95 backdrop-blur-xl rounded-3xl p-8 border border-white/10">
          <button type="button" onclick="closeVerificationCodeModal()" class="absolute top-6 right-6 w-10 h-10 flex items-center justify-center bg-white/5 hover:bg-white/10 rounded-full transition-all">
            <i data-lucide="x" class="w-5 h-5 text-slate-400"></i>
          </button>
          <div class="text-center mb-6">
            <div class="relative inline-block">
              <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-2xl blur-xl opacity-75"></div>
              <div class="relative w-16 h-16 bg-gradient-to-br from-sky-500 to-cyan-500 rounded-2xl flex items-center justify-center mx-auto">
                <i data-lucide="mail" class="w-8 h-8 text-white"></i>
              </div>
            </div>
          </div>
          <h2 class="text-2xl font-bold text-center mb-2">
            <span class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-transparent">인증번호 발송 완료</span>
          </h2>
          <p id="verificationCodeModalEmail" class="text-center text-slate-400 mb-6"></p>
          <div class="bg-green-500/10 border border-green-500/30 rounded-xl p-4 mb-6 flex items-start gap-3">
            <i data-lucide="check-circle" class="w-5 h-5 text-green-400 flex-shrink-0 mt-0.5"></i>
            <div class="flex-1">
              <p class="text-green-400 text-sm font-medium mb-1">인증번호가 발송되었습니다.</p>
              <p class="text-slate-400 text-xs">입력한 이메일 주소로 인증번호를 확인해 주세요.</p>
            </div>
          </div>
          <div class="bg-sky-500/10 border border-sky-500/30 rounded-xl p-4 mb-6">
            <p class="text-sky-400 text-sm text-center">인증번호는 5분간 유효합니다.</p>
          </div>
          <button type="button" onclick="closeVerificationCodeModal()" class="w-full py-4 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 rounded-xl transition-all font-semibold text-white shadow-lg shadow-sky-500/50">
            확인
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 인증번호 검증 성공 후 이메일이 잠겼다는 것을 안내하는 모달이다. -->
  <div id="verificationSuccessModal" class="hidden fixed inset-0 z-50">
    <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" onclick="closeVerificationSuccessModal()"></div>
    <div class="fixed inset-0 flex items-center justify-center p-4">
      <div class="w-full max-w-md relative">
        <div class="absolute inset-0 bg-gradient-to-r from-green-600 to-emerald-600 rounded-3xl blur-2xl opacity-30"></div>
        <div class="relative bg-slate-900/95 backdrop-blur-xl rounded-3xl p-8 border border-white/10">
          <button type="button" onclick="closeVerificationSuccessModal()" class="absolute top-6 right-6 w-10 h-10 flex items-center justify-center bg-white/5 hover:bg-white/10 rounded-full transition-all">
            <i data-lucide="x" class="w-5 h-5 text-slate-400"></i>
          </button>
          <div class="text-center mb-6">
            <div class="relative inline-block">
              <div class="absolute inset-0 bg-gradient-to-r from-green-600 to-emerald-600 rounded-full blur-xl opacity-75"></div>
              <div class="relative w-20 h-20 bg-gradient-to-br from-green-500 to-emerald-500 rounded-full flex items-center justify-center mx-auto">
                <i data-lucide="check-circle" class="w-10 h-10 text-white"></i>
              </div>
            </div>
          </div>
          <h2 class="text-2xl font-bold text-center mb-2">
            <span class="bg-gradient-to-r from-green-400 to-emerald-400 bg-clip-text text-transparent">인증이 완료되었습니다.</span>
          </h2>
          <p class="text-center text-slate-400 mb-6">이메일 인증이 정상적으로 완료되었습니다.</p>
          <button type="button" onclick="closeVerificationSuccessModal()" class="w-full py-4 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-500 hover:to-emerald-500 rounded-xl transition-all font-semibold text-white shadow-lg shadow-green-500/50">
            확인
          </button>
        </div>
      </div>
    </div>
  </div>

  <script>
    // 회원가입 화면에서 여러 함수가 공유하는 상태 값이다.
    // verified: 이메일 인증 완료 여부
    // emailChecked: 중복확인 완료 여부
    // codeSent: 인증번호 발송 여부
    // zonecode: 다음 주소검색에서 받은 우편번호
    const state = {
      verified: false,
      timeLeft: 0,
      timerId: null,
      zonecode: "",
      sending: false,
      checkingEmail: false,
      emailChecked: false,
      emailCheckedValue: "",
      codeSent: false,
      postcode: null
    };

    // 공통 페이지 이동 함수다.
    function goPage(path) {
      window.location.href = path;
    }

    // 실패 메시지를 빨간 박스에 표시하고 성공 메시지는 숨긴다.
    function showError(message) {
      document.getElementById("successBox").classList.add("hidden");
      document.getElementById("errorText").textContent = message;
      document.getElementById("errorBox").classList.remove("hidden");
      lucide.createIcons();
    }

    // 성공 메시지를 초록 박스에 표시하고 실패 메시지는 숨긴다.
    function showSuccess(message) {
      document.getElementById("errorBox").classList.add("hidden");
      document.getElementById("successText").textContent = message;
      document.getElementById("successBox").classList.remove("hidden");
      lucide.createIcons();
    }

    // 이전 요청의 성공/실패 메시지를 모두 지워 새 결과만 보이게 한다.
    function clearMessage() {
      document.getElementById("errorBox").classList.add("hidden");
      document.getElementById("successBox").classList.add("hidden");
      document.getElementById("errorText").textContent = "";
      document.getElementById("successText").textContent = "";
    }

    // 남은 인증 시간을 분:초 문자열로 변환한다.
    function formatTime(seconds) {
      const mins = Math.floor(seconds / 60);
      const secs = String(seconds % 60).padStart(2, "0");
      return mins + ":" + secs;
    }

    // 인증번호 유효시간 표시를 현재 state.timeLeft 기준으로 갱신한다.
    function updateTimer() {
      const timerWrap = document.getElementById("timerWrap");
      const timerText = document.getElementById("timerText");
      if (state.timeLeft > 0) {
        timerWrap.classList.remove("hidden");
        timerText.textContent = formatTime(state.timeLeft);
      } else {
        timerWrap.classList.add("hidden");
        timerText.textContent = "";
      }
    }

    // 인증번호 발송 후 5분 카운트다운을 시작한다.
    function startTimer(seconds) {
      state.timeLeft = seconds;
      if (state.timerId) clearInterval(state.timerId);
      updateTimer();
      state.timerId = setInterval(function () {
        state.timeLeft -= 1;
        updateTimer();
        if (state.timeLeft <= 0) {
          clearInterval(state.timerId);
          state.timerId = null;
        }
      }, 1000);
    }

    // 이메일은 앞뒤 공백 제거와 소문자 변환을 거쳐 서버와 같은 기준으로 사용한다.
    function getNormalizedEmail() {
      return document.getElementById("email").value.trim().toLowerCase();
    }

    // 브라우저 submit 전에 기본 이메일 형식을 한 번 더 확인한다.
    function isValidEmail(email) {
      return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    // 이메일 입력값이 바뀌면 이전 중복확인/인증 결과는 더 이상 유효하지 않으므로 초기화한다.
    function resetEmailVerificationState() {
      state.verified = false;
      state.emailChecked = false;
      state.emailCheckedValue = "";
      state.codeSent = false;
      document.getElementById("verifyCodeWrap").classList.add("hidden");
      document.getElementById("verifyCode").value = "";
      document.getElementById("emailVerifiedIcon").classList.add("hidden");
      if (state.timerId) clearInterval(state.timerId);
      state.timerId = null;
      state.timeLeft = 0;
      updateTimer();
      updateEmailButtons();
    }

    // 현재 상태에 맞춰 중복확인/인증번호 받기 버튼의 활성화 여부와 문구를 갱신한다.
    function updateEmailButtons() {
      const checkEmailButton = document.getElementById("checkEmailButton");
      const sendCodeButton = document.getElementById("sendCodeButton");
      checkEmailButton.disabled = state.verified || state.checkingEmail;
      checkEmailButton.textContent = state.emailChecked ? "확인완료" : "중복확인";
      sendCodeButton.disabled = state.verified || state.sending || !state.emailChecked;
      sendCodeButton.textContent = state.verified ? "인증완료" : (state.codeSent ? "재발송" : "인증번호 받기");
    }

    // 인증이 완료되면 이메일 입력칸을 잠가 가입 직전에 다른 이메일로 바꾸는 문제를 막는다.
    function setEmailVerified(verified) {
      state.verified = verified;
      document.getElementById("email").disabled = verified;
      document.getElementById("emailVerifiedIcon").classList.toggle("hidden", !verified);
      updateEmailButtons();
    }

    // 다음 주소검색 모달을 열고, 사용자가 주소를 선택하면 입력칸에 값을 채운다.
    function openAddressModal() {
      const modal = document.getElementById("addressModal");
      modal.classList.remove("hidden");
      modal.classList.add("flex");

      // 외부 주소검색 스크립트가 로드되지 않았으면 모달을 닫고 오류를 보여준다.
      if (!window.daum || !window.daum.Postcode) {
        closeAddressModal();
        showError("주소 검색 서비스를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        return;
      }

      const wrap = document.getElementById("postcodeWrap");
      wrap.innerHTML = "";
      state.postcode = new daum.Postcode({
        width: "100%",
        height: "100%",
        // 주소 선택 완료 콜백: 기본주소, 건물명/법정동 추가정보, 우편번호를 화면 상태에 저장한다.
        oncomplete: function (data) {
          let fullAddress = data.address || data.roadAddress || data.jibunAddress || "";
          let extraAddress = "";

          if (data.addressType === "R") {
            if (data.bname) extraAddress += data.bname;
            if (data.buildingName) {
              extraAddress += extraAddress ? ", " + data.buildingName : data.buildingName;
            }
            fullAddress += extraAddress ? " (" + extraAddress + ")" : "";
          }

          document.getElementById("address").value = fullAddress;
          document.getElementById("detailAddress").classList.remove("hidden");
          document.getElementById("detailAddress").focus();
          state.zonecode = data.zonecode || "";
          closeAddressModal();
        }
      });
      state.postcode.embed(wrap);
      lucide.createIcons();
    }

    // 주소검색 모달을 닫는다.
    function closeAddressModal() {
      document.getElementById("addressModal").classList.add("hidden");
      document.getElementById("addressModal").classList.remove("flex");
    }

    // 인증번호 발송 완료 모달을 열고 어느 이메일로 보냈는지 표시한다.
    function openVerificationCodeModal(email) {
      document.getElementById("verificationCodeModalEmail").textContent = email;
      document.getElementById("verificationCodeModal").classList.remove("hidden");
      lucide.createIcons();
    }

    // 인증번호 발송 안내 모달을 닫는다.
    function closeVerificationCodeModal() {
      document.getElementById("verificationCodeModal").classList.add("hidden");
    }

    // 인증 성공 모달을 열어 사용자가 다음 단계로 진행해도 된다는 것을 알려준다.
    function openVerificationSuccessModal() {
      document.getElementById("verificationSuccessModal").classList.remove("hidden");
      lucide.createIcons();
    }

    // 인증 성공 모달을 닫는다.
    function closeVerificationSuccessModal() {
      document.getElementById("verificationSuccessModal").classList.add("hidden");
    }

    // 중복확인 버튼 클릭 흐름:
    // 1. 이메일 형식을 검사하고
    // 2. /signup/check-email API로 DB 중복 여부를 확인하고
    // 3. 성공하면 인증번호 받기 버튼을 활성화한다.
    async function handleCheckEmail() {
      clearMessage();
      const email = getNormalizedEmail();
      if (!email) return showError("이메일을 입력해 주세요.");
      if (!isValidEmail(email)) return showError("올바른 이메일 형식이 아닙니다.");

      state.checkingEmail = true;
      updateEmailButtons();

      try {
        // AuthApiController.checkEmail()과 연결되는 API 호출이다.
        const response = await fetch("<%= contextPath %>/api/v1/auth/signup/check-email", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: email })
        });
        const json = await response.json().catch(function () { return null; });

        if (json && json.success) {
          state.emailChecked = true;
          state.emailCheckedValue = email;
          showSuccess("사용 가능한 이메일입니다.");
          return;
        }

        state.emailChecked = false;
        state.emailCheckedValue = "";
        showError(json && json.error && json.error.message ? json.error.message : "이메일 중복 확인에 실패했습니다.");
      } catch (error) {
        state.emailChecked = false;
        state.emailCheckedValue = "";
        showError("이메일 중복 확인 중 오류가 발생했습니다.");
      } finally {
        state.checkingEmail = false;
        updateEmailButtons();
      }
    }

    // 인증번호 받기 버튼 클릭 흐름:
    // 1. 중복확인을 먼저 했는지 확인하고
    // 2. /signup/send-code API로 인증번호 메일을 보내고
    // 3. 인증번호 입력 영역과 타이머를 표시한다.
    async function handleSendVerificationCode() {
      clearMessage();
      const email = getNormalizedEmail();
      if (!email) return showError("이메일을 입력해 주세요.");
      if (!isValidEmail(email)) return showError("올바른 이메일 형식이 아닙니다.");
      if (!state.emailChecked || state.emailCheckedValue !== email) {
        resetEmailVerificationState();
        return showError("이메일 중복확인을 먼저 완료해 주세요.");
      }

      state.sending = true;
      updateEmailButtons();

      try {
        // AuthApiController.sendCode()과 연결되는 API 호출이다.
        const response = await fetch("<%= contextPath %>/api/v1/auth/signup/send-code", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: email })
        });
        const json = await response.json().catch(function () { return null; });

        if (json && json.success) {
          state.codeSent = true;
          document.getElementById("verifyCodeWrap").classList.remove("hidden");
          startTimer(300);
          showSuccess("인증번호가 이메일로 발송되었습니다. (" + email + ")");
          openVerificationCodeModal(email);
          return;
        }

        showError(json && json.error && json.error.message ? json.error.message : "인증번호 발송에 실패했습니다.");
      } catch (error) {
        showError("인증번호 발송 중 오류가 발생했습니다.");
      } finally {
        state.sending = false;
        updateEmailButtons();
      }
    }

    // 인증번호 확인 버튼 클릭 흐름:
    // 입력한 코드가 서버에 저장된 코드와 맞는지 검증하고, 성공하면 이메일 입력을 잠근다.
    async function handleVerifyCode() {
      clearMessage();
      const email = getNormalizedEmail();
      const code = document.getElementById("verifyCode").value.trim();
      if (!code) return showError("인증번호를 입력해 주세요.");

      try {
        // AuthApiController.verifyCode()과 연결되는 API 호출이다.
        const response = await fetch("<%= contextPath %>/api/v1/auth/signup/verify-code", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: email, code: code })
        });
        const json = await response.json().catch(function () { return null; });

        if (json && json.success) {
          setEmailVerified(true);
          document.getElementById("verifyCodeWrap").classList.add("hidden");
          if (state.timerId) clearInterval(state.timerId);
          state.timerId = null;
          state.timeLeft = 0;
          updateTimer();
          showSuccess("이메일 인증이 완료되었습니다.");
          openVerificationSuccessModal();
          return;
        }

        showError(json && json.error && json.error.message ? json.error.message : "인증번호 확인에 실패했습니다.");
      } catch (error) {
        showError("인증번호 확인 중 오류가 발생했습니다.");
      }
    }

    // 이메일이 바뀌면 이전 중복확인/인증 결과를 초기화한다.
    document.getElementById("email").addEventListener("input", function () {
      if (!state.verified) resetEmailVerificationState();
    });

    // 전화번호 입력 중 숫자만 남기고 010-1234-5678 형식으로 자동 변환한다.
    document.getElementById("phone").addEventListener("input", function (event) {
      const onlyNumbers = event.target.value.replace(/[^\d]/g, "").slice(0, 11);
      if (onlyNumbers.length < 4) event.target.value = onlyNumbers;
      else if (onlyNumbers.length < 8) event.target.value = onlyNumbers.slice(0, 3) + "-" + onlyNumbers.slice(3);
      else event.target.value = onlyNumbers.slice(0, 3) + "-" + onlyNumbers.slice(3, 7) + "-" + onlyNumbers.slice(7);
    });

    // 최종 회원가입 submit 흐름:
    // 1. 이메일 인증 여부와 비밀번호 확인을 검사하고
    // 2. 주소/전화번호/이름을 payload로 만들고
    // 3. /signup/complete API가 DB에 회원을 저장한다.
    document.getElementById("signupForm").addEventListener("submit", async function (event) {
      event.preventDefault();
      clearMessage();

      if (!state.verified) return showError("이메일 인증을 먼저 완료해 주세요.");

      const password = document.getElementById("password").value;
      const confirmPassword = document.getElementById("confirmPassword").value;
      if (password !== confirmPassword) return showError("비밀번호가 일치하지 않습니다.");

      const address = document.getElementById("address").value.trim();
      const detailAddress = document.getElementById("detailAddress").value.trim();
      if (!address) return showError("주소를 입력해 주세요.");

      const payload = {
        name: document.getElementById("name").value.trim(),
        email: getNormalizedEmail(),
        password: password,
        phoneNumber: document.getElementById("phone").value.trim(),
        address: address,
        detailAddress: detailAddress,
        zonecode: state.zonecode
      };

      try {
        // AuthApiController.complete()과 연결되는 API 호출이다.
        const response = await fetch("<%= contextPath %>/api/v1/auth/signup/complete", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        });
        const json = await response.json().catch(function () { return null; });

        if (json && json.success) {
          showSuccess("회원가입이 완료되었습니다.");
          setTimeout(function () {
            goPage("<%= contextPath %>/login");
          }, 1200);
          return;
        }

        showError(json && json.error && json.error.message ? json.error.message : "회원가입에 실패했습니다.");
      } catch (error) {
        showError("회원가입 처리 중 오류가 발생했습니다.");
      }
    });

    // 초기 렌더링 시 버튼 상태와 아이콘을 한 번 정리한다.
    updateEmailButtons();
    lucide.createIcons();
  </script>
</body>
</html>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  체크리스트 기준 주석: 구현(인증/회원): 아이디 찾기와 비밀번호 재설정 화면을 구성한다.
--%>
<%
  String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/deepscan-theme.css?v=3">
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>계정 찾기 - DeepScan</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700;800&display=swap" rel="stylesheet">
  <style>
    body {
      font-family: "Pretendard", "Noto Sans KR", sans-serif;
      background: #020617;
    }
    .glass-panel {
      background: rgba(15, 23, 42, 0.5);
      backdrop-filter: blur(20px);
    }
  </style>
</head>
<body class="ds-page min-h-screen bg-slate-950 text-white relative">
  <div class="absolute inset-0">
    <div class="absolute top-20 left-1/4 w-96 h-96 bg-sky-600/20 rounded-full blur-3xl"></div>
    <div class="absolute bottom-20 right-1/4 w-96 h-96 bg-cyan-600/20 rounded-full blur-3xl"></div>
  </div>

  <nav class="fixed top-0 left-0 right-0 z-50 bg-slate-950/50 backdrop-blur-xl border-b border-white/10">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center h-16">
        <button type="button" onclick="goPage('<%= contextPath %>/login')" class="flex items-center gap-2 text-slate-300 hover:text-white transition-colors">
          <i data-lucide="arrow-left" class="w-5 h-5"></i>
          <span>로그인으로 돌아가기</span>
        </button>
      </div>
    </div>
  </nav>

  <div class="pt-24 pb-12 px-4 flex items-center justify-center min-h-screen">
    <div class="w-full max-w-md relative z-10">
      <div class="text-center mb-8">
        <div class="relative inline-flex h-14 w-14 items-center justify-center mb-6">
          <div class="absolute inset-1 bg-cyan-500/25 blur-xl"></div>
          <img src="<%= contextPath %>/resources/image/deepscan-mark.svg?v=30" alt="DeepScan" class="relative h-12 w-12" width="48" height="48">
        </div>
        <h1 class="text-3xl font-bold text-white mb-2">계정 찾기</h1>
        <p class="text-slate-400">아이디 또는 비밀번호를 찾으세요</p>
      </div>

      <div class="glass-panel rounded-2xl border border-white/10 overflow-hidden">
        <div class="flex border-b border-white/10">
          <button id="idTab" type="button" onclick="switchTab('id')" class="flex-1 py-4 text-center transition-all font-semibold bg-gradient-to-r from-sky-600 to-cyan-600 text-white shadow-lg">아이디 찾기</button>
          <button id="passwordTab" type="button" onclick="switchTab('password')" class="flex-1 py-4 text-center transition-all font-semibold text-slate-400 hover:text-white hover:bg-white/5">비밀번호 찾기</button>
        </div>

        <div class="p-8">
          <div id="idPanel" class="space-y-4">
            <div>
              <label class="block text-sm text-slate-300 mb-2 font-medium">이름</label>
              <div class="relative">
                <i data-lucide="user" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input id="findName" type="text" placeholder="이름" class="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
              </div>
            </div>

            <div>
              <label class="block text-sm text-slate-300 mb-2 font-medium">전화번호</label>
              <div class="relative">
                <i data-lucide="phone" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input id="findPhone" type="tel" maxlength="13" placeholder="010-1234-5678" class="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
              </div>
            </div>

            <div id="foundEmailBox" class="hidden p-4 bg-green-500/10 border border-green-500/30 rounded-xl">
              <div class="flex items-center gap-2 text-green-400 mb-2">
                <i data-lucide="check" class="w-5 h-5"></i>
                <span class="font-semibold">아이디를 찾았습니다.</span>
              </div>
              <p class="text-green-300">회원님의 이메일: <strong id="foundEmailText"></strong></p>
            </div>

            <div id="idErrorBox" class="hidden p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm"></div>
            <div id="idSuccessBox" class="hidden p-4 bg-green-500/10 border border-green-500/30 rounded-xl text-green-400 text-sm"></div>

            <button type="button" onclick="handleFindId()" class="w-full py-3 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white rounded-xl transition-all shadow-lg shadow-sky-500/50 font-semibold">아이디 찾기</button>
            <button id="goLoginButton" type="button" onclick="goPage('<%= contextPath %>/login')" class="hidden w-full py-3 bg-white/10 hover:bg-white/20 text-white rounded-xl transition-colors font-semibold">로그인하러 가기</button>
          </div>

          <div id="passwordPanel" class="hidden space-y-4">
            <div id="verifyStep" class="space-y-4">
              <div>
                <label class="block text-sm text-slate-300 mb-2 font-medium">이메일(아이디)</label>
                <div class="relative">
                  <i data-lucide="mail" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                  <input id="resetEmail" type="email" placeholder="example@email.com" class="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
                </div>
              </div>

              <div>
                <label class="block text-sm text-slate-300 mb-2 font-medium">이름</label>
                <div class="relative">
                  <i data-lucide="user" class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                  <input id="resetName" type="text" placeholder="이름" class="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
                </div>
              </div>

              <div id="codeWrap" class="hidden">
                <label class="block text-sm text-slate-300 mb-2 font-medium">
                  인증번호
                  <span id="timerText" class="ml-2 text-sky-400"></span>
                </label>
                <div class="flex gap-2">
                  <input id="resetCode" type="text" maxlength="6" placeholder="인증번호 입력" class="flex-1 px-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
                  <button type="button" onclick="verifyResetCode()" class="px-4 py-3 bg-white/10 hover:bg-white/20 rounded-xl transition-colors font-medium">확인</button>
                </div>
              </div>

              <div id="pwErrorBox" class="hidden p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm"></div>
              <div id="pwSuccessBox" class="hidden p-4 bg-green-500/10 border border-green-500/30 rounded-xl text-green-400 text-sm"></div>

              <button id="sendResetButton" type="button" onclick="sendResetCode()" class="w-full py-3 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white rounded-xl transition-all shadow-lg shadow-sky-500/50 font-semibold">인증번호 발송</button>
            </div>

            <div id="resetStep" class="hidden space-y-4">
              <div>
                <label class="block text-sm text-slate-300 mb-2 font-medium">새 비밀번호</label>
                <input id="newPassword" type="password" placeholder="새 비밀번호 입력" class="w-full px-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
              </div>
              <div>
                <label class="block text-sm text-slate-300 mb-2 font-medium">새 비밀번호 확인</label>
                <input id="confirmNewPassword" type="password" placeholder="새 비밀번호 다시 입력" class="w-full px-4 py-3 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 backdrop-blur-xl">
              </div>
              <button type="button" onclick="resetPassword()" class="w-full py-3 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white rounded-xl transition-all shadow-lg shadow-sky-500/50 font-semibold">비밀번호 재설정</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div id="passwordCodeSentModal" class="hidden fixed inset-0 z-50">
    <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" onclick="closePasswordCodeSentModal()"></div>
    <div class="fixed inset-0 flex items-center justify-center p-4">
      <div class="w-full max-w-md relative">
        <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-30"></div>
        <div class="relative bg-slate-900/95 backdrop-blur-xl rounded-3xl p-8 border border-white/10">
          <button type="button" onclick="closePasswordCodeSentModal()" class="absolute top-6 right-6 w-10 h-10 flex items-center justify-center bg-white/5 hover:bg-white/10 rounded-full transition-all">
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
            <span class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-transparent">
              인증번호 발송 완료
            </span>
          </h2>

          <p id="passwordCodeSentModalEmail" class="text-center text-slate-400 mb-6"></p>

          <div class="bg-green-500/10 border border-green-500/30 rounded-xl p-4 mb-6 flex items-start gap-3">
            <i data-lucide="check-circle" class="w-5 h-5 text-green-400 flex-shrink-0 mt-0.5"></i>
            <div class="flex-1">
              <p class="text-green-400 text-sm font-medium mb-1">인증번호가 발송되었습니다.</p>
              <p class="text-slate-400 text-xs">입력한 이메일 주소에서 인증번호를 확인해 주세요.</p>
            </div>
          </div>

          <div class="bg-sky-500/10 border border-sky-500/30 rounded-xl p-4 mb-6">
            <p class="text-sky-400 text-sm text-center">인증번호는 5분간 유효합니다.</p>
          </div>

          <button type="button" onclick="closePasswordCodeSentModal()" class="w-full py-4 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 rounded-xl transition-all font-semibold text-white shadow-lg shadow-sky-500/50">
            확인
          </button>
        </div>
      </div>
    </div>
  </div>

  <div id="passwordResetSuccessModal" class="hidden fixed inset-0 z-50">
    <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" onclick="closePasswordResetSuccessModal()"></div>
    <div class="fixed inset-0 flex items-center justify-center p-4">
      <div class="w-full max-w-md relative">
        <div class="absolute inset-0 bg-gradient-to-r from-green-600 to-emerald-600 rounded-3xl blur-2xl opacity-30"></div>
        <div class="relative bg-slate-900/95 backdrop-blur-xl rounded-3xl p-8 border border-white/10">
          <button type="button" onclick="closePasswordResetSuccessModal()" class="absolute top-6 right-6 w-10 h-10 flex items-center justify-center bg-white/5 hover:bg-white/10 rounded-full transition-all">
            <i data-lucide="x" class="w-5 h-5 text-slate-400"></i>
          </button>

          <div class="text-center mb-6">
            <div class="relative inline-block">
              <div class="absolute -top-2 -right-2">
                <i data-lucide="sparkles" class="w-6 h-6 text-yellow-400"></i>
              </div>
              <div class="absolute -bottom-2 -left-2">
                <i data-lucide="sparkles" class="w-5 h-5 text-green-400"></i>
              </div>
              <div class="absolute inset-0 bg-gradient-to-r from-green-600 to-emerald-600 rounded-full blur-xl opacity-75"></div>
              <div class="relative w-20 h-20 bg-gradient-to-br from-green-500 to-emerald-500 rounded-full flex items-center justify-center mx-auto">
                <i data-lucide="check-circle" class="w-10 h-10 text-white"></i>
              </div>
            </div>
          </div>

          <h2 class="text-2xl font-bold text-center mb-2">
            <span class="bg-gradient-to-r from-green-400 to-emerald-400 bg-clip-text text-transparent">
              비밀번호 변경 완료
            </span>
          </h2>

          <p class="text-center text-slate-400 mb-6">새 비밀번호로 다시 로그인할 수 있습니다.</p>

          <div class="bg-green-500/10 border border-green-500/30 rounded-xl p-4 mb-6">
            <p class="text-green-400 text-sm text-center font-medium">비밀번호가 정상적으로 변경되었습니다.</p>
          </div>

          <button type="button" onclick="closePasswordResetSuccessModal()" class="w-full py-4 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-500 hover:to-emerald-500 rounded-xl transition-all font-semibold text-white shadow-lg shadow-green-500/50">
            로그인하러 가기
          </button>
        </div>
      </div>
    </div>
  </div>

  <div id="passwordFeedbackModal" class="hidden fixed inset-0 z-50">
    <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" onclick="closePasswordFeedbackModal()"></div>
    <div class="fixed inset-0 flex items-center justify-center p-4">
      <div class="w-full max-w-md relative">
        <div id="passwordFeedbackGlow" class="absolute inset-0 rounded-3xl blur-2xl opacity-30 bg-gradient-to-r from-red-600 to-rose-600"></div>
        <div class="relative bg-slate-900/95 backdrop-blur-xl rounded-3xl p-8 border border-white/10">
          <button type="button" onclick="closePasswordFeedbackModal()" class="absolute top-6 right-6 w-10 h-10 flex items-center justify-center bg-white/5 hover:bg-white/10 rounded-full transition-all">
            <i data-lucide="x" class="w-5 h-5 text-slate-400"></i>
          </button>

          <div class="text-center mb-6">
            <div class="relative inline-block">
              <div id="passwordFeedbackIconWrap" class="absolute inset-0 rounded-full blur-xl opacity-75 bg-gradient-to-r from-red-600 to-rose-600"></div>
              <div id="passwordFeedbackIconBg" class="relative w-20 h-20 rounded-full flex items-center justify-center mx-auto bg-gradient-to-br from-red-500 to-rose-500">
                <i id="passwordFeedbackIcon" data-lucide="alert-circle" class="w-10 h-10 text-white"></i>
              </div>
            </div>
          </div>

          <h2 class="text-2xl font-bold text-center mb-2">
            <span id="passwordFeedbackTitle" class="bg-gradient-to-r from-red-400 to-rose-400 bg-clip-text text-transparent">
              안내
            </span>
          </h2>

          <p id="passwordFeedbackMessage" class="text-center text-slate-300 mb-6"></p>

          <button type="button" onclick="closePasswordFeedbackModal()" id="passwordFeedbackButton" class="w-full py-4 rounded-xl transition-all font-semibold text-white shadow-lg bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 shadow-red-500/40">
            확인
          </button>
        </div>
      </div>
    </div>
  </div>

  <script>
    const contextPath = "<%= contextPath %>";
    let timerId = null;
    let timeLeft = 0;

    function goPage(path) {
      window.location.href = path;
    }

    function switchTab(tab) {
      document.getElementById("idPanel").classList.toggle("hidden", tab !== "id");
      document.getElementById("passwordPanel").classList.toggle("hidden", tab !== "password");
      document.getElementById("idTab").className = tab === "id"
        ? "flex-1 py-4 text-center transition-all font-semibold bg-gradient-to-r from-sky-600 to-cyan-600 text-white shadow-lg"
        : "flex-1 py-4 text-center transition-all font-semibold text-slate-400 hover:text-white hover:bg-white/5";
      document.getElementById("passwordTab").className = tab === "password"
        ? "flex-1 py-4 text-center transition-all font-semibold bg-gradient-to-r from-sky-600 to-cyan-600 text-white shadow-lg"
        : "flex-1 py-4 text-center transition-all font-semibold text-slate-400 hover:text-white hover:bg-white/5";
    }

    function formatPhone(value) {
      const onlyNumbers = value.replace(/[^\d]/g, "").slice(0, 11);
      if (onlyNumbers.length < 4) return onlyNumbers;
      if (onlyNumbers.length < 8) return onlyNumbers.slice(0, 3) + "-" + onlyNumbers.slice(3);
      return onlyNumbers.slice(0, 3) + "-" + onlyNumbers.slice(3, 7) + "-" + onlyNumbers.slice(7);
    }

    function getApiErrorMessage(result, fallbackMessage) {
      if (result && result.error && result.error.message) {
        return result.error.message;
      }
      return fallbackMessage;
    }

    function openPasswordCodeSentModal(email) {
      document.getElementById("passwordCodeSentModalEmail").textContent = email;
      document.getElementById("passwordCodeSentModal").classList.remove("hidden");
      lucide.createIcons();
    }

    function closePasswordCodeSentModal() {
      document.getElementById("passwordCodeSentModal").classList.add("hidden");
    }

    function openPasswordResetSuccessModal() {
      document.getElementById("passwordResetSuccessModal").classList.remove("hidden");
      lucide.createIcons();
    }

    function closePasswordResetSuccessModal() {
      document.getElementById("passwordResetSuccessModal").classList.add("hidden");
      goPage(contextPath + "/login");
    }

    function openPasswordFeedbackModal(title, message, tone) {
      const isSuccess = tone === "success";
      const glow = document.getElementById("passwordFeedbackGlow");
      const iconWrap = document.getElementById("passwordFeedbackIconWrap");
      const iconBg = document.getElementById("passwordFeedbackIconBg");
      const titleEl = document.getElementById("passwordFeedbackTitle");
      const icon = document.getElementById("passwordFeedbackIcon");
      const button = document.getElementById("passwordFeedbackButton");

      document.getElementById("passwordFeedbackTitle").textContent = title;
      document.getElementById("passwordFeedbackMessage").textContent = message;

      if (isSuccess) {
        glow.className = "absolute inset-0 rounded-3xl blur-2xl opacity-30 bg-gradient-to-r from-green-600 to-emerald-600";
        iconWrap.className = "absolute inset-0 rounded-full blur-xl opacity-75 bg-gradient-to-r from-green-600 to-emerald-600";
        iconBg.className = "relative w-20 h-20 rounded-full flex items-center justify-center mx-auto bg-gradient-to-br from-green-500 to-emerald-500";
        titleEl.className = "bg-gradient-to-r from-green-400 to-emerald-400 bg-clip-text text-transparent";
        button.className = "w-full py-4 rounded-xl transition-all font-semibold text-white shadow-lg bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-500 hover:to-emerald-500 shadow-green-500/40";
        icon.setAttribute("data-lucide", "check-circle");
      } else {
        glow.className = "absolute inset-0 rounded-3xl blur-2xl opacity-30 bg-gradient-to-r from-red-600 to-rose-600";
        iconWrap.className = "absolute inset-0 rounded-full blur-xl opacity-75 bg-gradient-to-r from-red-600 to-rose-600";
        iconBg.className = "relative w-20 h-20 rounded-full flex items-center justify-center mx-auto bg-gradient-to-br from-red-500 to-rose-500";
        titleEl.className = "bg-gradient-to-r from-red-400 to-rose-400 bg-clip-text text-transparent";
        button.className = "w-full py-4 rounded-xl transition-all font-semibold text-white shadow-lg bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 shadow-red-500/40";
        icon.setAttribute("data-lucide", "alert-circle");
      }

      document.getElementById("passwordFeedbackModal").classList.remove("hidden");
      lucide.createIcons();
    }

    function closePasswordFeedbackModal() {
      document.getElementById("passwordFeedbackModal").classList.add("hidden");
    }

    document.getElementById("findPhone").addEventListener("input", function(event) {
      event.target.value = formatPhone(event.target.value);
    });

    function startTimer() {
      timeLeft = 300;
      clearInterval(timerId);
      timerId = setInterval(function() {
        timeLeft -= 1;
        const mins = Math.floor(timeLeft / 60);
        const secs = String(timeLeft % 60).padStart(2, "0");
        document.getElementById("timerText").textContent = "(" + mins + ":" + secs + ")";

        if (timeLeft <= 0) {
          clearInterval(timerId);
          timerId = null;
          document.getElementById("timerText").textContent = "";
        }
      }, 1000);
    }

    async function handleFindId() {
      const name = document.getElementById("findName").value.trim();
      const phone = document.getElementById("findPhone").value.trim();
      document.getElementById("idErrorBox").classList.add("hidden");
      document.getElementById("idSuccessBox").classList.add("hidden");
      document.getElementById("foundEmailBox").classList.add("hidden");
      document.getElementById("goLoginButton").classList.add("hidden");

      if (!name || !phone) {
        document.getElementById("idErrorBox").textContent = "이름과 전화번호를 모두 입력해 주세요.";
        document.getElementById("idErrorBox").classList.remove("hidden");
        return;
      }

      try {
        const response = await fetch(contextPath + "/api/v1/auth/find-id", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: name,
            phoneNumber: phone
          })
        });
        const result = await response.json();

        if (!response.ok || !result.success) {
          document.getElementById("idErrorBox").textContent = getApiErrorMessage(result, "아이디 조회에 실패했습니다.");
          document.getElementById("idErrorBox").classList.remove("hidden");
          return;
        }

        const email = result.data && result.data.email ? result.data.email : "";
        if (!email) {
          document.getElementById("idErrorBox").textContent = "조회 결과에 아이디 정보가 없습니다.";
          document.getElementById("idErrorBox").classList.remove("hidden");
          return;
        }

        document.getElementById("foundEmailText").textContent = email;
        document.getElementById("foundEmailBox").classList.remove("hidden");
        document.getElementById("idSuccessBox").textContent = "등록된 이메일을 찾았습니다.";
        document.getElementById("idSuccessBox").classList.remove("hidden");
        document.getElementById("goLoginButton").classList.remove("hidden");
      } catch (error) {
        document.getElementById("idErrorBox").textContent = "서버 호출 중 오류가 발생했습니다.";
        document.getElementById("idErrorBox").classList.remove("hidden");
      }
    }

    async function sendResetCode() {
      const email = document.getElementById("resetEmail").value.trim();
      const name = document.getElementById("resetName").value.trim();
      document.getElementById("pwErrorBox").classList.add("hidden");
      document.getElementById("pwSuccessBox").classList.add("hidden");

      if (!email || !name) {
        document.getElementById("pwErrorBox").textContent = "이메일과 이름을 모두 입력해 주세요.";
        document.getElementById("pwErrorBox").classList.remove("hidden");
        return;
      }

      try {
        const response = await fetch(contextPath + "/api/v1/auth/password/send-code", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: email,
            name: name
          })
        });
        const result = await response.json();

        if (!response.ok || !result.success) {
          const message = getApiErrorMessage(result, "인증번호 발송에 실패했습니다.");
          document.getElementById("pwErrorBox").textContent = message;
          document.getElementById("pwErrorBox").classList.remove("hidden");
          openPasswordFeedbackModal("인증번호 발송 실패", message, "error");
          return;
        }

        document.getElementById("codeWrap").classList.remove("hidden");
        document.getElementById("pwSuccessBox").textContent = "인증번호를 이메일로 발송했습니다.";
        document.getElementById("pwSuccessBox").classList.remove("hidden");
        startTimer();
        openPasswordCodeSentModal(email);
      } catch (error) {
        const message = "서버 호출 중 오류가 발생했습니다.";
        document.getElementById("pwErrorBox").textContent = message;
        document.getElementById("pwErrorBox").classList.remove("hidden");
        openPasswordFeedbackModal("인증번호 발송 실패", message, "error");
      }
    }

    async function verifyResetCode() {
      const email = document.getElementById("resetEmail").value.trim();
      const code = document.getElementById("resetCode").value.trim();
      document.getElementById("pwErrorBox").classList.add("hidden");
      document.getElementById("pwSuccessBox").classList.add("hidden");

      if (!code) {
        document.getElementById("pwErrorBox").textContent = "인증번호를 입력해 주세요.";
        document.getElementById("pwErrorBox").classList.remove("hidden");
        return;
      }

      try {
        const response = await fetch(contextPath + "/api/v1/auth/password/verify-code", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: email,
            code: code
          })
        });
        const result = await response.json();

        if (!response.ok || !result.success) {
          document.getElementById("pwErrorBox").textContent = getApiErrorMessage(result, "인증번호 확인에 실패했습니다.");
          document.getElementById("pwErrorBox").classList.remove("hidden");
          return;
        }

        document.getElementById("pwSuccessBox").textContent = "이메일 인증이 완료되었습니다.";
        document.getElementById("pwSuccessBox").classList.remove("hidden");
        document.getElementById("verifyStep").classList.add("hidden");
        document.getElementById("resetStep").classList.remove("hidden");
      } catch (error) {
        document.getElementById("pwErrorBox").textContent = "서버 호출 중 오류가 발생했습니다.";
        document.getElementById("pwErrorBox").classList.remove("hidden");
      }
    }

    async function resetPassword() {
      const email = document.getElementById("resetEmail").value.trim();
      const newPassword = document.getElementById("newPassword").value;
      const confirmNewPassword = document.getElementById("confirmNewPassword").value;
      document.getElementById("pwErrorBox").classList.add("hidden");
      document.getElementById("pwSuccessBox").classList.add("hidden");

      if (!newPassword || !confirmNewPassword) {
        document.getElementById("pwErrorBox").textContent = "새 비밀번호를 모두 입력해 주세요.";
        document.getElementById("pwErrorBox").classList.remove("hidden");
        return;
      }
      if (newPassword.length < 6) {
        document.getElementById("pwErrorBox").textContent = "비밀번호는 최소 6자 이상이어야 합니다.";
        document.getElementById("pwErrorBox").classList.remove("hidden");
        return;
      }
      if (newPassword !== confirmNewPassword) {
        document.getElementById("pwErrorBox").textContent = "비밀번호 확인이 일치하지 않습니다.";
        document.getElementById("pwErrorBox").classList.remove("hidden");
        return;
      }

      try {
        const response = await fetch(contextPath + "/api/v1/auth/password/reset", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: email,
            password: newPassword
          })
        });
        const result = await response.json();

        if (!response.ok || !result.success) {
          const message = getApiErrorMessage(result, "비밀번호 재설정에 실패했습니다.");
          document.getElementById("pwErrorBox").textContent = message;
          document.getElementById("pwErrorBox").classList.remove("hidden");
          openPasswordFeedbackModal("비밀번호 변경 실패", message, "error");
          return;
        }

        document.getElementById("pwSuccessBox").textContent = "비밀번호가 변경되었습니다. 로그인 페이지로 이동합니다.";
        document.getElementById("pwSuccessBox").classList.remove("hidden");
        openPasswordResetSuccessModal();
      } catch (error) {
        const message = "서버 호출 중 오류가 발생했습니다.";
        document.getElementById("pwErrorBox").textContent = message;
        document.getElementById("pwErrorBox").classList.remove("hidden");
        openPasswordFeedbackModal("비밀번호 변경 실패", message, "error");
      }
    }

    lucide.createIcons();
  </script>
</body>
</html>

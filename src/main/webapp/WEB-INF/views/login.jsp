<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  String contextPath = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/deepscan-theme.css?v=4">
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>로그인 - DeepScan</title>
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
  </style>
</head>
<body class="ds-page ds-auth-page min-h-screen bg-slate-950 text-white flex items-center justify-center p-4 relative">
  <div class="absolute inset-0">
    <div class="absolute top-20 left-1/4 w-96 h-96 bg-sky-600/30 rounded-full blur-3xl"></div>
    <div class="absolute bottom-20 right-1/4 w-96 h-96 bg-cyan-600/30 rounded-full blur-3xl"></div>
  </div>

  <div class="pointer-events-none fixed inset-x-0 top-0 z-50 border-b border-white/10 bg-slate-950/80 backdrop-blur-xl">
    <div class="mx-auto max-w-7xl px-4 py-4 sm:px-6 lg:px-8">
      <button
        type="button"
        onclick="goPage('<%= contextPath %>/')"
        class="pointer-events-auto flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm font-medium backdrop-blur-xl transition-all hover:bg-white/20"
      >
        <i data-lucide="arrow-left" class="w-4 h-4"></i>
        홈으로
      </button>
    </div>
  </div>

  <div class="w-full max-w-md relative">
    <div class="text-center mb-12">
      <div class="relative inline-flex h-20 w-20 items-center justify-center mb-6">
        <div class="absolute inset-2 bg-cyan-500/25 blur-2xl"></div>
        <img src="<%= contextPath %>/resources/image/deepscan-mark.svg?v=30" alt="DeepScan" class="relative h-16 w-16" width="64" height="64">
      </div>
      <h1 class="text-4xl font-bold mb-3">
        <span class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-transparent">로그인</span>
      </h1>
      <p class="text-slate-400">DeepScan 계정으로 로그인해 주세요.</p>
    </div>

    <div class="relative group">
      <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-3xl blur-2xl opacity-20 group-hover:opacity-30 transition-opacity"></div>
      <div class="relative bg-slate-900/50 backdrop-blur-xl rounded-3xl p-8 border border-white/10">
        <form id="loginForm" class="space-y-6">
          <div>
            <label for="email" class="block text-sm text-slate-300 mb-2 font-medium">이메일</label>
            <div class="relative">
              <i data-lucide="mail" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
              <input
                id="email"
                type="email"
                placeholder="your_email@example.com"
                required
                class="w-full pl-12 pr-4 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all backdrop-blur-xl"
              >
            </div>
          </div>

          <div>
            <label for="password" class="block text-sm text-slate-300 mb-2 font-medium">비밀번호</label>
            <div class="relative">
              <i data-lucide="lock" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
              <input
                id="password"
                type="password"
                placeholder="비밀번호를 입력해 주세요."
                required
                class="w-full pl-12 pr-4 py-4 bg-white/5 border border-white/20 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all backdrop-blur-xl"
              >
            </div>
          </div>

          <div id="errorBox" class="hidden p-4 bg-red-500/10 border border-red-500/50 rounded-xl flex items-start gap-3">
            <i data-lucide="alert-circle" class="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5"></i>
            <p id="errorText" class="text-red-400 text-sm"></p>
          </div>

          <button
            id="submitBtn"
            type="submit"
            class="w-full py-4 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 disabled:from-slate-700 disabled:to-slate-700 disabled:cursor-not-allowed text-white rounded-xl transition-all shadow-2xl shadow-sky-500/50 font-semibold text-lg"
          >
            로그인
          </button>

          <a
            href="<%= contextPath %>/oauth2/authorization/google"
            class="flex w-full items-center justify-center gap-3 rounded-xl border border-white/15 bg-white px-4 py-4 font-semibold text-slate-900 transition-all hover:bg-slate-100"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" aria-hidden="true">
              <path fill="#EA4335" d="M12 10.2v3.9h5.5c-.2 1.3-1.5 3.9-5.5 3.9-3.3 0-6-2.7-6-6s2.7-6 6-6c1.9 0 3.1.8 3.8 1.5l2.6-2.5C16.8 3.5 14.6 2.5 12 2.5 6.8 2.5 2.5 6.8 2.5 12s4.3 9.5 9.5 9.5c5.5 0 9.1-3.8 9.1-9.2 0-.6-.1-1.1-.2-1.6H12z"/>
              <path fill="#34A853" d="M3.6 7.4l3.2 2.3C7.7 7.7 9.7 6 12 6c1.9 0 3.1.8 3.8 1.5l2.6-2.5C16.8 3.5 14.6 2.5 12 2.5 8.3 2.5 5.1 4.6 3.6 7.4z"/>
              <path fill="#4A90E2" d="M12 21.5c2.5 0 4.7-.8 6.2-2.3l-3-2.4c-.8.6-1.8 1.2-3.2 1.2-3.9 0-5.2-2.6-5.5-3.8l-3.2 2.4c1.5 2.9 4.6 4.9 8.7 4.9z"/>
              <path fill="#FBBC05" d="M6.5 14.2c-.2-.6-.4-1.3-.4-2s.1-1.4.4-2L3.3 7.8C2.8 9 2.5 10.5 2.5 12s.3 3 .8 4.2l3.2-2z"/>
            </svg>
            Google로 로그인
          </a>

          <div class="flex items-center justify-between pt-4">
            <button
              type="button"
              onclick="goPage('<%= contextPath %>/find-account')"
              class="text-sky-400 hover:text-sky-300 text-sm transition-colors"
            >
              아이디 / 비밀번호 찾기
            </button>
            <button
              type="button"
              onclick="goPage('<%= contextPath %>/signup')"
              class="text-sky-400 hover:text-sky-300 text-sm transition-colors"
            >
              회원가입
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>

  <script>
    const contextPath = "<%= contextPath %>";
    const form = document.getElementById("loginForm");
    const submitBtn = document.getElementById("submitBtn");
    const errorBox = document.getElementById("errorBox");
    const errorText = document.getElementById("errorText");

    function goPage(path) {
      window.location.href = path;
    }

    function showError(message) {
      errorText.textContent = message;
      errorBox.classList.remove("hidden");
      lucide.createIcons();
    }

    function clearError() {
      errorText.textContent = "";
      errorBox.classList.add("hidden");
    }

    function setLoading(isLoading) {
      submitBtn.disabled = isLoading;
      submitBtn.textContent = isLoading ? "로그인 중..." : "로그인";
    }

    form.addEventListener("submit", async function (event) {
      event.preventDefault();
      clearError();
      setLoading(true);

      try {
        const response = await fetch(contextPath + "/api/v1/auth/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            email: document.getElementById("email").value.trim(),
            password: document.getElementById("password").value
          })
        });

        const json = await response.json().catch(function () {
          return null;
        });

        if (json && json.success) {
          sessionStorage.setItem("appToast", JSON.stringify({
            type: "success",
            title: "로그인되었습니다",
            description: "DeepScan에 오신 것을 환영합니다."
          }));
          window.location.href = contextPath + "/";
          return;
        }

        showError(
          json && json.error && json.error.message
            ? json.error.message
            : "이메일 또는 비밀번호를 확인해 주세요."
        );
      } catch (error) {
        showError("로그인 처리 중 오류가 발생했습니다.");
      } finally {
        setLoading(false);
      }
    });

    if (new URLSearchParams(window.location.search).get("oauthError")) {
      showError("Google 로그인 처리 중 오류가 발생했습니다. 설정값과 계정 정보를 확인해 주세요.");
    }

    lucide.createIcons();
  </script>
</body>
</html>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  String contextPath = request.getContextPath();
  Object userIdObj = session.getAttribute("USER_ID");
  if (userIdObj == null) {
    response.sendRedirect(contextPath + "/login");
    return;
  }
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/deepscan-theme.css?v=3">
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>게시글 작성 - DeepScan</title>
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
    .reveal {
      opacity: 0;
      transform: translateY(24px) scale(0.985);
      transition: opacity 0.7s ease, transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
      transition-delay: var(--reveal-delay, 0ms);
    }
    .reveal.reveal-visible {
      opacity: 1;
      transform: translateY(0) scale(1);
    }
  </style>
</head>
<body class="ds-page min-h-screen bg-slate-950 text-white relative overflow-hidden">
  <div class="absolute inset-0">
    <div class="absolute top-20 left-1/4 h-96 w-96 rounded-full bg-gradient-to-r from-sky-600 to-cyan-600 opacity-20 blur-3xl"></div>
    <div class="absolute bottom-20 right-1/3 h-96 w-96 rounded-full bg-gradient-to-r from-cyan-600 to-blue-600 opacity-20 blur-3xl"></div>
  </div>

  <button type="button" onclick="goPage('<%= contextPath %>/community')" class="fixed left-6 top-6 z-50 flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm font-medium backdrop-blur-xl transition-all hover:bg-white/20 reveal" data-reveal>
    <i data-lucide="arrow-left" class="h-4 w-4"></i>
    목록으로
  </button>

  <main class="relative px-4 pb-12 pt-24">
    <div class="mx-auto max-w-3xl">
      <section class="relative group reveal" style="--reveal-delay: 120ms;" data-reveal>
        <div class="absolute inset-0 rounded-3xl bg-gradient-to-r from-sky-600 to-cyan-600 opacity-20 blur-2xl transition-opacity group-hover:opacity-30"></div>
        <div class="relative rounded-3xl border border-white/10 bg-slate-900/50 p-8 backdrop-blur-xl">
          <div class="mb-8">
            <p class="mb-3 inline-flex items-center gap-2 rounded-full border border-sky-400/20 bg-sky-400/10 px-4 py-2 text-sm text-sky-300">
              <i data-lucide="pen-square" class="h-4 w-4"></i>
              커뮤니티 글쓰기
            </p>
            <h1 class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-4xl font-bold text-transparent">게시글 작성</h1>
            <p class="mt-3 text-slate-400">딥페이크 사례, 검증 팁, 질문을 자연스럽게 공유할 수 있도록 제목과 내용을 작성해 주세요.</p>
          </div>

          <form id="postForm" class="space-y-6">
            <div>
              <label class="mb-3 block text-sm font-medium text-slate-300">주제</label>
              <div class="grid grid-cols-2 gap-3 md:grid-cols-4">
                <button type="button" class="topic-option rounded-xl border border-fuchsia-400/40 bg-fuchsia-500/10 px-4 py-3 text-sm font-medium text-fuchsia-200 transition-all hover:bg-fuchsia-500/20" data-topic="deepfake">딥페이크 감별</button>
                <button type="button" class="topic-option rounded-xl border border-white/10 bg-slate-800/50 px-4 py-3 text-sm font-medium text-slate-300 transition-all hover:bg-slate-800" data-topic="ai-tech">AI 기술</button>
                <button type="button" class="topic-option rounded-xl border border-white/10 bg-slate-800/50 px-4 py-3 text-sm font-medium text-slate-300 transition-all hover:bg-slate-800" data-topic="news">뉴스 & 이슈</button>
                <button type="button" class="topic-option rounded-xl border border-white/10 bg-slate-800/50 px-4 py-3 text-sm font-medium text-slate-300 transition-all hover:bg-slate-800" data-topic="discussion">자유 토론</button>
              </div>
              <input id="topic" type="hidden" value="deepfake">
            </div>

            <div>
              <label for="title" class="mb-2 block text-sm font-medium text-slate-300">제목</label>
              <input id="title" type="text" placeholder="게시글 제목을 입력하세요" class="w-full rounded-xl border border-white/10 bg-slate-800/50 px-4 py-3 text-white placeholder-slate-500 transition-all focus:border-transparent focus:outline-none focus:ring-2 focus:ring-sky-500">
            </div>

            <div>
              <label for="content" class="mb-2 block text-sm font-medium text-slate-300">내용</label>
              <textarea id="content" rows="12" placeholder="내용을 입력하세요" class="w-full resize-none rounded-xl border border-white/10 bg-slate-800/50 px-4 py-3 text-white placeholder-slate-500 transition-all focus:border-transparent focus:outline-none focus:ring-2 focus:ring-sky-500"></textarea>
            </div>

            <div id="errorBox" class="hidden items-start gap-3 rounded-xl border border-red-500/30 bg-red-500/10 p-4">
              <i data-lucide="alert-circle" class="mt-0.5 h-5 w-5 flex-shrink-0 text-red-400"></i>
              <p id="errorText" class="text-sm text-red-300"></p>
            </div>

            <div class="flex gap-4 pt-2">
              <button id="submitBtn" type="submit" class="flex-1 rounded-xl bg-gradient-to-r from-sky-600 to-cyan-600 py-3 font-medium text-white shadow-lg shadow-sky-500/50 transition-all hover:from-sky-500 hover:to-cyan-500">작성하기</button>
              <button type="button" onclick="goPage('<%= contextPath %>/community')" class="rounded-xl border border-white/20 bg-white/5 px-6 py-3 font-medium text-white transition-all hover:bg-white/10">취소</button>
            </div>
          </form>
        </div>
      </section>
    </div>
  </main>

  <script>
    const contextPath = "<%= contextPath %>";

    function goPage(path) {
      window.location.href = path;
    }

    function showError(message) {
      document.getElementById("errorText").textContent = message;
      document.getElementById("errorBox").classList.remove("hidden");
      document.getElementById("errorBox").classList.add("flex");
      lucide.createIcons();
    }

    function clearError() {
      document.getElementById("errorText").textContent = "";
      document.getElementById("errorBox").classList.add("hidden");
      document.getElementById("errorBox").classList.remove("flex");
    }

    function applyTopicSelection(selectedTopic) {
      document.getElementById("topic").value = selectedTopic;
      document.querySelectorAll(".topic-option").forEach(function (button) {
        const topic = button.dataset.topic;
        const isActive = topic === selectedTopic;

        if (!isActive) {
          button.className = "topic-option rounded-xl border border-white/10 bg-slate-800/50 px-4 py-3 text-sm font-medium text-slate-300 transition-all hover:bg-slate-800";
          return;
        }

        const activeClass = {
          "deepfake": "rounded-xl border border-fuchsia-400/40 bg-fuchsia-500/10 px-4 py-3 text-sm font-medium text-fuchsia-200 transition-all hover:bg-fuchsia-500/20",
          "ai-tech": "rounded-xl border border-blue-400/40 bg-blue-500/10 px-4 py-3 text-sm font-medium text-blue-200 transition-all hover:bg-blue-500/20",
          "news": "rounded-xl border border-emerald-400/40 bg-emerald-500/10 px-4 py-3 text-sm font-medium text-emerald-200 transition-all hover:bg-emerald-500/20",
          "discussion": "rounded-xl border border-orange-400/40 bg-orange-500/10 px-4 py-3 text-sm font-medium text-orange-200 transition-all hover:bg-orange-500/20"
        };

        button.className = "topic-option " + activeClass[topic];
      });
    }

    document.querySelectorAll(".topic-option").forEach(function (button) {
      button.addEventListener("click", function () {
        applyTopicSelection(button.dataset.topic);
      });
    });

    document.getElementById("postForm").addEventListener("submit", async function (event) {
      event.preventDefault();
      clearError();

      const topic = document.getElementById("topic").value.trim();
      const title = document.getElementById("title").value.trim();
      const content = document.getElementById("content").value.trim();

      if (!topic) {
        showError("주제를 선택해 주세요.");
        return;
      }

      if (!title || !content) {
        showError("주제, 제목, 내용을 모두 입력해 주세요.");
        return;
      }

      const submitBtn = document.getElementById("submitBtn");
      submitBtn.disabled = true;
      submitBtn.textContent = "작성 중...";

      try {
        const response = await fetch(contextPath + "/api/v1/community/posts", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ topic: topic, title: title, content: content })
        });

        const json = await response.json().catch(function () {
          return null;
        });

        if (json && json.success) {
          window.location.href = contextPath + "/community";
          return;
        }

        showError(json && json.error && json.error.message ? json.error.message : "게시글 작성에 실패했습니다.");
      } catch (error) {
        showError("게시글 작성 중 오류가 발생했습니다.");
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "작성하기";
      }
    });

    function initReveal() {
      const items = Array.from(document.querySelectorAll("[data-reveal]"));
      if ("IntersectionObserver" in window) {
        const observer = new IntersectionObserver(function (entries, currentObserver) {
          entries.forEach(function (entry) {
            if (entry.isIntersecting) {
              entry.target.classList.add("reveal-visible");
              currentObserver.unobserve(entry.target);
            }
          });
        }, { threshold: 0.14, rootMargin: "0px 0px -8% 0px" });
        items.forEach(function (item) {
          observer.observe(item);
        });
      } else {
        items.forEach(function (item) {
          item.classList.add("reveal-visible");
        });
      }
    }

    applyTopicSelection("deepfake");
    initReveal();
    lucide.createIcons();
  </script>
</body>
</html>

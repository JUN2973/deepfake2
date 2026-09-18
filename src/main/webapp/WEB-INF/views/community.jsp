<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
  String contextPath = request.getContextPath();
  Object userIdObj = session.getAttribute("USER_ID");
  boolean isAuthenticated = (userIdObj != null);
  String createPostHref = contextPath + (isAuthenticated ? "/community/create" : "/login");
  request.setAttribute("activePage", "community");
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>커뮤니티 - DeepScan</title>
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
    .card-hover-lift {
      transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
    }
    .card-hover-lift:hover {
      transform: translateY(-6px);
      box-shadow: 0 22px 56px rgba(14, 165, 233, 0.12);
    }
    .topic-chip {
      transition: transform 0.25s ease, border-color 0.25s ease, background-color 0.25s ease, box-shadow 0.25s ease;
    }
    .topic-chip:hover {
      transform: translateY(-3px);
    }
    .topic-chip[data-topic="all"]:hover {
      border-color: rgba(56, 189, 248, 0.45);
      background: rgba(15, 23, 42, 0.78);
      box-shadow: 0 14px 30px rgba(14, 165, 233, 0.16);
    }
    .topic-chip[data-topic="deepfake"]:hover {
      border-color: rgba(217, 70, 239, 0.42);
      background: rgba(88, 28, 135, 0.16);
      box-shadow: 0 14px 30px rgba(217, 70, 239, 0.14);
    }
    .topic-chip[data-topic="ai-tech"]:hover {
      border-color: rgba(96, 165, 250, 0.42);
      background: rgba(30, 64, 175, 0.14);
      box-shadow: 0 14px 30px rgba(59, 130, 246, 0.14);
    }
    .topic-chip[data-topic="news"]:hover {
      border-color: rgba(52, 211, 153, 0.42);
      background: rgba(6, 95, 70, 0.14);
      box-shadow: 0 14px 30px rgba(16, 185, 129, 0.14);
    }
    .topic-chip[data-topic="discussion"]:hover {
      border-color: rgba(251, 146, 60, 0.42);
      background: rgba(154, 52, 18, 0.14);
      box-shadow: 0 14px 30px rgba(249, 115, 22, 0.14);
    }
    .skeleton-shimmer {
      position: relative;
      overflow: hidden;
      background: rgba(51, 65, 85, 0.42);
    }
    .skeleton-shimmer::after {
      content: "";
      position: absolute;
      inset: 0;
      transform: translateX(-100%);
      background: linear-gradient(90deg, transparent, rgba(148, 163, 184, 0.12), transparent);
      animation: skeleton-slide 1.35s infinite;
    }
    .post-topic-line {
      position: absolute;
      inset: 0 auto 0 0;
      width: 3px;
      border-radius: 1rem 0 0 1rem;
    }
    @keyframes skeleton-slide {
      100% { transform: translateX(100%); }
    }
    @media (prefers-reduced-motion: reduce) {
      .skeleton-shimmer::after { animation: none; }
    }
  </style>
</head>
<body class="ds-page min-h-screen bg-slate-950 text-white relative">
  <div class="absolute inset-0 pointer-events-none">
    <div class="absolute top-20 left-1/4 w-96 h-96 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-full blur-3xl opacity-20"></div>
    <div class="absolute bottom-20 right-1/4 w-96 h-96 bg-gradient-to-r from-purple-600 to-pink-600 rounded-full blur-3xl opacity-20"></div>
  </div>

  <%@ include file="common/dashboard-nav.jspf" %>

  <div class="pt-24 pb-12 px-4 relative">
    <div class="max-w-7xl mx-auto">
      <div class="text-center mb-12 reveal" data-reveal>
        <div class="relative inline-block mb-6">
          <div class="absolute inset-0 bg-gradient-to-r from-sky-600 to-cyan-600 rounded-2xl blur-2xl opacity-75"></div>
          <div class="relative w-20 h-20 bg-gradient-to-br from-sky-500 to-cyan-500 rounded-2xl flex items-center justify-center mx-auto shadow-lg shadow-sky-500/50">
            <i data-lucide="message-square" class="w-10 h-10 text-white"></i>
          </div>
        </div>
        <h1 class="text-5xl md:text-6xl font-bold mb-4">커뮤니티</h1>
        <p class="text-xl text-slate-400">딥페이크와 AI 이미지 사례, 검증 정보를 나누는 공간입니다.</p>
      </div>

      <div class="mb-8 reveal" style="--reveal-delay: 100ms;" data-reveal>
        <div class="bg-slate-900/50 backdrop-blur-xl rounded-3xl p-6 border border-white/10 card-hover-lift">
          <h2 class="text-lg font-semibold mb-4 text-white">주제 선택</h2>
          <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
            <button type="button" class="topic-chip relative group p-4 rounded-2xl transition-all bg-gradient-to-br from-slate-800 to-slate-900 border-2 border-sky-500/50 shadow-lg shadow-sky-500/30" data-topic="all">
              <div class="flex items-center gap-2 mb-2 text-sky-400 transition-colors"><i data-lucide="flame" class="w-5 h-5"></i><span class="font-semibold text-sm">전체</span></div>
              <p class="text-xs text-slate-500 text-left">모든 주제의 게시글</p>
              <div class="absolute inset-0 bg-gradient-to-r from-sky-600/10 to-cyan-600/10 rounded-2xl pointer-events-none"></div>
            </button>
            <button type="button" class="topic-chip relative group p-4 rounded-2xl transition-all bg-slate-800/30 border border-white/10" data-topic="deepfake">
              <div class="flex items-center gap-2 mb-2 text-slate-400 group-hover:text-fuchsia-300 transition-colors"><i data-lucide="eye" class="w-5 h-5"></i><span class="font-semibold text-sm">딥페이크 감별</span></div>
              <p class="text-xs text-slate-500 text-left">딥페이크 탐지와 검증</p>
            </button>
            <button type="button" class="topic-chip relative group p-4 rounded-2xl transition-all bg-slate-800/30 border border-white/10" data-topic="ai-tech">
              <div class="flex items-center gap-2 mb-2 text-slate-400 group-hover:text-blue-300 transition-colors"><i data-lucide="trending-up" class="w-5 h-5"></i><span class="font-semibold text-sm">AI 기술</span></div>
              <p class="text-xs text-slate-500 text-left">AI 이미지 생성 기술</p>
            </button>
            <button type="button" class="topic-chip relative group p-4 rounded-2xl transition-all bg-slate-800/30 border border-white/10" data-topic="news">
              <div class="flex items-center gap-2 mb-2 text-slate-400 group-hover:text-emerald-300 transition-colors"><i data-lucide="newspaper" class="w-5 h-5"></i><span class="font-semibold text-sm">뉴스 & 이슈</span></div>
              <p class="text-xs text-slate-500 text-left">최신 뉴스와 이슈</p>
            </button>
            <button type="button" class="topic-chip relative group p-4 rounded-2xl transition-all bg-slate-800/30 border border-white/10" data-topic="discussion">
              <div class="flex items-center gap-2 mb-2 text-slate-400 group-hover:text-orange-300 transition-colors"><i data-lucide="user" class="w-5 h-5"></i><span class="font-semibold text-sm">자유 토론</span></div>
              <p class="text-xs text-slate-500 text-left">자유로운 의견 교환</p>
            </button>
          </div>
        </div>
      </div>

      <div class="grid lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 space-y-6">
          <div class="space-y-4 reveal" style="--reveal-delay: 160ms;" data-reveal>
            <div class="flex items-center gap-3">
              <div class="relative min-w-0 flex-1">
                <i data-lucide="search" class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-500"></i>
                <input id="searchInput" type="text" placeholder="제목이나 내용 검색" class="w-full pl-12 pr-4 py-3 bg-slate-900/50 backdrop-blur-xl border border-white/10 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all">
              </div>
              <button type="button" onclick="goPage('<%= createPostHref %>')" class="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-sky-600 to-cyan-600 px-5 py-3 font-semibold text-white shadow-lg shadow-sky-500/40 transition-all hover:from-sky-500 hover:to-cyan-500">
                <i data-lucide="pen-square" class="h-5 w-5"></i>
                <span class="hidden sm:inline">&#51089;&#49457;&#54616;&#44592;</span>
              </button>
            </div>

            <div class="flex items-center gap-2 flex-wrap">
              <span class="text-sm text-slate-400">정렬:</span>
              <button type="button" class="sort-chip flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-all bg-sky-500/20 text-sky-400 border border-sky-500/30" data-sort="recent">
                <i data-lucide="clock" class="w-4 h-4"></i>최신순
              </button>
              <button type="button" class="sort-chip flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-all bg-slate-800/50 text-slate-400 hover:text-white border border-white/10" data-sort="popular">
                <i data-lucide="flame" class="w-4 h-4"></i>인기순
              </button>
              <button type="button" class="sort-chip flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-all bg-slate-800/50 text-slate-400 hover:text-white border border-white/10" data-sort="mostViewed">
                <i data-lucide="eye" class="w-4 h-4"></i>조회순
              </button>
            </div>
          </div>

          <div id="emptyState" class="hidden bg-slate-900/50 backdrop-blur-xl rounded-3xl p-12 border border-white/10 text-center">
            <i data-lucide="message-square" class="w-16 h-16 text-slate-700 mx-auto mb-4"></i>
            <p class="text-slate-400 mb-6 text-lg">등록된 게시글이 없습니다.</p>
            <% if (isAuthenticated) { %>
            <button type="button" onclick="goPage('<%= contextPath %>/community/create')" class="px-8 py-4 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white rounded-xl transition-all shadow-lg shadow-sky-500/50 font-semibold">게시글 작성</button>
            <% } else { %>
            <button type="button" onclick="goPage('<%= contextPath %>/login')" class="px-8 py-4 bg-gradient-to-r from-sky-600 to-cyan-600 hover:from-sky-500 hover:to-cyan-500 text-white rounded-xl transition-all shadow-lg shadow-sky-500/50 font-semibold">로그인</button>
            <% } %>
          </div>

          <div id="loadingState" class="space-y-3" aria-live="polite" aria-label="게시글을 불러오는 중">
            <div class="rounded-2xl border border-white/10 bg-slate-900/50 p-6"><div class="flex gap-4"><div class="h-12 w-12 flex-shrink-0 rounded-full skeleton-shimmer"></div><div class="flex-1 space-y-3"><div class="h-4 w-24 rounded skeleton-shimmer"></div><div class="h-6 w-3/4 rounded skeleton-shimmer"></div><div class="h-4 w-full rounded skeleton-shimmer"></div><div class="h-4 w-2/3 rounded skeleton-shimmer"></div></div></div></div>
            <div class="rounded-2xl border border-white/10 bg-slate-900/50 p-6"><div class="flex gap-4"><div class="h-12 w-12 flex-shrink-0 rounded-full skeleton-shimmer"></div><div class="flex-1 space-y-3"><div class="h-4 w-20 rounded skeleton-shimmer"></div><div class="h-6 w-2/3 rounded skeleton-shimmer"></div><div class="h-4 w-full rounded skeleton-shimmer"></div><div class="h-4 w-1/2 rounded skeleton-shimmer"></div></div></div></div>
            <div class="rounded-2xl border border-white/10 bg-slate-900/50 p-6"><div class="flex gap-4"><div class="h-12 w-12 flex-shrink-0 rounded-full skeleton-shimmer"></div><div class="flex-1 space-y-3"><div class="h-4 w-28 rounded skeleton-shimmer"></div><div class="h-6 w-4/5 rounded skeleton-shimmer"></div><div class="h-4 w-full rounded skeleton-shimmer"></div><div class="h-4 w-3/5 rounded skeleton-shimmer"></div></div></div></div>
          </div>

          <div id="errorState" class="hidden rounded-3xl border border-red-500/20 bg-slate-900/50 p-10 text-center" role="alert">
            <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-red-500/10 text-red-300"><i data-lucide="wifi-off" class="h-7 w-7"></i></div>
            <h3 class="mb-2 text-lg font-semibold text-white">게시글을 불러오지 못했어요</h3>
            <p id="errorMessage" class="mb-6 text-sm text-slate-400">잠시 후 다시 시도해 주세요.</p>
            <button type="button" onclick="fetchAndRenderPosts()" class="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-slate-800 px-5 py-3 font-semibold text-white transition-colors hover:bg-slate-700"><i data-lucide="refresh-cw" class="h-4 w-4"></i>다시 시도</button>
          </div>

          <div id="postsWrap" class="hidden space-y-3"></div>
        </div>

        <aside class="space-y-6 lg:sticky lg:top-24 lg:self-start">
          <div id="trendingPanel" class="hidden rounded-3xl border border-white/10 bg-slate-900/50 p-5 backdrop-blur-xl reveal" style="--reveal-delay: 220ms;" data-reveal>
            <div class="mb-3 flex items-center justify-between">
              <div class="flex items-center gap-2"><i data-lucide="flame" class="h-5 w-5 text-orange-300"></i><h3 class="font-bold text-white">이번 주 인기글</h3></div>
              <span class="text-xs text-slate-500">TOP 5</span>
            </div>
            <div id="trendingPosts" class="divide-y divide-white/10"></div>
          </div>
          <div class="bg-slate-900/50 backdrop-blur-xl rounded-3xl p-6 border border-white/10 reveal card-hover-lift" style="--reveal-delay: 220ms;" data-reveal>
            <div id="topicBadge" class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-gradient-to-r from-sky-500 to-cyan-500 bg-opacity-10 mb-4">
              <i id="topicBadgeIcon" data-lucide="flame" class="w-5 h-5 text-white"></i>
              <span id="topicBadgeName" class="font-semibold text-white">전체</span>
            </div>
            <h3 id="topicDescTitle" class="text-xl font-bold text-white mb-2">모든 주제의 게시글</h3>
            <div class="space-y-3 pt-4 border-t border-white/10">
              <div class="flex items-center justify-between">
                <span class="text-sm text-slate-400">현재 게시글</span>
                <span id="postCount" class="text-lg font-bold text-white">0</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-slate-400">로그인 상태</span>
                <span class="text-sm font-medium text-sky-400"><%= isAuthenticated ? "로그인됨" : "비로그인" %></span>
              </div>
            </div>
          </div>
          <div class="rounded-3xl border border-white/10 bg-slate-900/50 p-6 backdrop-blur-xl reveal" style="--reveal-delay: 320ms;" data-reveal>
            <div class="mb-3 flex items-center gap-2 text-sky-300"><i data-lucide="shield-check" class="h-5 w-5"></i><h3 class="font-bold text-white">안전한 커뮤니티</h3></div>
            <p class="text-sm leading-6 text-slate-400">개인정보와 원본 증거는 게시하지 말아 주세요. 실제 피해 신고나 삭제 요청이 필요하다면 공개 게시글 대신 신고 안내를 이용하세요.</p>
            <a href="<%= contextPath %>/report" class="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-lg border border-sky-500/30 bg-sky-500/10 px-4 py-2.5 text-sm font-semibold text-sky-300 transition-colors hover:bg-sky-500/20">
              <i data-lucide="shield-alert" class="h-4 w-4"></i>
              <span>신고 안내 보기</span>
            </a>
          </div>
        </aside>
      </div>
    </div>
  </div>

  <script>
    const contextPath = "<%= contextPath %>";
    const isAuthenticated = <%= isAuthenticated ? "true" : "false" %>;
    const topics = {
      all: { name: "전체", icon: "flame", description: "모든 주제의 게시글", color: "from-sky-500 to-cyan-500", line: "bg-sky-400", text: "text-sky-300" },
      deepfake: { name: "딥페이크 감별", icon: "eye", description: "딥페이크 탐지와 검증", color: "from-purple-500 to-pink-500", line: "bg-fuchsia-400", text: "text-fuchsia-300" },
      "ai-tech": { name: "AI 기술", icon: "trending-up", description: "AI 이미지 생성 기술", color: "from-blue-500 to-indigo-500", line: "bg-blue-400", text: "text-blue-300" },
      news: { name: "뉴스 & 이슈", icon: "newspaper", description: "최신 뉴스와 이슈", color: "from-green-500 to-emerald-500", line: "bg-emerald-400", text: "text-emerald-300" },
      discussion: { name: "자유 토론", icon: "user", description: "자유로운 의견 교환", color: "from-orange-500 to-red-500", line: "bg-orange-400", text: "text-orange-300" }
    };
    let posts = [];
    let selectedTopic = "all";
    let sortBy = "recent";
    let searchQuery = "";

    function getTopicChipClass(topicKey, active) {
      const base = "topic-chip relative group p-4 rounded-2xl transition-all";
      const activeClasses = {
        all: "bg-gradient-to-br from-slate-800 to-slate-900 border-2 border-sky-500/50 shadow-lg shadow-sky-500/30",
        deepfake: "bg-gradient-to-br from-slate-800 to-slate-900 border-2 border-fuchsia-500/45 shadow-lg shadow-fuchsia-500/20",
        "ai-tech": "bg-gradient-to-br from-slate-800 to-slate-900 border-2 border-blue-500/45 shadow-lg shadow-blue-500/20",
        news: "bg-gradient-to-br from-slate-800 to-slate-900 border-2 border-emerald-500/45 shadow-lg shadow-emerald-500/20",
        discussion: "bg-gradient-to-br from-slate-800 to-slate-900 border-2 border-orange-500/45 shadow-lg shadow-orange-500/20"
      };
      return base + " " + (active ? (activeClasses[topicKey] || activeClasses.all) : "bg-slate-800/30 border border-white/10");
    }

    function goPage(path) { window.location.href = path; }
    function escapeHtml(value) {
      return String(value || "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&#39;");
    }
    function parseDate(value) {
      if (!value) return new Date(NaN);
      const normalized = String(value).replace(" ", "T");
      return new Date(normalized);
    }
    function formatRelativeDate(value) {
      const date = parseDate(value);
      if (Number.isNaN(date.getTime())) return "-";
      const diffSeconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000));
      if (diffSeconds < 60) return "방금 전";
      if (diffSeconds < 3600) return Math.floor(diffSeconds / 60) + "분 전";
      if (diffSeconds < 86400) return Math.floor(diffSeconds / 3600) + "시간 전";
      if (diffSeconds < 604800) return Math.floor(diffSeconds / 86400) + "일 전";
      return date.toLocaleDateString("ko-KR", { year: "numeric", month: "short", day: "numeric" });
    }
    function isNewPost(post) {
      const createdAt = parseDate(post.createdAt).getTime();
      return Number.isFinite(createdAt) && Date.now() - createdAt < 86400000;
    }
    function getCommentCount(post) { return Array.isArray(post.comments) ? post.comments.length : (post.commentCount || 0); }
    function getLikeCount(post) { return Number(post.likeCount || 0); }
    function getPopularity(post) { return getLikeCount(post) * 3 + getCommentCount(post) * 2 + Number(post.views || 0) * 0.05; }
    function getTopicMeta(topicKey) { return topics[topicKey] || topics.discussion; }
    function getAuthorInitial(author) {
      const value = String(author || "익명").trim();
      return escapeHtml(value.charAt(0).toUpperCase() || "익");
    }
    function getAvatarClass(author) {
      const palette = ["from-sky-500 to-blue-600", "from-fuchsia-500 to-purple-600", "from-emerald-500 to-teal-600", "from-orange-500 to-rose-600", "from-indigo-500 to-violet-600"];
      const hash = Array.from(String(author || "익명")).reduce(function (total, char) { return total + char.charCodeAt(0); }, 0);
      return palette[hash % palette.length];
    }
    function getPopularPosts(limit) {
      const weekAgo = Date.now() - 7 * 86400000;
      const sorted = posts.slice().sort(function (a, b) { return getPopularity(b) - getPopularity(a); });
      const recent = sorted.filter(function (post) { return parseDate(post.createdAt).getTime() >= weekAgo; });
      const selected = recent.slice(0, limit);
      if (selected.length < limit) {
        sorted.forEach(function (post) {
          if (selected.length < limit && !selected.some(function (item) { return String(item.id) === String(post.id); })) selected.push(post);
        });
      }
      return selected;
    }
    async function loadPosts() {
      const response = await fetch(contextPath + "/api/v1/community/posts");
      const json = await response.json().catch(function () { return null; });
      if (!response.ok || !json || !json.success || !Array.isArray(json.data)) throw new Error(json && json.error && json.error.message ? json.error.message : "게시글을 불러오지 못했습니다.");
      return json.data;
    }
    function applyTopicStyles() {
      document.querySelectorAll(".topic-chip").forEach(function (button) {
        const topicKey = button.dataset.topic || "all";
        const active = button.dataset.topic === selectedTopic;
        button.className = getTopicChipClass(topicKey, active);
      });
      const topic = topics[selectedTopic];
      document.getElementById("topicBadge").className = "inline-flex items-center gap-2 px-4 py-2 rounded-full bg-gradient-to-r " + topic.color + " bg-opacity-10 mb-4";
      document.getElementById("topicBadgeIcon").setAttribute("data-lucide", topic.icon);
      document.getElementById("topicBadgeName").textContent = topic.name;
      document.getElementById("topicDescTitle").textContent = topic.description;
      lucide.createIcons();
    }
    function applySortStyles() {
      document.querySelectorAll(".sort-chip").forEach(function (button) {
        const active = button.dataset.sort === sortBy;
        button.className = active
          ? "sort-chip flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-all bg-sky-500/20 text-sky-400 border border-sky-500/30"
          : "sort-chip flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-all bg-slate-800/50 text-slate-400 hover:text-white border border-white/10";
      });
    }
    function filteredPosts() {
      return posts.filter(function (post) {
        const matchesSearch = String(post.title || "").toLowerCase().includes(searchQuery) || String(post.content || "").toLowerCase().includes(searchQuery);
        const matchesTopic = selectedTopic === "all" || String(post.topic || "") === selectedTopic;
        return matchesSearch && matchesTopic;
      }).sort(function (a, b) {
        if (sortBy === "popular") return getPopularity(b) - getPopularity(a);
        if (sortBy === "mostViewed") return Number(b.views || 0) - Number(a.views || 0);
        return parseDate(b.createdAt).getTime() - parseDate(a.createdAt).getTime();
      });
    }
    async function togglePostLike(event, postId) {
      event.stopPropagation();
      if (!isAuthenticated) {
        goPage(contextPath + "/login");
        return;
      }
      try {
        const response = await fetch(contextPath + "/api/v1/community/posts/" + encodeURIComponent(postId) + "/like", {
          method: "POST"
        });
        const json = await response.json().catch(function () { return null; });
        if (!json || !json.success) {
          throw new Error(json && json.error && json.error.message ? json.error.message : "좋아요 처리에 실패했습니다.");
        }
        const target = posts.find(function (item) { return String(item.id) === String(postId); });
        if (target) {
          target.likedByCurrentUser = !!json.data.liked;
          target.likeCount = Number(json.data.likeCount || 0);
        }
        renderCommunity();
      } catch (error) {
        alert(error.message || "좋아요 처리에 실패했습니다.");
      }
    }

    function renderTrendingPosts() {
      const panel = document.getElementById("trendingPanel");
      const wrap = document.getElementById("trendingPosts");
      const trending = getPopularPosts(5);
      if (!trending.length) {
        panel.classList.add("hidden");
        wrap.innerHTML = "";
        return;
      }
      panel.classList.remove("hidden");
      wrap.innerHTML = trending.map(function (post, index) {
        return '<button type="button" onclick="goPage(\'' + contextPath + '/community/' + post.id + '\')" class="group flex w-full items-center gap-3 py-2.5 text-left first:pt-0 last:pb-0"><span class="w-5 flex-shrink-0 text-sm font-black ' + (index < 3 ? 'text-sky-400' : 'text-slate-600') + '">' + (index + 1) + '</span><span class="min-w-0 flex-1 truncate text-sm font-medium text-slate-300 transition-colors group-hover:text-white">' + escapeHtml(post.title) + '</span></button>';
      }).join("");
    }

    function renderPosts() {
      const list = filteredPosts();
      document.getElementById("postCount").textContent = String(list.length);
      const emptyState = document.getElementById("emptyState");
      const wrap = document.getElementById("postsWrap");
      if (!list.length) {
        wrap.innerHTML = "";
        wrap.classList.add("hidden");
        emptyState.classList.remove("hidden");
        lucide.createIcons();
        return;
      }
      emptyState.classList.add("hidden");
      wrap.classList.remove("hidden");
      const hotPostIds = new Set(getPopularPosts(3).map(function (post) { return String(post.id); }));
      wrap.innerHTML = list.map(function (post, index) {
        const liked = post.likedByCurrentUser === true;
        const topic = getTopicMeta(String(post.topic || "discussion"));
        const isHot = hotPostIds.has(String(post.id));
        return '' +
          '<div class="relative group reveal" data-reveal style="--reveal-delay:' + (Math.min(index || 0, 8) * 60) + 'ms;">' +
            '<div class="absolute inset-0 bg-gradient-to-r from-sky-600/0 to-cyan-600/0 group-hover:from-sky-600/10 group-hover:to-cyan-600/10 rounded-2xl transition-all duration-300"></div>' +
            '<div class="relative bg-slate-900/50 backdrop-blur-xl rounded-2xl p-6 border border-white/10 group-hover:border-sky-500/50 transition-all cursor-pointer card-hover-lift" onclick="goPage(\'' + contextPath + '/community/' + post.id + '\')">' +
              '<div class="post-topic-line ' + topic.line + '"></div>' +
              '<div class="flex gap-4">' +
                '<div class="flex-shrink-0"><div class="w-12 h-12 bg-gradient-to-br ' + getAvatarClass(post.author) + ' rounded-full flex items-center justify-center text-sm font-bold text-white shadow-lg shadow-black/20">' + getAuthorInitial(post.author) + '</div></div>' +
                '<div class="flex-1 min-w-0">' +
                  '<div class="flex items-start justify-between gap-4 mb-2">' +
                    '<div><div class="mb-2 flex flex-wrap items-center gap-2"><span class="inline-flex items-center gap-1.5 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs font-medium ' + topic.text + '"><i data-lucide="' + topic.icon + '" class="h-3.5 w-3.5"></i><span>' + escapeHtml(topic.name) + '</span></span>' +
                    (isNewPost(post) ? '<span class="rounded-full border border-sky-400/20 bg-sky-400/10 px-2 py-1 text-[10px] font-bold tracking-wide text-sky-300">NEW</span>' : '') +
                    (isHot ? '<span class="rounded-full border border-orange-400/20 bg-orange-400/10 px-2 py-1 text-[10px] font-bold tracking-wide text-orange-200">HOT</span>' : '') +
                    '</div><h3 class="text-lg font-semibold text-white group-hover:text-sky-400 transition-colors mb-1">' + escapeHtml(post.title) + '</h3>' +
                    '<div class="flex items-center gap-2 text-sm text-slate-500"><span class="font-medium text-slate-400">' + escapeHtml(post.author || "익명") + '</span><span>&bull;</span><time datetime="' + escapeHtml(post.createdAt || "") + '" title="' + escapeHtml(post.createdAt || "") + '">' + formatRelativeDate(post.createdAt) + '</time></div></div>' +
                  '</div>' +
                  '<p class="text-slate-400 mb-4 line-clamp-2 leading-relaxed">' + escapeHtml(post.content) + '</p>' +
                  '<div class="flex items-center gap-4 text-sm">' +
                    '<button type="button" onclick="togglePostLike(event, \'' + post.id + '\')" class="flex items-center gap-1.5 transition-colors ' + (liked ? 'text-sky-400' : 'text-slate-500 hover:text-sky-400') + '"><i data-lucide="thumbs-up" class="w-4 h-4"></i><span class="font-medium">' + getLikeCount(post) + '</span></button>' +
                    '<div class="flex items-center gap-1.5 text-slate-500 hover:text-sky-400 transition-colors"><i data-lucide="message-square" class="w-4 h-4"></i><span class="font-medium">' + getCommentCount(post) + '</span></div>' +
                    '<div class="flex items-center gap-1.5 text-slate-500 hover:text-sky-400 transition-colors"><i data-lucide="eye" class="w-4 h-4"></i><span class="font-medium">' + Number(post.views || 0) + '</span></div>' +
                  '</div>' +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>';
      }).join("");
      initReveal(wrap);
      lucide.createIcons();
    }

    function renderCommunity() {
      renderTrendingPosts();
      renderPosts();
      lucide.createIcons();
    }

    function initReveal(scope) {
      const items = Array.from((scope || document).querySelectorAll("[data-reveal]"));
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
          if (!item.classList.contains("reveal-visible")) {
            observer.observe(item);
          }
        });
      } else {
        items.forEach(function (item) {
          item.classList.add("reveal-visible");
        });
      }
    }

    document.getElementById("searchInput").addEventListener("input", function (event) {
      searchQuery = String(event.target.value || "").toLowerCase();
      renderPosts();
    });
    document.querySelectorAll(".topic-chip").forEach(function (button) {
      button.addEventListener("click", function () { selectedTopic = button.dataset.topic || "all"; applyTopicStyles(); renderPosts(); });
    });
    document.querySelectorAll(".sort-chip").forEach(function (button) {
      button.addEventListener("click", function () { sortBy = button.dataset.sort || "recent"; applySortStyles(); renderPosts(); });
    });

    async function fetchAndRenderPosts() {
      const loadingState = document.getElementById("loadingState");
      const errorState = document.getElementById("errorState");
      const postsWrap = document.getElementById("postsWrap");
      const emptyState = document.getElementById("emptyState");
      loadingState.classList.remove("hidden");
      errorState.classList.add("hidden");
      postsWrap.classList.add("hidden");
      emptyState.classList.add("hidden");
      document.getElementById("trendingPanel").classList.add("hidden");
      try {
        posts = await loadPosts();
        loadingState.classList.add("hidden");
        applyTopicStyles();
        applySortStyles();
        renderCommunity();
      } catch (error) {
        posts = [];
        loadingState.classList.add("hidden");
        errorState.classList.remove("hidden");
        document.getElementById("postCount").textContent = "-";
        document.getElementById("errorMessage").textContent = error && error.message ? error.message : "잠시 후 다시 시도해 주세요.";
        lucide.createIcons();
      }
    }

    initReveal(document);
    lucide.createIcons();
    fetchAndRenderPosts();
  </script>
</body>
</html>

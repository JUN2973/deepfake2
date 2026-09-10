<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
  체크리스트 기준 주석: 구현(커뮤니티): 게시글 상세, 댓글 작성/수정/삭제 화면을 구성한다.
--%>

<%--
  발표용 설명: 커뮤니티 상세 화면입니다.
  게시글 상세, 댓글 목록, 대댓글, 좋아요, 수정/삭제 버튼을 사용자 권한에 맞춰 보여줍니다.
--%>
<%
  String contextPath = request.getContextPath();
  Object userIdObj = session.getAttribute("USER_ID");
  boolean isAuthenticated = (userIdObj != null);
  Long sessionUserId = null;
  if (userIdObj != null) {
    try {
      sessionUserId = Long.valueOf(String.valueOf(userIdObj));
    } catch (Exception ignored) {
      sessionUserId = null;
    }
  }
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>게시글 상세 - DeepScan</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
  <style>
    body {
      font-family: "Pretendard", "Noto Sans KR", sans-serif;
      background: #020617;
    }
    .glass-card {
      background: rgba(15, 23, 42, 0.52);
      backdrop-filter: blur(20px);
      border: 1px solid rgba(255, 255, 255, 0.08);
      box-shadow: 0 28px 80px rgba(2, 6, 23, 0.55);
    }
    .hero-glow {
      pointer-events: none;
      position: absolute;
      inset: 0;
      overflow: hidden;
    }
    .hero-glow::before,
    .hero-glow::after {
      content: "";
      position: absolute;
      width: 26rem;
      height: 26rem;
      border-radius: 9999px;
      filter: blur(96px);
      opacity: 0.18;
    }
    .hero-glow::before {
      top: 5rem;
      left: 12%;
      background: linear-gradient(135deg, #0284c7, #06b6d4);
    }
    .hero-glow::after {
      right: 12%;
      bottom: 4rem;
      background: linear-gradient(135deg, #0891b2, #2563eb);
    }
    .loading-dot {
      animation: blink 1.1s infinite;
    }
    @keyframes blink {
      0%, 100% { opacity: 0.3; }
      50% { opacity: 1; }
    }
  </style>
</head>
<body class="min-h-screen bg-slate-950 text-white relative overflow-x-hidden">
  <div class="hero-glow"></div>

  <button
    type="button"
    onclick="goPage('<%= contextPath %>/community')"
    class="fixed top-6 left-6 z-50 flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm font-medium backdrop-blur-xl transition-all hover:bg-white/20"
  >
    <i data-lucide="arrow-left" class="h-4 w-4"></i>
    목록으로
  </button>

  <div class="pt-24 pb-12 px-4 relative">
    <div class="max-w-4xl mx-auto">
      <div id="loadingBox" class="flex min-h-[40vh] items-center justify-center">
        <div class="text-center text-slate-400">
          <div class="mb-4 flex justify-center gap-2">
            <span class="loading-dot h-2.5 w-2.5 rounded-full bg-sky-400"></span>
            <span class="loading-dot h-2.5 w-2.5 rounded-full bg-cyan-400" style="animation-delay:0.15s;"></span>
            <span class="loading-dot h-2.5 w-2.5 rounded-full bg-blue-400" style="animation-delay:0.3s;"></span>
          </div>
          <p class="text-sm tracking-[0.18em] uppercase">Loading Post</p>
        </div>
      </div>

      <div id="postWrap" class="hidden space-y-6">
        <section class="relative group">
          <div class="absolute inset-0 rounded-[2rem] bg-gradient-to-r from-sky-600/20 to-cyan-600/20 blur-2xl opacity-70"></div>
          <div class="relative glass-card rounded-[2rem] p-8 md:p-10">
            <div class="mb-5 flex flex-wrap items-start justify-between gap-4">
              <div class="flex flex-1 items-start gap-4">
                <h1 id="postTitle" class="max-w-3xl flex-1 text-3xl font-bold tracking-tight text-white md:text-4xl"></h1>
                <div id="postActionButtons" class="hidden items-center gap-2">
                  <button type="button" onclick="startPostEdit()" class="rounded-lg p-2 text-sky-400 transition-colors hover:bg-sky-500/10 hover:text-sky-300" title="수정">
                    <i data-lucide="pencil" class="h-5 w-5"></i>
                  </button>
                  <button type="button" onclick="openDeleteModal()" class="rounded-lg p-2 text-red-400 transition-colors hover:bg-red-500/10 hover:text-red-300" title="삭제">
                    <i data-lucide="trash-2" class="h-5 w-5"></i>
                  </button>
                </div>
              </div>
              <div id="authorBadge" class="hidden rounded-full border border-sky-400/30 bg-sky-500/10 px-3 py-1 text-xs font-semibold text-sky-300">
                내 게시글
              </div>
            </div>

            <div class="mb-8 flex flex-wrap items-center gap-x-5 gap-y-3 border-b border-white/10 pb-6 text-sm text-slate-400">
              <div class="flex items-center gap-2">
                <i data-lucide="user" class="h-4 w-4"></i>
                <span id="postAuthor"></span>
              </div>
              <div class="flex items-center gap-2">
                <i data-lucide="eye" class="h-4 w-4"></i>
                <span id="postViews"></span>
              </div>
              <div class="flex items-center gap-2">
                <i data-lucide="message-square" class="h-4 w-4"></i>
                <span id="postCommentCount"></span>
              </div>
              <div class="flex items-center gap-2">
                <i data-lucide="clock-3" class="h-4 w-4"></i>
                <span id="postCreatedAt"></span>
              </div>
            </div>

            <div id="postViewMode">
              <div id="postContent" class="whitespace-pre-wrap text-[15px] leading-8 text-slate-300 md:text-base"></div>
            </div>

            <div id="postEditMode" class="hidden space-y-4">
              <input id="editPostTitle" type="text" class="w-full rounded-2xl border border-white/10 bg-slate-800/50 px-4 py-3 text-2xl font-bold text-white outline-none focus:border-sky-500 focus:ring-2 focus:ring-sky-500/40 md:text-3xl">
              <textarea id="editPostContent" rows="10" class="w-full resize-none rounded-2xl border border-white/10 bg-slate-800/50 px-4 py-3 text-base leading-8 text-slate-200 outline-none focus:border-sky-500 focus:ring-2 focus:ring-sky-500/40"></textarea>
              <div class="flex justify-end gap-2">
                <button type="button" onclick="cancelPostEdit()" class="rounded-xl border border-white/15 bg-white/5 px-4 py-2 text-white transition-colors hover:bg-white/10">취소</button>
                <button type="button" onclick="savePostEdit()" class="rounded-xl bg-gradient-to-r from-sky-600 to-cyan-600 px-4 py-2 font-medium text-white transition hover:from-sky-500 hover:to-cyan-500">저장</button>
              </div>
            </div>
          </div>
        </section>

        <section class="relative group">
          <div class="absolute inset-0 rounded-[2rem] bg-gradient-to-r from-sky-600/20 to-cyan-600/20 blur-2xl opacity-70"></div>
          <div class="relative glass-card rounded-[2rem] p-8 md:p-10">
            <div class="mb-6 flex items-center justify-between gap-4">
              <h2 class="text-xl font-bold text-white md:text-2xl">
                댓글 <span id="commentTitleCount">0</span>개
              </h2>
            </div>

            <% if (isAuthenticated) { %>
            <form id="commentForm" onsubmit="submitRootComment(event)" class="mb-8">
              <div class="flex items-start gap-3">
                <div class="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-sky-500 to-cyan-500 shadow-lg shadow-sky-500/30">
                  <i data-lucide="user" class="h-5 w-5 text-white"></i>
                </div>
                <div class="flex-1">
                  <div class="flex flex-col gap-3 md:flex-row">
                    <input
                      id="commentInput"
                      type="text"
                      placeholder="댓글을 입력해 주세요."
                      class="w-full flex-1 rounded-2xl border border-white/10 bg-slate-800/50 px-4 py-3 text-white placeholder-slate-500 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-500/40"
                    >
                    <button
                      id="commentSubmitButton"
                      type="submit"
                      class="rounded-xl bg-gradient-to-r from-sky-600 to-cyan-600 px-6 py-3 font-medium text-white shadow-lg shadow-sky-500/30 transition hover:from-sky-500 hover:to-cyan-500 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      댓글 게시
                    </button>
                  </div>
                </div>
              </div>
            </form>
            <% } else { %>
            <div class="mb-8 rounded-2xl border border-sky-500/30 bg-sky-500/10 p-6 text-center">
              <p class="mb-3 text-slate-300">댓글을 작성하려면 로그인이 필요합니다.</p>
              <button
                type="button"
                onclick="goPage('<%= contextPath %>/login')"
                class="rounded-xl bg-gradient-to-r from-sky-600 to-cyan-600 px-6 py-2.5 font-medium text-white shadow-lg shadow-sky-500/30 transition hover:from-sky-500 hover:to-cyan-500"
              >
                로그인
              </button>
            </div>
            <% } %>

            <div id="commentErrorBox" class="hidden mb-5 rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300"></div>
            <div id="commentsWrap" class="space-y-4">
              <div class="rounded-2xl border border-white/8 bg-slate-900/35 px-6 py-10 text-center text-slate-500">
                <i data-lucide="message-square" class="mx-auto mb-3 h-10 w-10 text-slate-700"></i>
                <p>첫 댓글을 남겨보세요.</p>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>

  <div id="deleteModal" class="hidden fixed inset-0 z-50 items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/70 backdrop-blur-sm" onclick="closeDeleteModal()"></div>
    <div class="relative w-full max-w-sm rounded-[1.75rem] border border-white/10 bg-slate-900 p-6 shadow-2xl">
      <h3 class="mb-3 text-xl font-bold text-white">게시글 삭제</h3>
      <p class="mb-6 text-slate-300">이 게시글을 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다.</p>
      <div class="flex justify-end gap-3">
        <button type="button" onclick="closeDeleteModal()" class="rounded-xl border border-white/15 bg-white/5 px-4 py-2 text-white transition-colors hover:bg-white/10">취소</button>
        <button type="button" onclick="confirmDeletePost()" class="rounded-xl bg-red-600 px-4 py-2 font-medium text-white transition-colors hover:bg-red-700">삭제</button>
      </div>
    </div>
  </div>

  <script>
    const contextPath = "<%= contextPath %>";
    const isAuthenticated = <%= isAuthenticated ? "true" : "false" %>;
    const sessionUserId = <%= sessionUserId == null ? "null" : sessionUserId.toString() %>;
    const commentUiState = {
      replyTargetId: null,
      replyDraft: "",
      expandedReplyIds: new Set(),
      editTargetId: null,
      editDraft: ""
    };
    let currentPost = null;
    let currentComments = [];
    let postUiState = { isEditing: false };

    function goPage(path) { window.location.href = path; }

    function escapeHtml(value) {
      return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
    }

    function formatDate(value) {
      try {
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? (value || "-") : date.toLocaleString("ko-KR");
      } catch (error) {
        return value || "-";
      }
    }

    function getTimeAgo(value) {
      const timestamp = new Date(value).getTime();
      if (Number.isNaN(timestamp)) return value || "";
      const diff = Date.now() - timestamp;
      const seconds = Math.floor(diff / 1000);
      const minutes = Math.floor(seconds / 60);
      const hours = Math.floor(minutes / 60);
      const days = Math.floor(hours / 24);
      const weeks = Math.floor(days / 7);
      if (weeks > 0) return weeks + "주 전";
      if (days > 0) return days + "일 전";
      if (hours > 0) return hours + "시간 전";
      if (minutes > 0) return minutes + "분 전";
      return "방금 전";
    }

    function getPostIdFromUrl() {
      const parts = window.location.pathname.split("/");
      return parts[parts.length - 1];
    }

    function showCommentError(message) {
      const errorBox = document.getElementById("commentErrorBox");
      if (!message) {
        errorBox.classList.add("hidden");
        errorBox.textContent = "";
        return;
      }
      errorBox.textContent = message;
      errorBox.classList.remove("hidden");
    }

    function requireLogin() {
      goPage(contextPath + "/login");
    }

    function setPostEditMode(isEditing) {
      postUiState.isEditing = isEditing;
      document.getElementById("postViewMode").classList.toggle("hidden", isEditing);
      document.getElementById("postEditMode").classList.toggle("hidden", !isEditing);
    }

    function countAllComments(comments) {
      return comments.reduce(function (sum, comment) {
        return sum + 1 + countAllComments(comment.replies || []);
      }, 0);
    }

    function findCommentById(comments, commentId) {
      for (const comment of comments) {
        if (String(comment.id) === String(commentId)) return comment;
        const child = findCommentById(comment.replies || [], commentId);
        if (child) return child;
      }
      return null;
    }

    async function loadPost(postId) {
      const response = await fetch(contextPath + "/api/v1/community/posts/" + encodeURIComponent(postId));
      const json = await response.json().catch(function () { return null; });
      if (!json || !json.success || !json.data) throw new Error("게시글을 불러오지 못했습니다.");
      return json.data;
    }

    async function loadComments(postId) {
      const response = await fetch(contextPath + "/api/v1/community/posts/" + encodeURIComponent(postId) + "/comments");
      const json = await response.json().catch(function () { return null; });
      if (!json || !json.success || !Array.isArray(json.data)) throw new Error("댓글을 불러오지 못했습니다.");
      return json.data;
    }

    function renderPost(post) {
      currentPost = post;
      document.getElementById("postTitle").textContent = post.title || "";
      document.getElementById("postAuthor").textContent = post.author || "익명";
      document.getElementById("postViews").textContent = String(post.views || 0);
      document.getElementById("postCreatedAt").textContent = formatDate(post.createdAt);
      document.getElementById("postContent").textContent = post.content || "";
      const authorBadge = document.getElementById("authorBadge");
      const postActionButtons = document.getElementById("postActionButtons");
      if (sessionUserId !== null && String(post.userId) === String(sessionUserId)) {
        authorBadge.classList.remove("hidden");
        postActionButtons.classList.remove("hidden");
        postActionButtons.classList.add("flex");
      } else {
        authorBadge.classList.add("hidden");
        postActionButtons.classList.add("hidden");
        postActionButtons.classList.remove("flex");
      }
      document.getElementById("loadingBox").classList.add("hidden");
      document.getElementById("postWrap").classList.remove("hidden");
      setPostEditMode(false);
      lucide.createIcons();
    }

    function startPostEdit() {
      if (!currentPost) return;
      document.getElementById("editPostTitle").value = currentPost.title || "";
      document.getElementById("editPostContent").value = currentPost.content || "";
      setPostEditMode(true);
    }

    function cancelPostEdit() {
      setPostEditMode(false);
    }

    async function savePostEdit() {
      const title = document.getElementById("editPostTitle").value.trim();
      const content = document.getElementById("editPostContent").value.trim();
      if (!title) return showCommentError("게시글 제목을 입력해 주세요.");
      if (!content) return showCommentError("게시글 내용을 입력해 주세요.");
      showCommentError("");
      try {
        const response = await fetch(contextPath + "/api/v1/community/posts/" + encodeURIComponent(postId), {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ topic: currentPost.topic || "discussion", title: title, content: content })
        });
        const json = await response.json().catch(function () { return null; });
        if (!json || !json.success) throw new Error(json && json.error && json.error.message ? json.error.message : "게시글 수정에 실패했습니다.");
        currentPost.title = title;
        currentPost.content = content;
        renderPost(currentPost);
      } catch (error) {
        showCommentError(error.message || "게시글 수정에 실패했습니다.");
      }
    }

    function openDeleteModal() {
      document.getElementById("deleteModal").classList.remove("hidden");
      document.getElementById("deleteModal").classList.add("flex");
    }

    function closeDeleteModal() {
      document.getElementById("deleteModal").classList.add("hidden");
      document.getElementById("deleteModal").classList.remove("flex");
    }

    async function confirmDeletePost() {
      closeDeleteModal();
      showCommentError("");
      try {
        const response = await fetch(contextPath + "/api/v1/community/posts/" + encodeURIComponent(postId), {
          method: "DELETE"
        });
        const json = await response.json().catch(function () { return null; });
        if (!json || !json.success) throw new Error(json && json.error && json.error.message ? json.error.message : "게시글 삭제에 실패했습니다.");
        goPage(contextPath + "/community");
      } catch (error) {
        showCommentError(error.message || "게시글 삭제에 실패했습니다.");
      }
    }

    function renderSingleComment(comment, depth) {
      const isAuthor = sessionUserId !== null && String(comment.userId) === String(sessionUserId);
      const isEditing = String(commentUiState.editTargetId) === String(comment.id);
      const isReplyTarget = String(commentUiState.replyTargetId) === String(comment.id);
      const replies = comment.replies || [];
      const repliesCount = replies.length;
      const showReplies = commentUiState.expandedReplyIds.has(String(comment.id));
      const canReply = depth < 1;
      const likeCount = Number(comment.likeCount || 0);
      const liked = comment.likedByCurrentUser === true;

      return '' +
        '<div class="' + (depth > 0 ? 'ml-8 md:ml-12 ' : '') + '">' +
          '<div class="group flex gap-3">' +
            '<div class="flex-shrink-0">' +
              '<div class="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-sky-500 to-cyan-500 shadow-md">' +
                '<i data-lucide="user" class="h-4 w-4 text-white"></i>' +
              '</div>' +
            '</div>' +
            '<div class="min-w-0 flex-1">' +
              (isEditing
                ? '<div class="space-y-2">' +
                    '<input type="text" value="' + escapeHtml(commentUiState.editDraft) + '" oninput="commentUiState.editDraft = this.value" onkeydown="handleEditKeydown(event, \'' + comment.id + '\')" class="w-full rounded-lg border border-white/10 bg-slate-800/50 px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-sky-500">' +
                    '<div class="flex gap-2">' +
                      '<button type="button" onclick="cancelCommentEdit()" class="px-3 py-1 text-xs text-slate-400 transition-colors hover:text-white">취소</button>' +
                      '<button type="button" onclick="saveCommentEdit(\'' + comment.id + '\')" class="px-3 py-1 text-xs font-medium text-sky-400 transition-colors hover:text-sky-300">저장</button>' +
                    '</div>' +
                  '</div>'
                : '<div class="inline-block max-w-full rounded-2xl bg-slate-800/30 px-4 py-2.5">' +
                    '<div class="mb-0.5 flex items-baseline gap-2">' +
                      '<span class="text-sm font-semibold text-white">' + escapeHtml(comment.author || '익명') + '</span>' +
                    '</div>' +
                    '<p class="break-words text-sm text-slate-200">' + escapeHtml(comment.content) + '</p>' +
                  '</div>' +
                  '<div class="mt-1.5 flex flex-wrap items-center gap-4 px-1">' +
                    '<span class="text-xs text-slate-500">' + escapeHtml(getTimeAgo(comment.createdAt)) + '</span>' +
                    (likeCount > 0 ? '<button type="button" onclick="toggleCommentLike(\'' + comment.id + '\')" class="text-xs font-medium text-slate-500 transition-colors hover:text-white">좋아요 ' + likeCount + '개</button>' : '') +
                    (canReply ? '<button type="button" onclick="toggleReplyInput(\'' + comment.id + '\')" class="text-xs font-medium text-slate-500 transition-colors hover:text-white">답글 달기</button>' : '') +
                    (isAuthor ? '<div class="flex items-center gap-2 opacity-0 transition-opacity group-hover:opacity-100"><button type="button" onclick="startCommentEdit(\'' + comment.id + '\')" class="text-xs text-sky-400 transition-colors hover:text-sky-300">수정</button><button type="button" onclick="deleteComment(\'' + comment.id + '\')" class="text-xs text-red-400 transition-colors hover:text-red-300">삭제</button></div>' : '') +
                  '</div>'
              ) +
              (isReplyTarget
                ? '<div class="mt-2"><div class="flex items-center gap-2"><input type="text" value="' + escapeHtml(commentUiState.replyDraft) + '" oninput="commentUiState.replyDraft = this.value" onkeydown="handleReplyKeydown(event, \'' + comment.id + '\')" placeholder="@' + escapeHtml(comment.author) + '에게 답글..." class="flex-1 rounded-lg border border-white/10 bg-slate-800/50 px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500"><button type="button" onclick="submitReply(\'' + comment.id + '\')" class="rounded-lg bg-sky-500 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-sky-600">게시</button></div></div>'
                : '') +
              (repliesCount > 0
                ? '<button type="button" onclick="toggleReplies(\'' + comment.id + '\')" class="mt-3 flex items-center gap-2 text-xs font-medium text-slate-500 transition-colors hover:text-white"><div class="h-px w-6 bg-slate-700"></div>' + (showReplies ? '답글 숨기기' : '답글 ' + repliesCount + '개 보기') + '</button>'
                : '') +
              (showReplies && repliesCount > 0 ? '<div class="mt-3 space-y-3">' + replies.map(function (reply) { return renderSingleComment(reply, depth + 1); }).join("") + '</div>' : '') +
            '</div>' +
            '<div class="flex-shrink-0 pt-2"><button type="button" onclick="toggleCommentLike(\'' + comment.id + '\')" class="transition-transform hover:scale-110">' +
              (liked ? '<i data-lucide="heart" class="h-3.5 w-3.5 fill-red-500 text-red-500"></i>' : '<i data-lucide="heart" class="h-3.5 w-3.5 text-slate-500 transition-colors hover:text-red-400"></i>') +
            '</button></div>' +
          '</div>' +
        '</div>';
    }

    function renderComments(comments) {
      currentComments = comments;
      const totalCount = countAllComments(comments);
      const wrap = document.getElementById("commentsWrap");
      if (!comments.length) {
        wrap.innerHTML = '<div class="rounded-2xl border border-white/8 bg-slate-900/35 px-6 py-10 text-center text-slate-500"><i data-lucide="message-square" class="mx-auto mb-3 h-10 w-10 text-slate-700"></i><p>첫 댓글을 남겨보세요.</p></div>';
      } else {
        wrap.innerHTML = comments.map(function (comment) { return renderSingleComment(comment, 0); }).join("");
      }
      document.getElementById("postCommentCount").textContent = String(totalCount);
      document.getElementById("commentTitleCount").textContent = String(totalCount);
      lucide.createIcons();
    }

    async function refreshComments() {
      currentComments = await loadComments(postId);
      renderComments(currentComments);
    }

    function toggleReplyInput(commentId) {
      if (!isAuthenticated) return requireLogin();
      if (String(commentUiState.replyTargetId) === String(commentId)) {
        commentUiState.replyTargetId = null;
        commentUiState.replyDraft = "";
      } else {
        commentUiState.replyTargetId = String(commentId);
        commentUiState.replyDraft = "";
      }
      renderComments(currentComments);
    }

    function toggleReplies(commentId) {
      const key = String(commentId);
      if (commentUiState.expandedReplyIds.has(key)) commentUiState.expandedReplyIds.delete(key);
      else commentUiState.expandedReplyIds.add(key);
      renderComments(currentComments);
    }

    function startCommentEdit(commentId) {
      const comment = findCommentById(currentComments, commentId);
      if (!comment) return;
      commentUiState.editTargetId = String(commentId);
      commentUiState.editDraft = comment.content || "";
      renderComments(currentComments);
    }

    function cancelCommentEdit() {
      commentUiState.editTargetId = null;
      commentUiState.editDraft = "";
      renderComments(currentComments);
    }

    function handleReplyKeydown(event, commentId) {
      if (event.key === "Enter") {
        event.preventDefault();
        submitReply(commentId);
      }
      if (event.key === "Escape") {
        commentUiState.replyTargetId = null;
        commentUiState.replyDraft = "";
        renderComments(currentComments);
      }
    }

    function handleEditKeydown(event, commentId) {
      if (event.key === "Enter") {
        event.preventDefault();
        saveCommentEdit(commentId);
      }
      if (event.key === "Escape") cancelCommentEdit();
    }

    async function submitRootComment(event) {
      event.preventDefault();
      const input = document.getElementById("commentInput");
      const submitButton = document.getElementById("commentSubmitButton");
      const value = input.value.trim();
      if (!value) return showCommentError("댓글 내용을 입력해 주세요.");
      submitButton.disabled = true;
      showCommentError("");
      try {
        await sendCommentCreate(value, null);
        input.value = "";
        await refreshComments();
      } catch (error) {
        showCommentError(error.message || "댓글 작성에 실패했습니다.");
      } finally {
        submitButton.disabled = false;
      }
    }

    async function submitReply(commentId) {
      if (!commentUiState.replyDraft.trim()) return showCommentError("답글 내용을 입력해 주세요.");
      showCommentError("");
      try {
        await sendCommentCreate(commentUiState.replyDraft.trim(), commentId);
        commentUiState.replyTargetId = null;
        commentUiState.replyDraft = "";
        commentUiState.expandedReplyIds.add(String(commentId));
        await refreshComments();
      } catch (error) {
        showCommentError(error.message || "답글 작성에 실패했습니다.");
      }
    }

    async function sendCommentCreate(content, parentId) {
      const response = await fetch(contextPath + "/api/v1/community/posts/" + encodeURIComponent(postId) + "/comments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ content: content, parentId: parentId ? Number(parentId) : null })
      });
      const json = await response.json().catch(function () { return null; });
      if (!json || !json.success) throw new Error(json && json.error && json.error.message ? json.error.message : "댓글 작성에 실패했습니다.");
      return json.data;
    }

    async function saveCommentEdit(commentId) {
      const value = commentUiState.editDraft.trim();
      if (!value) return showCommentError("댓글 내용을 입력해 주세요.");
      showCommentError("");
      try {
        const response = await fetch(contextPath + "/api/v1/community/posts/" + encodeURIComponent(postId) + "/comments/" + encodeURIComponent(commentId), {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ content: value })
        });
        const json = await response.json().catch(function () { return null; });
        if (!json || !json.success) throw new Error(json && json.error && json.error.message ? json.error.message : "댓글 수정에 실패했습니다.");
        commentUiState.editTargetId = null;
        commentUiState.editDraft = "";
        await refreshComments();
      } catch (error) {
        showCommentError(error.message || "댓글 수정에 실패했습니다.");
      }
    }

    async function deleteComment(commentId) {
      if (!window.confirm("댓글을 삭제하시겠습니까?")) return;
      showCommentError("");
      try {
        const response = await fetch(contextPath + "/api/v1/community/posts/" + encodeURIComponent(postId) + "/comments/" + encodeURIComponent(commentId), {
          method: "DELETE"
        });
        const json = await response.json().catch(function () { return null; });
        if (!json || !json.success) throw new Error(json && json.error && json.error.message ? json.error.message : "댓글 삭제에 실패했습니다.");
        if (String(commentUiState.replyTargetId) === String(commentId)) {
          commentUiState.replyTargetId = null;
          commentUiState.replyDraft = "";
        }
        if (String(commentUiState.editTargetId) === String(commentId)) {
          commentUiState.editTargetId = null;
          commentUiState.editDraft = "";
        }
        commentUiState.expandedReplyIds.delete(String(commentId));
        await refreshComments();
      } catch (error) {
        showCommentError(error.message || "댓글 삭제에 실패했습니다.");
      }
    }

    async function toggleCommentLike(commentId) {
      if (!isAuthenticated) return requireLogin();
      showCommentError("");
      try {
        const response = await fetch(contextPath + "/api/v1/community/posts/" + encodeURIComponent(postId) + "/comments/" + encodeURIComponent(commentId) + "/like", {
          method: "POST"
        });
        const json = await response.json().catch(function () { return null; });
        if (!json || !json.success) throw new Error(json && json.error && json.error.message ? json.error.message : "댓글 좋아요 처리에 실패했습니다.");
        await refreshComments();
      } catch (error) {
        showCommentError(error.message || "댓글 좋아요 처리에 실패했습니다.");
      }
    }

    const postId = getPostIdFromUrl();
    if (!postId) {
      goPage(contextPath + "/community");
    } else {
      Promise.all([loadPost(postId), loadComments(postId)]).then(function (results) {
        renderPost(results[0]);
        renderComments(results[1]);
      }).catch(function () {
        goPage(contextPath + "/community");
      });
    }

    lucide.createIcons();
  </script>
</body>
</html>

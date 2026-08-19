<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  체크리스트 기준 주석: 구현(뉴스): 뉴스 상세 보기와 관련 링크 이동 화면을 구성한다.
--%>
<%
  String contextPath = request.getContextPath();
  request.setAttribute("activePage", "news");
  Object newsBookmarkUserIdObj = session.getAttribute("USER_ID");
  String newsBookmarkOwner = newsBookmarkUserIdObj == null ? "" : String.valueOf(newsBookmarkUserIdObj);
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>뉴스 상세 - DeepScan</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard/dist/web/static/pretendard.css">
  <style>
    body {
      font-family: "Pretendard", sans-serif;
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
      transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
    }
    .card-hover-lift:hover {
      transform: translateY(-6px);
      box-shadow: 0 24px 60px rgba(14, 165, 233, 0.14);
    }
    .detail-hero {
      position: relative;
      overflow: hidden;
      background: linear-gradient(135deg, #18344f 0%, #0f2740 35%, #0e1b35 70%, #0a5b7a 100%);
    }
    .detail-hero::before {
      content: "";
      position: absolute;
      inset: 0;
      background:
        radial-gradient(circle at 18% 20%, rgba(125, 211, 252, 0.16), transparent 32%),
        radial-gradient(circle at 85% 78%, rgba(34, 211, 238, 0.14), transparent 28%);
      pointer-events: none;
    }
    .detail-hero-icon {
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08), 0 18px 40px rgba(2, 6, 23, 0.28);
    }
  </style>
</head>
<body class="relative min-h-screen overflow-x-hidden bg-slate-950 text-white" data-news-id="<c:out value='${news.id}'/>">
  <c:choose>
    <c:when test="${empty news}">
      <div class="flex min-h-screen items-center justify-center bg-slate-950 px-4 text-white">
        <div class="text-center">
          <i data-lucide="alert-triangle" class="mx-auto mb-4 h-16 w-16 text-slate-600"></i>
          <p class="mb-4 text-lg text-slate-400">뉴스를 찾을 수 없습니다.</p>
          <button
            type="button"
            onclick="window.location.href='<%= contextPath %>/news'"
            class="rounded-xl bg-gradient-to-r from-sky-600 to-cyan-600 px-6 py-3 font-semibold text-white shadow-lg shadow-sky-500/50 transition-all hover:from-sky-500 hover:to-cyan-500"
          >
            뉴스 목록으로
          </button>
        </div>
      </div>
    </c:when>
    <c:otherwise>
      <div class="absolute inset-0 -z-10 overflow-hidden">
        <div class="absolute left-1/4 top-20 h-96 w-96 rounded-full bg-sky-600/20 blur-3xl"></div>
        <div class="absolute bottom-20 right-1/4 h-96 w-96 rounded-full bg-cyan-600/20 blur-3xl"></div>
      </div>

      <%@ include file="common/dashboard-nav.jspf" %>

      <main class="relative z-10 px-4 pb-16 pt-24 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-4xl">
          <button
            type="button"
            onclick="window.location.href='<%= contextPath %>/news'"
            class="mb-8 flex items-center gap-2 text-slate-300 transition-colors hover:text-white reveal"
            data-reveal
          >
            <i data-lucide="arrow-left" class="h-5 w-5"></i>
            뉴스 목록으로
          </button>

          <section
            class="mb-8 overflow-hidden rounded-2xl border border-white/10 bg-slate-900/50 shadow-2xl backdrop-blur-xl reveal card-hover-lift"
            style="--reveal-delay: 100ms;"
            data-reveal
          >
            <div class="detail-hero h-[330px]">
              <div class="absolute inset-0 flex items-center justify-center">
                <div class="detail-hero-icon flex h-24 w-24 items-center justify-center rounded-[2rem] border border-white/10 bg-slate-700/35 backdrop-blur-xl">
                  <i id="categoryHeroIcon" data-lucide="newspaper" class="h-12 w-12 text-sky-300"></i>
                </div>
              </div>
              <div id="categoryBadge" class="absolute left-6 top-6 flex items-center gap-2 rounded-full px-5 py-2 text-sm font-semibold text-white shadow-lg"></div>
            </div>

            <div class="p-8">
              <div class="mb-6 flex flex-wrap items-center gap-4 text-sm text-slate-400">
                <div class="flex items-center gap-2">
                  <i data-lucide="calendar" class="h-4 w-4"></i>
                  <span id="newsDate"><c:out value="${news.displayDate}"/></span>
                </div>
                <span class="text-slate-600">|</span>
                <span id="newsSource"><c:out value="${news.displayProvider}"/></span>
              </div>

              <h1 id="newsTitle" class="mb-6 text-3xl font-bold text-white md:text-4xl"><c:out value="${news.displayTitle}"/></h1>
              <p id="newsSummary" class="mb-6 border-b border-white/10 pb-6 text-lg text-slate-300"><c:out value="${news.displaySummary}"/></p>
              <div id="newsTags" class="mb-8 flex flex-wrap items-center gap-2"></div>

              <div class="mb-3 text-sm font-semibold uppercase tracking-[0.2em] text-sky-400/80">
                <c:choose>
                  <c:when test="${empty news.displaySummary}">요약</c:when>
                  <c:otherwise>본문</c:otherwise>
                </c:choose>
              </div>
              <div class="prose prose-lg prose-invert max-w-none">
                <div id="newsContent" class="whitespace-pre-line leading-relaxed text-slate-300"><c:out value="${news.displaySummary}"/></div>
              </div>

              <div class="mt-8 flex flex-wrap items-center gap-4 border-t border-white/10 pt-8">
                <button
                  type="button"
                  id="bookmarkButton"
                  class="flex items-center gap-2 rounded-xl border border-white/10 bg-white/10 px-4 py-2 font-semibold text-slate-300 transition-all hover:bg-white/20"
                >
                  <i id="bookmarkIcon" data-lucide="bookmark-plus" class="h-5 w-5"></i>
                  <span id="bookmarkLabel">북마크</span>
                </button>
                <button
                  type="button"
                  id="shareButton"
                  class="flex items-center gap-2 rounded-xl border border-white/10 bg-white/10 px-4 py-2 font-semibold text-slate-300 transition-all hover:bg-white/20"
                >
                  <i id="shareIcon" data-lucide="share-2" class="h-5 w-5"></i>
                  <span id="shareLabel">공유하기</span>
                </button>
              </div>
            </div>
          </section>

          <section
            class="rounded-2xl border border-white/10 bg-slate-900/50 p-6 shadow-2xl backdrop-blur-xl reveal card-hover-lift"
            style="--reveal-delay: 180ms;"
            data-reveal
          >
            <h3 class="mb-4 flex items-center gap-2 text-xl font-bold text-white">
              <i data-lucide="external-link" class="h-5 w-5 text-sky-400"></i>
              관련 링크
            </h3>
            <div id="relatedLinks" class="space-y-3"></div>
          </section>
        </div>
      </main>
    </c:otherwise>
  </c:choose>

  <script>
    const contextPath = "<%= contextPath %>";
    const newsId = document.body.dataset.newsId || "";
    const newsBookmarkOwner = "<%= newsBookmarkOwner %>";
    const NEWS_BOOKMARKS_KEY = newsBookmarkOwner ? "newsBookmarks:" + newsBookmarkOwner : "newsBookmarks:guest";
    let isBookmarked = false;

    const categoryIcons = {
      "범죄사례": "siren",
      "법률/규제": "scale",
      "기술동향": "cpu",
      "피해사례": "triangle-alert",
      "대응방법": "shield-check"
    };

    const categoryColors = {
      "범죄사례": "bg-gradient-to-r from-red-500 to-orange-500",
      "법률/규제": "bg-gradient-to-r from-sky-500 to-cyan-500",
      "기술동향": "bg-gradient-to-r from-emerald-500 to-teal-500",
      "피해사례": "bg-gradient-to-r from-fuchsia-500 to-pink-500",
      "대응방법": "bg-gradient-to-r from-cyan-500 to-sky-500"
    };

    const keywordMap = {
      "범죄사례": ["범죄", "검거", "체포", "사건", "성범죄", "처벌", "피의자", "경찰"],
      "법률/규제": ["법률", "규제", "법안", "입법", "개정", "시행", "선거법", "금지"],
      "기술동향": ["기술", "ai", "모델", "탐지", "연구", "개발", "시스템", "업그레이드"],
      "피해사례": ["피해", "협박", "유출", "사칭", "유포", "고통", "불안", "착취"],
      "대응방법": ["대응", "예방", "신고", "가이드", "방법", "체크리스트", "보호", "수칙"]
    };

    function detectCategory(text) {
      const value = String(text || "").toLowerCase();
      let bestCategory = "기술동향";
      let bestScore = 0;

      Object.keys(keywordMap).forEach(function (category) {
        const score = keywordMap[category].reduce(function (sum, keyword) {
          return sum + (value.includes(keyword.toLowerCase()) ? 1 : 0);
        }, 0);

        if (score > bestScore) {
          bestScore = score;
          bestCategory = category;
        }
      });

      return bestCategory;
    }

    function extractTags(text) {
      const value = String(text || "").toLowerCase();
      const tags = [];

      if (value.includes("법률") || value.includes("규제") || value.includes("법안")) tags.push("법률");
      if (value.includes("기술") || value.includes("ai") || value.includes("모델")) tags.push("기술");
      if (value.includes("범죄") || value.includes("체포") || value.includes("사건")) tags.push("범죄");
      if (value.includes("피해") || value.includes("협박") || value.includes("유출")) tags.push("피해");
      if (value.includes("대응") || value.includes("예방") || value.includes("신고")) tags.push("대응");
      if (value.includes("딥페이크") || value.includes("deepfake")) tags.push("딥페이크");

      return tags.slice(0, 4);
    }

    function renderTags(tags) {
      const container = document.getElementById("newsTags");
      if (!container) {
        return;
      }

      container.innerHTML = "";

      tags.forEach(function (tag) {
        const span = document.createElement("span");
        span.className = "flex items-center gap-1 rounded-full border border-sky-500/20 bg-sky-500/10 px-3 py-1 text-sm text-sky-400";
        span.innerHTML = '<i data-lucide="tag" class="h-3 w-3"></i><span>' + tag + "</span>";
        container.appendChild(span);
      });
    }

    function renderRelatedLinks(links) {
      const container = document.getElementById("relatedLinks");
      if (!container) {
        return;
      }

      container.innerHTML = "";

      links.filter(function (link) {
        return link.url && link.url !== "#";
      }).forEach(function (link) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "group flex w-full items-center justify-between rounded-xl border border-white/10 bg-white/5 p-4 text-left text-slate-300 transition-all hover:bg-sky-500/10 hover:text-sky-400";
        button.innerHTML = '<span class="font-medium">' + link.title + '</span><i data-lucide="chevron-right" class="h-5 w-5 transition-transform group-hover:translate-x-1"></i>';
        button.addEventListener("click", function () {
          if (link.url.startsWith("http://") || link.url.startsWith("https://")) {
            window.open(link.url, "_blank", "noopener,noreferrer");
          } else {
            window.location.href = link.url;
          }
        });
        container.appendChild(button);
      });
    }

    function getStoredBookmarks() {
      try {
        const parsed = JSON.parse(localStorage.getItem(NEWS_BOOKMARKS_KEY) || "[]");
        if (!Array.isArray(parsed)) {
          return [];
        }
        return parsed.filter(function (item) {
          return item && item.id;
        });
      } catch (error) {
        return [];
      }
    }

    function saveStoredBookmarks(bookmarks) {
      localStorage.setItem(NEWS_BOOKMARKS_KEY, JSON.stringify(bookmarks));
    }

    function buildBookmarkPayload() {
      const titleElement = document.getElementById("newsTitle");
      const summaryElement = document.getElementById("newsSummary");
      const dateElement = document.getElementById("newsDate");
      const originalLink = "<c:out value='${news.originalUrl}'/>";
      const link = "";

      if (!newsId) {
        return null;
      }

      return {
        id: String(newsId),
        title: titleElement ? titleElement.textContent.trim() : "",
        summary: summaryElement ? summaryElement.textContent.trim() : "",
        pubDate: dateElement ? dateElement.textContent.trim() : "",
        url: window.location.pathname + window.location.search,
        originalLink: originalLink || "",
        link: link || "",
        savedAt: new Date().toISOString()
      };
    }

    function syncBookmarkUi() {
      const button = document.getElementById("bookmarkButton");
      const icon = document.getElementById("bookmarkIcon");
      const label = document.getElementById("bookmarkLabel");
      if (!button || !icon || !label) {
        return;
      }

      if (isBookmarked) {
        button.className = "flex items-center gap-2 rounded-xl border border-sky-500/30 bg-sky-500/20 px-4 py-2 font-semibold text-sky-400 transition-all";
        icon.setAttribute("data-lucide", "bookmark");
        icon.className = "h-5 w-5 fill-sky-400";
        label.textContent = "북마크됨";
      } else {
        button.className = "flex items-center gap-2 rounded-xl border border-white/10 bg-white/10 px-4 py-2 font-semibold text-slate-300 transition-all hover:bg-white/20";
        icon.setAttribute("data-lucide", "bookmark-plus");
        icon.className = "h-5 w-5";
        label.textContent = "북마크";
      }

      lucide.createIcons();
    }

    function toggleBookmark() {
      if (!newsId) {
        return;
      }

      const bookmarks = getStoredBookmarks();
      const existingIndex = bookmarks.findIndex(function (item) {
        return String(item.id) === String(newsId);
      });

      isBookmarked = existingIndex < 0;

      if (isBookmarked) {
        const payload = buildBookmarkPayload();
        if (payload) {
          bookmarks.unshift(payload);
        }
      } else {
        bookmarks.splice(existingIndex, 1);
      }

      saveStoredBookmarks(bookmarks);
      syncBookmarkUi();
    }

    async function shareArticle() {
      const shareIcon = document.getElementById("shareIcon");
      const shareLabel = document.getElementById("shareLabel");
      if (!shareIcon || !shareLabel) {
        return;
      }

      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(window.location.href);
        }
        shareIcon.setAttribute("data-lucide", "check");
        shareLabel.textContent = "링크 복사됨";
      } catch (error) {
        shareLabel.textContent = "복사 실패";
      }

      lucide.createIcons();

      setTimeout(function () {
        shareIcon.setAttribute("data-lucide", "share-2");
        shareLabel.textContent = "공유하기";
        lucide.createIcons();
      }, 2000);
    }

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

    function renderPage() {
      const titleElement = document.getElementById("newsTitle");
      const summaryElement = document.getElementById("newsSummary");
      const dateElement = document.getElementById("newsDate");
      const sourceElement = document.getElementById("newsSource");
      const categoryBadge = document.getElementById("categoryBadge");
      const categoryHeroIcon = document.getElementById("categoryHeroIcon");

      if (!titleElement || !summaryElement || !dateElement || !sourceElement || !categoryBadge || !categoryHeroIcon) {
        return;
      }

      const title = titleElement.textContent.trim();
      const summary = summaryElement.textContent.trim();
      const category = detectCategory(title + " " + summary);
      const icon = categoryIcons[category] || "newspaper";
      const color = categoryColors[category] || "bg-gradient-to-r from-sky-500 to-cyan-500";
      const originalLink = "<c:out value='${news.originalUrl}'/>";
      const link = "";

      document.title = title ? title + " - DeepScan" : "뉴스 상세 - DeepScan";
      sourceElement.textContent = "뉴스 서비스";
      categoryBadge.className = "absolute left-6 top-6 flex items-center gap-2 rounded-full px-4 py-2 text-white shadow-lg backdrop-blur-xl " + color;
      categoryBadge.innerHTML = '<i data-lucide="' + icon + '" class="h-5 w-5"></i><span>' + category + "</span>";
      categoryHeroIcon.setAttribute("data-lucide", icon);

      renderTags(extractTags(title + " " + summary));
      renderRelatedLinks([
        { title: "원문 보기", url: originalLink || link || "#" },
        { title: "딥페이크 신고하러 가기", url: contextPath + "/report" }
      ]);

      if (newsId) {
        isBookmarked = getStoredBookmarks().some(function (item) {
          return String(item.id) === String(newsId);
        });
      }
      syncBookmarkUi();
    }

    document.addEventListener("DOMContentLoaded", function () {
      const bookmarkButton = document.getElementById("bookmarkButton");
      const shareButton = document.getElementById("shareButton");

      if (bookmarkButton) {
        bookmarkButton.addEventListener("click", toggleBookmark);
      }

      if (shareButton) {
        shareButton.addEventListener("click", shareArticle);
      }

      renderPage();
      initReveal();
      lucide.createIcons();
    });
  </script>
</body>
</html>

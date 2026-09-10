<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
  request.setAttribute("activePage", "news");
  Object newsBookmarkUserIdObj = session.getAttribute("USER_ID");
  String newsBookmarkOwner = newsBookmarkUserIdObj == null ? "" : String.valueOf(newsBookmarkUserIdObj);
%>
<!DOCTYPE html>
<html lang="ko">
<head>
  <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/resources/image/deepscan-mark.svg?v=30">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>뉴스 - DeepScan</title>
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
      transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
    }
    .card-hover-lift:hover {
      transform: translateY(-6px);
      box-shadow: 0 24px 60px rgba(15, 23, 42, 0.32);
    }
    .news-card {
      transition: transform 0.32s ease, border-color 0.32s ease, box-shadow 0.32s ease, background-color 0.32s ease;
    }
    .news-card:hover {
      border-color: color-mix(in srgb, var(--card-accent, #38bdf8) 42%, rgba(255, 255, 255, 0.12));
      box-shadow: 0 26px 72px color-mix(in srgb, var(--card-accent, #38bdf8) 18%, transparent), 0 24px 60px rgba(15, 23, 42, 0.34);
    }
    .news-card-media {
      position: relative;
      height: 220px;
      overflow: hidden;
      background: #0f172a;
    }
    .news-card-media img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform 0.45s ease, filter 0.35s ease;
    }
    .news-card-media::after {
      content: "";
      position: absolute;
      inset: 0;
      background: linear-gradient(to bottom, rgba(2, 6, 23, 0.12), rgba(2, 6, 23, 0.34));
      pointer-events: none;
      transition: background 0.32s ease;
    }
    .news-card:hover .news-card-media img {
      transform: scale(1.045);
      filter: saturate(1.08);
    }
    .news-card:hover .news-card-media::after {
      background: linear-gradient(to bottom, color-mix(in srgb, var(--card-accent, #38bdf8) 18%, rgba(2, 6, 23, 0.18)), rgba(2, 6, 23, 0.48));
    }
    .news-card-body {
      transition: background 0.32s ease;
    }
    .news-card:hover .news-card-body {
      background: linear-gradient(180deg, color-mix(in srgb, var(--card-accent, #38bdf8) 8%, #0b1220), #0b1220 48%);
    }
    .news-card-chip {
      transition: transform 0.28s ease, box-shadow 0.28s ease, background-color 0.28s ease, border-color 0.28s ease;
    }
    .news-card:hover .news-card-chip {
      transform: translateY(-1px);
      box-shadow: 0 14px 30px color-mix(in srgb, var(--card-accent, #38bdf8) 30%, transparent);
      border-color: color-mix(in srgb, var(--card-accent, #38bdf8) 55%, rgba(255, 255, 255, 0.22));
      background-color: color-mix(in srgb, var(--card-accent, #38bdf8) 84%, rgba(15, 23, 42, 0.18));
    }
    .news-card-tag {
      transition: transform 0.24s ease, border-color 0.24s ease, background-color 0.24s ease, color 0.24s ease;
    }
    .news-card:hover .news-card-tag {
      transform: translateY(-1px);
      border-color: color-mix(in srgb, var(--card-accent, #38bdf8) 28%, rgba(255, 255, 255, 0.1));
      background-color: color-mix(in srgb, var(--card-accent, #38bdf8) 14%, rgba(15, 23, 42, 0.72));
      color: #f8fafc;
    }
    .news-card-bookmark {
      transition: transform 0.24s ease, border-color 0.24s ease, background-color 0.24s ease, box-shadow 0.24s ease;
    }
    .news-card:hover .news-card-bookmark {
      transform: translateY(-1px);
      border-color: color-mix(in srgb, var(--card-accent, #38bdf8) 35%, rgba(255, 255, 255, 0.16));
      box-shadow: 0 12px 24px color-mix(in srgb, var(--card-accent, #38bdf8) 18%, transparent);
    }
    .news-card-bookmark.is-active {
      border-color: rgba(56, 189, 248, 0.55);
      background: linear-gradient(135deg, rgba(14, 165, 233, 0.92), rgba(34, 211, 238, 0.88));
      box-shadow: 0 14px 30px rgba(14, 165, 233, 0.34);
      color: #f0f9ff;
    }
    .news-card:hover .news-card-bookmark.is-active {
      border-color: rgba(125, 211, 252, 0.75);
      box-shadow: 0 16px 34px rgba(14, 165, 233, 0.42);
    }
    .line-clamp-2 {
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  </style>
</head>
<body class="min-h-screen overflow-x-hidden bg-slate-950 text-white">
  <%@ include file="common/dashboard-nav.jspf" %>

  <div class="relative min-h-screen px-4 pb-16 pt-28 sm:px-6 lg:px-8">
    <div class="absolute inset-0 -z-10 overflow-hidden">
      <div class="absolute left-1/4 top-16 h-80 w-80 rounded-full bg-sky-600/20 blur-3xl"></div>
      <div class="absolute bottom-10 right-1/4 h-96 w-96 rounded-full bg-cyan-600/20 blur-3xl"></div>
    </div>

    <div class="mx-auto max-w-7xl">
      <div class="mb-12 text-center reveal" data-reveal>
        <div class="relative mb-6 inline-block">
          <div class="absolute inset-0 rounded-2xl bg-gradient-to-r from-sky-600 to-cyan-600 opacity-75 blur-xl"></div>
          <div class="relative mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-500 to-cyan-500">
            <i data-lucide="newspaper" class="h-8 w-8 text-white"></i>
          </div>
        </div>
        <h1 class="mb-4 text-4xl font-bold md:text-5xl">
          딥페이크 <span class="bg-gradient-to-r from-sky-400 to-cyan-400 bg-clip-text text-transparent">뉴스</span>
        </h1>
        <p class="mx-auto max-w-2xl text-lg text-slate-400">최신 딥페이크 뉴스를 주제와 정렬 기준에 맞춰 빠르게 확인하세요.</p>
      </div>

      <div class="mb-8 reveal" style="--reveal-delay: 100ms;" data-reveal>
        <form id="newsFilterForm" method="get" action="${pageContext.request.contextPath}/news" class="rounded-2xl border border-white/10 bg-slate-900/60 p-6 backdrop-blur-xl card-hover-lift">
          <div class="relative mb-4">
            <i data-lucide="search" class="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500"></i>
            <input
              id="searchInput"
              name="query"
              type="text"
              placeholder="뉴스 검색어를 입력하세요"
              value="<c:out value='${query}'/>"
              class="w-full rounded-xl border border-white/15 bg-white/5 py-3 pl-12 pr-4 text-white placeholder-slate-500 backdrop-blur-xl transition-all focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
          </div>

          <div class="flex items-center gap-2 overflow-x-auto pb-2">
            <i data-lucide="filter" class="h-5 w-5 flex-shrink-0 text-slate-400"></i>
            <c:url var="categoryAllUrl" value="/news"><c:param name="query" value="${query}" /><c:param name="category" value="전체" /><c:param name="sort" value="${selectedSort}" /><c:param name="page" value="1" /></c:url>
            <c:url var="categoryCrimeUrl" value="/news"><c:param name="query" value="${query}" /><c:param name="category" value="범죄사례" /><c:param name="sort" value="${selectedSort}" /><c:param name="page" value="1" /></c:url>
            <c:url var="categoryLawUrl" value="/news"><c:param name="query" value="${query}" /><c:param name="category" value="법률/규제" /><c:param name="sort" value="${selectedSort}" /><c:param name="page" value="1" /></c:url>
            <c:url var="categoryTechUrl" value="/news"><c:param name="query" value="${query}" /><c:param name="category" value="기술동향" /><c:param name="sort" value="${selectedSort}" /><c:param name="page" value="1" /></c:url>
            <c:url var="categoryDamageUrl" value="/news"><c:param name="query" value="${query}" /><c:param name="category" value="피해사례" /><c:param name="sort" value="${selectedSort}" /><c:param name="page" value="1" /></c:url>
            <c:url var="categoryResponseUrl" value="/news"><c:param name="query" value="${query}" /><c:param name="category" value="대응방법" /><c:param name="sort" value="${selectedSort}" /><c:param name="page" value="1" /></c:url>
            <a href="${categoryAllUrl}" class="category-chip whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-all ${selectedCategory == '전체' ? 'border border-sky-400/30 bg-gradient-to-r from-sky-600 to-cyan-600 text-white' : 'bg-white/10 text-slate-300 hover:bg-white/20'}">전체</a>
            <a href="${categoryCrimeUrl}" class="category-chip whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-all ${selectedCategory == '범죄사례' ? 'border border-sky-400/30 bg-gradient-to-r from-sky-600 to-cyan-600 text-white' : 'bg-white/10 text-slate-300 hover:bg-white/20'}">범죄사례</a>
            <a href="${categoryLawUrl}" class="category-chip whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-all ${selectedCategory == '법률/규제' ? 'border border-sky-400/30 bg-gradient-to-r from-sky-600 to-cyan-600 text-white' : 'bg-white/10 text-slate-300 hover:bg-white/20'}">법률/규제</a>
            <a href="${categoryTechUrl}" class="category-chip whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-all ${selectedCategory == '기술동향' ? 'border border-sky-400/30 bg-gradient-to-r from-sky-600 to-cyan-600 text-white' : 'bg-white/10 text-slate-300 hover:bg-white/20'}">기술동향</a>
            <a href="${categoryDamageUrl}" class="category-chip whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-all ${selectedCategory == '피해사례' ? 'border border-sky-400/30 bg-gradient-to-r from-sky-600 to-cyan-600 text-white' : 'bg-white/10 text-slate-300 hover:bg-white/20'}">피해사례</a>
            <a href="${categoryResponseUrl}" class="category-chip whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-all ${selectedCategory == '대응방법' ? 'border border-sky-400/30 bg-gradient-to-r from-sky-600 to-cyan-600 text-white' : 'bg-white/10 text-slate-300 hover:bg-white/20'}">대응방법</a>
          </div>

          <div class="mt-4 flex items-center gap-2 overflow-x-auto pb-1">
            <i data-lucide="arrow-up-down" class="h-5 w-5 flex-shrink-0 text-slate-400"></i>
            <c:url var="sortDateUrl" value="/news"><c:param name="query" value="${query}" /><c:param name="category" value="${selectedCategory}" /><c:param name="sort" value="date" /><c:param name="page" value="1" /></c:url>
            <c:url var="sortSimUrl" value="/news"><c:param name="query" value="${query}" /><c:param name="category" value="${selectedCategory}" /><c:param name="sort" value="sim" /><c:param name="page" value="1" /></c:url>
            <a href="${sortDateUrl}" class="sort-chip whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-all ${selectedSort == 'date' ? 'border border-sky-400/30 bg-gradient-to-r from-sky-600 to-cyan-600 text-white' : 'bg-white/10 text-slate-300 hover:bg-white/20'}">최신순</a>
            <a href="${sortSimUrl}" class="sort-chip whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition-all ${selectedSort == 'sim' ? 'border border-sky-400/30 bg-gradient-to-r from-sky-600 to-cyan-600 text-white' : 'bg-white/10 text-slate-300 hover:bg-white/20'}">관련도순</a>
          </div>
        </form>
      </div>

      <div id="newsGrid" class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        <c:forEach var="news" items="${newsList}" varStatus="status">
          <article
            class="news-card group relative overflow-hidden rounded-[28px] border border-white/10 bg-[#0b1220] transition-all reveal card-hover-lift"
            style="--reveal-delay:${status.index % 6 * 60}ms"
            data-reveal
            data-news-card
            data-id="${news.id}"
            data-title="<c:out value='${news.title}'/>"
            data-summary="<c:out value='${news.description}'/>"
          >
            <c:url var="newsDetailUrl" value="/news/${news.id}">
              <c:param name="title" value="${news.title}" />
              <c:param name="description" value="${news.description}" />
              <c:param name="pubDate" value="${news.pubDate}" />
              <c:param name="originallink" value="${news.originallink}" />
              <c:param name="link" value="${news.link}" />
            </c:url>

            <button
              type="button"
              onclick="toggleBookmark('${news.id}', this); event.preventDefault(); event.stopPropagation();"
              class="news-card-bookmark absolute right-4 top-4 z-20 rounded-full border border-white/15 bg-slate-950/75 p-2.5 text-white shadow-lg backdrop-blur-md transition-all hover:bg-slate-900"
              data-bookmark-button
              aria-label="북마크 추가"
            >
              <i data-lucide="bookmark-plus" class="h-5 w-5 text-slate-200"></i>
            </button>

            <a href="${newsDetailUrl}" class="block">
              <div class="news-card-media">
                <img data-card-image alt="">

                <div class="absolute left-4 top-4 z-10">
                  <span data-card-chip class="news-card-chip inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-semibold text-white shadow-lg backdrop-blur-md">
                    <i data-card-chip-icon data-lucide="newspaper" class="h-3.5 w-3.5"></i>
                    <span data-card-chip-text></span>
                  </span>
                </div>
              </div>

              <div class="news-card-body bg-[#0b1220] px-6 pb-6 pt-5">
                <div class="mb-3 flex items-center gap-2 text-sm text-slate-400">
                  <i data-lucide="calendar" class="h-4 w-4"></i>
                  <span><c:out value="${news.pubDate}"/></span>
                  <span class="text-slate-700">|</span>
                  <span>뉴스 서비스</span>
                </div>

                <h3 class="mb-3 line-clamp-2 text-xl font-bold leading-snug text-white transition-colors group-hover:text-sky-300">
                  <c:out value="${news.title}"/>
                </h3>
                <p class="mb-4 line-clamp-2 text-sm leading-6 text-slate-400">
                  <c:out value="${news.description}"/>
                </p>

                <div class="mb-4 flex flex-wrap gap-2" data-tags-wrap></div>

                <div class="flex items-center justify-between">
                  <span class="inline-flex items-center gap-1 text-sm font-semibold text-sky-300 transition-all group-hover:gap-2">
                    자세히 보기
                    <i data-lucide="chevron-right" class="h-4 w-4 transition-transform group-hover:translate-x-1"></i>
                  </span>
                </div>
              </div>
            </a>
          </article>
        </c:forEach>
      </div>

      <c:if test="${empty newsList}">
        <div id="emptyState" class="py-20 text-center reveal" style="--reveal-delay: 120ms;" data-reveal>
          <i data-lucide="newspaper" class="mx-auto mb-4 h-16 w-16 text-slate-700"></i>
          <p class="text-lg text-slate-500">검색 결과가 없습니다.</p>
        </div>
      </c:if>

      <div id="resultsCount" class="mt-8 text-center text-slate-400 reveal" style="--reveal-delay: 160ms;" data-reveal>
        현재 조건 결과 ${totalCount}건
      </div>

      <c:if test="${totalPages > 1}">
        <c:set var="startPage" value="${currentPage - 2 > 1 ? currentPage - 2 : 1}" />
        <c:set var="endPage" value="${startPage + 4 < totalPages ? startPage + 4 : totalPages}" />
        <c:set var="startPage" value="${endPage - 4 > 1 ? endPage - 4 : 1}" />

        <div class="mt-10 reveal" style="--reveal-delay: 200ms;" data-reveal>
          <nav class="flex flex-wrap items-center justify-center gap-2" aria-label="뉴스 페이지 이동">
            <c:url var="firstPageUrl" value="/news">
              <c:param name="query" value="${query}" />
              <c:param name="category" value="${selectedCategory}" />
              <c:param name="sort" value="${selectedSort}" />
              <c:param name="page" value="1" />
            </c:url>
            <c:url var="prevPageUrl" value="/news">
              <c:param name="query" value="${query}" />
              <c:param name="category" value="${selectedCategory}" />
              <c:param name="sort" value="${selectedSort}" />
              <c:param name="page" value="${currentPage - 1}" />
            </c:url>
            <c:url var="nextPageUrl" value="/news">
              <c:param name="query" value="${query}" />
              <c:param name="category" value="${selectedCategory}" />
              <c:param name="sort" value="${selectedSort}" />
              <c:param name="page" value="${currentPage + 1}" />
            </c:url>
            <c:url var="lastPageUrl" value="/news">
              <c:param name="query" value="${query}" />
              <c:param name="category" value="${selectedCategory}" />
              <c:param name="sort" value="${selectedSort}" />
              <c:param name="page" value="${totalPages}" />
            </c:url>

            <a href="${firstPageUrl}" class="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-300 transition-all hover:bg-white/10 ${currentPage == 1 ? 'pointer-events-none opacity-40' : ''}">처음</a>
            <a href="${prevPageUrl}" class="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-300 transition-all hover:bg-white/10 ${currentPage == 1 ? 'pointer-events-none opacity-40' : ''}">이전</a>

            <c:forEach begin="${startPage}" end="${endPage}" var="pageNumber">
              <c:url var="newsPageUrl" value="/news">
                <c:param name="query" value="${query}" />
                <c:param name="category" value="${selectedCategory}" />
                <c:param name="sort" value="${selectedSort}" />
                <c:param name="page" value="${pageNumber}" />
              </c:url>
              <a href="${newsPageUrl}" aria-current="${pageNumber == currentPage ? 'page' : 'false'}" class="min-w-11 rounded-xl border px-4 py-2 text-center text-sm font-semibold transition-all ${pageNumber == currentPage ? 'border-sky-500/40 bg-sky-500/20 text-sky-300' : 'border-white/10 bg-white/5 text-slate-300 hover:bg-white/10'}">${pageNumber}</a>
            </c:forEach>

            <a href="${nextPageUrl}" class="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-300 transition-all hover:bg-white/10 ${currentPage == totalPages ? 'pointer-events-none opacity-40' : ''}">다음</a>
            <a href="${lastPageUrl}" class="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-300 transition-all hover:bg-white/10 ${currentPage == totalPages ? 'pointer-events-none opacity-40' : ''}">마지막</a>
          </nav>

          <p class="mt-4 text-center text-sm text-slate-500">${currentPage} / ${totalPages} 페이지</p>
        </div>
      </c:if>
    </div>
  </div>

  <script>
    const contextPath = "${pageContext.request.contextPath}";
    const newsBookmarkOwner = "<%= newsBookmarkOwner %>";
    const NEWS_BOOKMARKS_KEY = newsBookmarkOwner ? "newsBookmarks:" + newsBookmarkOwner : "newsBookmarks:guest";

    const categoryConfig = {
      "범죄사례": {
        icon: "siren",
        image: "범죄사례.png",
        accent: "#ef4444",
        chipClass: "border-red-400/30 bg-red-500/75",
        tagClass: "border-red-400/20 bg-red-500/10 text-red-200"
      },
      "법률/규제": {
        icon: "scale",
        image: "법률규제.png",
        accent: "#3b82f6",
        chipClass: "border-blue-300/30 bg-blue-500/80",
        tagClass: "border-blue-400/20 bg-blue-500/10 text-blue-200"
      },
      "기술동향": {
        icon: "cpu",
        image: "기술동향.png",
        accent: "#10b981",
        chipClass: "border-emerald-300/30 bg-emerald-500/80",
        tagClass: "border-emerald-400/20 bg-emerald-500/10 text-emerald-200"
      },
      "피해사례": {
        icon: "triangle-alert",
        image: "피해사례.png",
        accent: "#ec4899",
        chipClass: "border-pink-300/30 bg-pink-500/80",
        tagClass: "border-pink-400/20 bg-pink-500/10 text-pink-200"
      },
      "대응방법": {
        icon: "shield-check",
        image: "대응방법.png",
        accent: "#06b6d4",
        chipClass: "border-cyan-300/30 bg-cyan-500/80",
        tagClass: "border-cyan-400/20 bg-cyan-500/10 text-cyan-200"
      }
    };

    const keywordMap = {
      "범죄사례": ["범죄", "검거", "체포", "사건", "성착취", "처벌", "사기", "경찰"],
      "법률/규제": ["법률", "규제", "법안", "입법", "개정", "시행", "관련법", "금지"],
      "기술동향": ["기술", "ai", "모델", "연구", "개발", "시스템", "업그레이드"],
      "피해사례": ["피해", "협박", "유출", "사칭", "폭로", "고통", "불안", "창피"],
      "대응방법": ["대응", "예방", "경고", "가이드", "방법", "체크리스트", "보호", "수칙"]
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
      const lower = String(text || "").toLowerCase();
      const rules = [
        ["법률", ["법률", "규제", "법안", "개정"]],
        ["분석", ["분석", "탐지", "검증", "시스템"]],
        ["범죄", ["범죄", "처벌", "체포", "사건"]],
        ["피해", ["피해", "유출", "협박", "고통"]],
        ["대응", ["대응", "예방", "가이드", "경고"]],
        ["AI", ["ai", "deepfake", "딥페이크", "생성형"]]
      ];

      return rules.filter(function (rule) {
        return rule[1].some(function (keyword) {
          return lower.includes(keyword.toLowerCase());
        });
      }).map(function (rule) {
        return rule[0];
      }).slice(0, 3);
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

    function getBookmarkedNewsIds() {
      return new Set(getStoredBookmarks().map(function (item) {
        return String(item.id);
      }));
    }

    function buildBookmarkPayload(card) {
      if (!card) {
        return null;
      }

      const newsId = String(card.dataset.id || "").trim();
      if (!newsId) {
        return null;
      }

      const link = card.querySelector("a[href]");
      const dateText = Array.from(card.querySelectorAll("span")).map(function (item) {
        return (item.textContent || "").trim();
      }).find(function (text) {
        return text && /\d/.test(text);
      }) || "";

      return {
        id: newsId,
        title: String(card.dataset.title || "").trim(),
        summary: String(card.dataset.summary || "").trim(),
        pubDate: dateText,
        url: link ? link.getAttribute("href") : "",
        savedAt: new Date().toISOString()
      };
    }

    function syncBookmarkButtons() {
      const bookmarkedNews = getBookmarkedNewsIds();
      document.querySelectorAll("[data-news-card]").forEach(function (card) {
        const id = card.dataset.id;
        const button = card.querySelector("[data-bookmark-button]");
        if (!button) {
          return;
        }

        const icon = button.querySelector("i");
        if (!icon) {
          return;
        }

        const active = bookmarkedNews.has(id);
        icon.setAttribute("data-lucide", active ? "bookmark-check" : "bookmark-plus");
        if (active) {
          button.className = "news-card-bookmark is-active absolute right-4 top-4 z-10 rounded-full border p-2.5 shadow-lg backdrop-blur-md transition-all";
          button.setAttribute("aria-label", "북마크 해제");
          icon.className = "h-5 w-5 text-white";
        } else {
          button.className = "news-card-bookmark absolute right-4 top-4 z-10 rounded-full border border-white/15 bg-slate-950/75 p-2.5 text-white shadow-lg backdrop-blur-md transition-all hover:bg-slate-900";
          button.setAttribute("aria-label", "북마크 추가");
          icon.className = "h-5 w-5 text-slate-200";
        }
      });

      lucide.createIcons();
    }

    function toggleBookmark(newsId, button) {
      const card = button ? button.closest("[data-news-card]") : document.querySelector('[data-news-card][data-id="' + newsId + '"]');
      const payload = buildBookmarkPayload(card);
      const bookmarks = getStoredBookmarks();
      const existingIndex = bookmarks.findIndex(function (item) {
        return String(item.id) === String(newsId);
      });

      if (existingIndex >= 0) {
        bookmarks.splice(existingIndex, 1);
      } else if (payload) {
        bookmarks.unshift(payload);
      }

      saveStoredBookmarks(bookmarks);

      if (button) {
        button.blur();
      }

      syncBookmarkButtons();
    }

    function decorateCards() {
      document.querySelectorAll("[data-news-card]").forEach(function (card) {
        const title = card.dataset.title || "";
        const summary = card.dataset.summary || "";
        const category = detectCategory(title + " " + summary);
        const config = categoryConfig[category];
        const image = card.querySelector("[data-card-image]");
        const chip = card.querySelector("[data-card-chip]");
        const chipIcon = card.querySelector("[data-card-chip-icon]");
        const chipText = card.querySelector("[data-card-chip-text]");
        const tagsWrap = card.querySelector("[data-tags-wrap]");

        if (!config || !image || !chip || !chipIcon || !chipText || !tagsWrap) {
          return;
        }

        card.dataset.category = category;
        card.style.setProperty("--card-accent", config.accent);
        image.src = contextPath + "/resources/image/" + encodeURIComponent(config.image);
        image.alt = category;
        chip.className = "news-card-chip inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-semibold text-white shadow-lg backdrop-blur-md " + config.chipClass;
        chipText.textContent = category;
        chipIcon.setAttribute("data-lucide", config.icon);
        chipIcon.className = "h-3.5 w-3.5";

        tagsWrap.innerHTML = extractTags(title + " " + summary).map(function (tag) {
          return '<span class="news-card-tag inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-medium ' + config.tagClass + '"><i data-lucide="tag" class="h-3 w-3"></i><span>' + tag + '</span></span>';
        }).join("");
      });

      lucide.createIcons();
      syncBookmarkButtons();
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

    decorateCards();
    initReveal(document);
    lucide.createIcons();
  </script>
</body>
</html>

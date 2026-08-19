package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(뉴스): 네이버 뉴스 API 조회, 검색 결과 변환, 캐시 처리를 담당한다.
 */

/**
 * 발표용 설명: 뉴스 기능의 핵심 서비스입니다.
 * 네이버 뉴스 API 호출, 기사 본문 추출, 딥페이크 관련성 필터링, 중복 제거, MongoDB 캐시 저장을 담당합니다.
 * 캐시를 사용해 같은 검색어 요청이 반복될 때 외부 API 호출을 줄이고 화면 응답 속도를 높입니다.
 */
import kopo.poly.document.NewsCacheDocument;
import kopo.poly.dto.NaverNewsItemDTO;
import kopo.poly.dto.NaverNewsResponseDTO;
import kopo.poly.dto.NewsPageDTO;
import kopo.poly.dto.NewsViewDTO;
import kopo.poly.repository.NewsCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Naver News API and article pages are collected, filtered, deduplicated, and cached here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {
    // Naver News API sort options: sim for relevance, date for latest.
    public static final String SORT_SIM = "sim";
    public static final String SORT_DATE = "date";

    // Fetch enough data for deduplication, then display a smaller stable list.
    private static final int NEWS_DISPLAY_COUNT = 30;
    private static final int NEWS_MAX_FETCH_COUNT = 100;
    private static final int NEWS_FETCH_PAGE_COUNT = 5;
    private static final int LOCAL_CACHE_MINUTES = 360;
    private static final String NEWS_CACHE_VERSION = "v3";
    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile("(?is)<(script|style|noscript|iframe|svg)[^>]*>.*?</\\1>");
    private static final Pattern BLOCK_BREAK_PATTERN = Pattern.compile("(?is)</(p|div|section|article|li|br|h1|h2|h3|h4|h5|h6|tr)>");
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern ARTICLE_PATTERN = Pattern.compile("(?is)<article[^>]*>(.*?)</article>");
    private static final Pattern MAIN_PATTERN = Pattern.compile("(?is)<main[^>]*>(.*?)</main>");
    private static final Pattern BODY_PATTERN = Pattern.compile("(?is)<body[^>]*>(.*?)</body>");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("(?is)<p[^>]*>(.*?)</p>");
    private static final Pattern META_DESCRIPTION_PATTERN = Pattern.compile("(?is)<meta[^>]+(?:property|name)=[\"'](?:og:description|description)[\"'][^>]+content=[\"'](.*?)[\"'][^>]*>");
    private static final Pattern META_CHARSET_PATTERN = Pattern.compile("(?is)<meta[^>]+charset=[\"']?([a-zA-Z0-9_\\-]+)[\"']?");
    private static final Pattern META_CONTENT_TYPE_CHARSET_PATTERN = Pattern.compile("(?is)<meta[^>]+content=[\"'][^\"']*charset=([a-zA-Z0-9_\\-]+)[^\"']*[\"']");
    private static final Pattern LEADING_BRACKET_PATTERN = Pattern.compile("^(?:\\[[^\\]]+\\]|\\([^\\)]+\\)|<[^>]+>|\\{[^\\}]+\\})\\s*");
    private static final Pattern TITLE_NOISE_PATTERN = Pattern.compile("(?:\\b(?:breaking|exclusive|photo|video|\\uC18D\\uBCF4|\\uB2E8\\uB3C5|\\uC885\\uD569|\\uC601\\uC0C1|\\uD3EC\\uD1A0)\\b|[`\\u201C\\u201D\\u2018\\u2019]|\\.{2,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_WORD_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}\\s]");
    private static final Pattern REPORTER_PATTERN = Pattern.compile(".*(?:reporter|journalist|news|press|email|copyright).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(".*[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}.*", Pattern.CASE_INSENSITIVE);
    private static final String ARTICLE_SELECTOR = String.join(", ",
            "article",
            "main",
            "[itemprop=articleBody]",
            "[role=main]",
            "#dic_area",
            "#newsct_article",
            "#articleBodyContents",
            "#articletxt",
            "#article_body",
            "#articleBody",
            ".article_body",
            ".article-body",
            ".articleBody",
            ".article_txt",
            ".article-body-view",
            ".article-view-content",
            ".article-view-content-div",
            ".newsct_article",
            ".news_end",
            ".news_view",
            ".news-view",
            ".news_contents",
            ".news_content",
            ".story-news",
            ".entry-content",
            ".post-content",
            ".read-content",
            ".view-content"
    );
    private static final String REMOVABLE_SELECTOR = String.join(", ",
            "script",
            "style",
            "noscript",
            "iframe",
            "svg",
            "form",
            "button",
            "input",
            "nav",
            "aside",
            "footer",
            "header",
            ".ad",
            ".ads",
            ".advertisement",
            ".promotion",
            ".related",
            ".related_news",
            ".reporter_area",
            ".byline",
            ".copyright",
            ".photo",
            ".image",
            ".video",
            ".sns",
            ".link_news"
    );
    private static final List<String> ARTICLE_CONTAINER_HINTS = List.of(
            "article_body",
            "articlebody",
            "article_txt",
            "articletext",
            "article-view-content-div",
            "article-view-content",
            "article-view",
            "articleBodyContents",
            "articletxt",
            "newsct_article",
            "news_end",
            "newsView",
            "news-view",
            "news_view",
            "news_contents",
            "news_content",
            "dic_area",
            "_article_content",
            "story-news",
            "post-content",
            "entry-content",
            "article_content",
            "read-content",
            "view-content"
    );
    private static final List<String> DEEPFAKE_KEYWORDS = List.of(
            "\uB525\uD398\uC774\uD06C",
            "\uB525 \uD398\uC774\uD06C",
            "deepfake",
            "deep fake",
            "\uAC00\uC9DC \uC601\uC0C1",
            "\uD569\uC131 \uC601\uC0C1",
            "\uC870\uC791 \uC601\uC0C1",
            "ai \uC870\uC791"
    );

    private static final List<String> NEWS_SEARCH_KEYWORDS = List.of(
            "\uB525\uD398\uC774\uD06C",
            "\uC0DD\uC131\uD615 AI",
            "\uC778\uACF5\uC9C0\uB2A5 \uC870\uC791",
            "\uD5C8\uC704\uC601\uC0C1",
            "\uC774\uBBF8\uC9C0 \uC870\uC791",
            "\uC601\uC0C1 \uC870\uC791",
            "\uAC00\uC9DC\uB274\uC2A4",
            "AI \uC0AC\uAE30",
            "AI",
            "\uC778\uACF5\uC9C0\uB2A5",
            "\uC870\uC791",
            "\uD5C8\uC704",
            "\uAC00\uC9DC",
            "\uC0AC\uAE30",
            "deepfake",
            "deep fake"
    );

    private final RestClient restClient;
    private final NewsCacheRepository newsCacheRepository;
    // Short-lived in-memory cache used to reduce repeated MongoDB/API calls.
    private final Map<String, CachedNewsBundle> localCache = new ConcurrentHashMap<>();
    // Per-query locks prevent concurrent requests from triggering duplicate API calls.
    private final Map<String, Object> queryLocks = new ConcurrentHashMap<>();

    @Value("${naver.client-id:}")
    private String naverClientId;

    @Value("${naver.client-secret:}")
    private String naverClientSecret;

    @Value("${naver.news-url:https://openapi.naver.com/v1/search/news.json}")
    private String naverNewsUrl;

    private record ArticleHttpResponse(byte[] body, HttpHeaders headers) {
    }

    private record NewsApiDocument(String title,
                                   String description,
                                   String summary,
                                   String originalUrl,
                                   String link,
                                   String provider,
                                   String pubDate) {
    }

    public List<NewsCacheDocument> getNews(String query, String sort) {
        // Normalize inputs, then resolve news from local cache -> MongoDB cache -> live API.
        String normalizedQuery = normalizeQuery(query);
        String normalizedSort = normalizeSort(sort);
        String cacheKey = buildCacheKey(normalizedQuery, normalizedSort);

        // Check local memory first to avoid repeated API/MongoDB calls.
        List<NewsCacheDocument> localCached = getLocalCache(cacheKey);
        if (!localCached.isEmpty()) {
            return localCached;
        }

        Object queryLock = queryLocks.computeIfAbsent(cacheKey, key -> new Object());
        try {
            synchronized (queryLock) {
                localCached = getLocalCache(cacheKey);
                if (!localCached.isEmpty()) {
                    return deduplicateArticles(localCached);
                }

                // If local cache is empty, check the persisted MongoDB cache.
                List<NewsCacheDocument> mongoCached = getMongoCache(cacheKey);
                if (!mongoCached.isEmpty()) {
                    putLocalCache(cacheKey, mongoCached);
                    return deduplicateArticles(mongoCached);
                }

                List<NewsCacheDocument> fetched = fetchAndCacheNews(normalizedQuery, normalizedSort);
                if (!fetched.isEmpty()) {
                    putLocalCache(cacheKey, fetched);
                }
                return deduplicateArticles(fetched);
            }
        } finally {
            queryLocks.remove(cacheKey, queryLock);
        }
    }

    public List<NewsCacheDocument> refreshNews(String query, String sort) {
        // Refresh ignores existing caches and collects current API data again.
        String normalizedQuery = normalizeQuery(query);
        String normalizedSort = normalizeSort(sort);
        String cacheKey = buildCacheKey(normalizedQuery, normalizedSort);
        // Clear both local and MongoDB caches before fetching fresh data.
        localCache.remove(cacheKey);
        try {
            newsCacheRepository.deleteByQuery(cacheKey);
        } catch (RuntimeException e) {
            log.warn("Failed to clear Mongo news cache for cacheKey={}. Continuing without cache.", cacheKey, e);
        }

        List<NewsCacheDocument> fetched = fetchAndCacheNews(normalizedQuery, normalizedSort);
        if (!fetched.isEmpty()) {
            putLocalCache(cacheKey, fetched);
        }
        return deduplicateArticles(fetched);
    }

    public NewsPageDTO getNewsPage(String query, String category, String sort, int page, int pageSize) {
        String effectiveQuery = normalizeQuery(query);
        String effectiveCategory = normalizeCategory(category);
        String effectiveSort = normalizeSort(sort);

        List<NewsCacheDocument> deduplicatedNews = getNews(effectiveQuery, effectiveSort).stream()
                .filter(news -> matchesCategory(news, effectiveCategory))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                this::buildDisplayDedupKey,
                                news -> news,
                                this::pickPreferredArticle,
                                LinkedHashMap::new
                        ),
                        map -> map.values().stream().toList()
                ));

        List<NewsCacheDocument> newsList = deduplicatedNews.stream()
                .sorted(buildDisplaySortComparator(effectiveSort))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) newsList.size() / pageSize));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, newsList.size());
        int toIndex = Math.min(fromIndex + pageSize, newsList.size());
        List<NewsCacheDocument> pagedNewsList = newsList.subList(fromIndex, toIndex);

        return new NewsPageDTO(
                pagedNewsList,
                effectiveQuery,
                effectiveCategory,
                effectiveSort,
                currentPage,
                totalPages,
                newsList.size()
        );
    }

    public String normalizeNewsCategory(String category) {
        return normalizeCategory(category);
    }

    public String normalizeNewsSort(String sort) {
        return normalizeSort(sort);
    }

    public NewsCacheDocument getNewsDetail(String id) {
        NewsCacheDocument localCachedNews = getLocalCachedNewsById(id);
        if (localCachedNews != null) {
            return localCachedNews;
        }

        try {
            return newsCacheRepository.findById(id).orElse(null);
        } catch (RuntimeException e) {
            log.warn("Failed to load Mongo news detail for id={}.", id, e);
            return null;
        }
    }

    public NewsViewDTO getNewsDetailView(String id) {
        return toNewsDetailView(getNewsDetail(id));
    }

    public NewsViewDTO getNewsDetailView(
            String id,
            String title,
            String description,
            String pubDate,
            String originallink,
            String link
    ) {
        NewsCacheDocument news = getNewsDetail(id);

        if (news == null) {
            news = NewsCacheDocument.builder()
                    .id(id)
                    .title(title)
                    .description(description)
                    .pubDate(pubDate)
                    .originallink(originallink)
                    .link(link)
                    .build();
        }

        if (isBlank(news.getDescription())) {
            NewsCacheDocument foundByTitle = findCachedNewsByTitle(title);
            if (foundByTitle != null) {
                news = mergeNewsDetail(news, foundByTitle);
            }
        }

        if (isBlank(news.getDescription())) {
            news = enrichSummaryFromNewsApi(news);
        }

        return toNewsDetailView(news);
    }

    public NewsViewDTO toNewsDetailView(NewsCacheDocument news) {
        if (news == null) {
            return null;
        }

        String displayTitle = cleanNaverText(firstNonBlank(
                news.getTitle(),
                "\uC81C\uBAA9 \uC5C6\uC74C"
        ));

        String summarySource = firstNonBlank(
                news.getDescription(),
                news.getArticleContent()
        );

        String displaySummary = limitText(cleanNaverText(firstNonBlank(
                summarySource,
                "\uC694\uC57D \uC815\uBCF4\uAC00 \uC81C\uACF5\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4."
        )), 300);

        if (isBlank(displaySummary)) {
            displaySummary = "\uC694\uC57D \uC815\uBCF4\uAC00 \uC81C\uACF5\uB418\uC9C0 \uC54A\uC558\uC2B5\uB2C8\uB2E4.";
        }

        String originalUrl = firstNonBlank(
                news.getOriginallink(),
                news.getLink()
        );

        NewsViewDTO view = NewsViewDTO.builder()
                .id(news.getId())
                .displayTitle(displayTitle)
                .displaySummary(displaySummary)
                .displayProvider(cleanText(firstNonBlank(news.getProvider(), "\uB274\uC2A4 \uC11C\uBE44\uC2A4")))
                .displayDate(formatDate(news.getPubDate()))
                .originalUrl(originalUrl)
                .displayCategory(buildDisplayCategory(news))
                .displayTags(buildDisplayTags(news))
                .build();

        log.info("\uB274\uC2A4 \uC0C1\uC138 \uC81C\uBAA9={}", view.getDisplayTitle());
        log.info("\uB274\uC2A4 \uC694\uC57D \uAE38\uC774={}", view.getDisplaySummary() == null ? 0 : view.getDisplaySummary().length());
        log.info("\uB274\uC2A4 \uC6D0\uBB38 URL={}", view.getOriginalUrl());
        log.info("\uB274\uC2A4 \uC5B8\uB860\uC0AC={}", view.getDisplayProvider());
        log.info("\uB274\uC2A4 \uB0A0\uC9DC={}", view.getDisplayDate());

        return view;
    }

    private String buildNewsSearchUrl(String title) {
        if (isBlank(title)) {
            return "";
        }

        try {
            return "https://search.naver.com/search.naver?where=news&query="
                    + URLEncoder.encode(title, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return "";
        }
    }

    public NewsCacheDocument enrichNewsDetail(NewsCacheDocument news) {
        if (news == null) {
            return null;
        }

        // Enrich the detail view by fetching article body text when API summaries are not enough.
        String articleContent = fetchArticleContent(news);
        if (!isBlank(articleContent)) {
            news.setArticleContent(articleContent);
        }

        return news;
    }

    private NewsCacheDocument mergeNewsDetail(NewsCacheDocument base, NewsCacheDocument fallback) {
        if (base == null) {
            return fallback;
        }
        if (fallback == null) {
            return base;
        }

        base.setTitle(firstNonBlank(base.getTitle(), fallback.getTitle()));
        base.setDescription(firstNonBlank(base.getDescription(), fallback.getDescription()));
        base.setPubDate(firstNonBlank(base.getPubDate(), fallback.getPubDate()));
        base.setOriginallink(firstNonBlank(base.getOriginallink(), fallback.getOriginallink()));
        base.setLink(firstNonBlank(base.getLink(), fallback.getLink()));
        base.setProvider(firstNonBlank(base.getProvider(), fallback.getProvider()));
        return base;
    }

    private NewsCacheDocument findCachedNewsByTitle(String title) {
        if (isBlank(title)) {
            return null;
        }

        String normalizedTitle = normalize(title);

        for (CachedNewsBundle cached : localCache.values()) {
            if (cached == null || cached.newsList() == null) {
                continue;
            }
            for (NewsCacheDocument news : cached.newsList()) {
                if (news != null && normalize(news.getTitle()).equals(normalizedTitle)) {
                    return news;
                }
            }
        }

        try {
            return newsCacheRepository.findAll().stream()
                    .filter(news -> news != null && normalize(news.getTitle()).equals(normalizedTitle))
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("Failed to find cached news by title={}", title, e);
            return null;
        }
    }

    private NewsCacheDocument enrichSummaryFromNewsApi(NewsCacheDocument news) {
        if (news == null || isBlank(news.getTitle()) || isNewsApiMisconfigured()) {
            return news;
        }

        try {
            NewsApiDocument raw = null;
            for (String detailQuery : buildDetailSearchQueries(news.getTitle())) {
                List<NewsApiDocument> rawItems = requestNews(detailQuery, 0, SORT_DATE);
                log.info("News detail enrich API raw count={} detailQuery={} title={}",
                        rawItems.size(), detailQuery, news.getTitle());

                raw = pickBestDetailDocument(news.getTitle(), rawItems);
                if (raw != null && !isBlank(extractSummarySource(raw))) {
                    break;
                }
            }

            if (raw == null) {
                log.warn("News detail summary source not found. id={} title={}", news.getId(), news.getTitle());
                return news;
            }

            logRawDetailFields(raw);

            news.setTitle(firstNonBlank(news.getTitle(), raw.title()));
            news.setDescription(firstNonBlank(news.getDescription(), extractSummarySource(raw)));
            news.setProvider(firstNonBlank(news.getProvider(), raw.provider()));
            news.setPubDate(firstNonBlank(news.getPubDate(), raw.pubDate()));
            news.setOriginallink(firstNonBlank(
                    news.getOriginallink(),
                    raw.originalUrl(),
                    raw.link()
            ));
            news.setLink(firstNonBlank(news.getLink(), news.getOriginallink()));

            if (isBlank(extractSummarySource(raw))) {
                log.warn("News detail API document has no summary/content fields. id={} title={}", news.getId(), news.getTitle());
            }
        } catch (RuntimeException e) {
            log.warn("Failed to enrich news detail summary from API. id={} title={}", news.getId(), news.getTitle(), e);
        }

        return news;
    }

    private List<String> buildDetailSearchQueries(String title) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();
        String cleanedTitle = cleanText(firstNonBlank(title, ""));
        String normalizedTitle = NON_WORD_PATTERN.matcher(cleanedTitle).replaceAll(" ").replaceAll("\\s+", " ").trim();

        addDetailQuery(queries, cleanedTitle);
        addDetailQuery(queries, normalizedTitle);

        for (String token : normalizedTitle.split("\\s+")) {
            if (token.length() >= 2 && token.length() <= 12 && !isDetailStopWord(token)) {
                addDetailQuery(queries, token);
            }
        }

        String normalizedLower = normalize(normalizedTitle).toLowerCase(Locale.ROOT);
        if (normalizedLower.contains(normalize("\uB525\uD398\uC774\uD06C"))) {
            addDetailQuery(queries, "\uB525\uD398\uC774\uD06C");
        }
        if (normalizedLower.contains(normalize("\uB300\uD1B5\uB839"))) {
            addDetailQuery(queries, "\uB300\uD1B5\uB839");
        }
        if (normalizedLower.contains("ai") || normalizedLower.contains(normalize("\uC778\uACF5\uC9C0\uB2A5"))) {
            addDetailQuery(queries, "AI");
            addDetailQuery(queries, "\uC778\uACF5\uC9C0\uB2A5");
        }

        for (String keyword : NEWS_SEARCH_KEYWORDS) {
            addDetailQuery(queries, keyword);
            if (queries.size() >= 12) {
                break;
            }
        }

        return new ArrayList<>(queries);
    }

    private void addDetailQuery(Set<String> queries, String query) {
        if (isBlank(query)) {
            return;
        }

        String cleaned = cleanText(query)
                .replaceAll("[\\\"'`\\u201C\\u201D\\u2018\\u2019]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (!isBlank(cleaned) && cleaned.length() >= 2) {
            queries.add(cleaned);
        }
    }

    private boolean isDetailStopWord(String token) {
        String value = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return value.equals("\uBC95\uC801")
                || value.equals("\uB300\uC751")
                || value.equals("\uC601\uC0C1")
                || value.equals("\uAD00\uB828")
                || value.equals("\uB274\uC2A4")
                || value.equals("news")
                || value.equals("ai");
    }

    private NewsApiDocument pickBestDetailDocument(String title, List<NewsApiDocument> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            return null;
        }

        NewsApiDocument firstWithSummary = null;
        for (NewsApiDocument item : rawItems) {
            if (item == null) {
                continue;
            }
            String itemTitle = firstNonBlank(item.title());
            boolean hasSummary = !isBlank(extractSummarySource(item));
            if (hasSummary && isSameTitle(title, itemTitle)) {
                return item;
            }
            if (firstWithSummary == null && hasSummary) {
                firstWithSummary = item;
            }
        }

        return firstWithSummary == null ? rawItems.get(0) : firstWithSummary;
    }

    private boolean isSameTitle(String first, String second) {
        String normalizedFirst = normalize(first).replaceAll("\\s+", "");
        String normalizedSecond = normalize(second).replaceAll("\\s+", "");
        return !isBlank(normalizedFirst)
                && !isBlank(normalizedSecond)
                && (normalizedFirst.equals(normalizedSecond)
                || normalizedFirst.contains(normalizedSecond)
                || normalizedSecond.contains(normalizedFirst));
    }

    private String extractSummarySource(NewsApiDocument raw) {
        if (raw == null) {
            return "";
        }

        return firstNonBlank(
                raw.summary(),
                raw.description()
        );
    }

    private void logRawDetailFields(NewsApiDocument raw) {
        log.info("raw.title={}", raw.title());
        log.info("raw.description={}", raw.description());
        log.info("raw.link={}", raw.link());
    }

    private List<NewsCacheDocument> fetchAndCacheNews(String query, String sort) {

        if (isNewsApiMisconfigured()) {
            log.warn("Naver news API is not configured. clientId/clientSecret/newsUrl is missing.");
            return new ArrayList<>();
        }

        // Collect results across fallback keywords, filter irrelevant articles, and deduplicate by URL/title.
        String cacheKey = buildCacheKey(query, sort);
        Map<String, NewsCacheDocument> deduped = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusMinutes(360);
        int filteredOutCount = 0;
        int rawApiCount = 0;
        List<String> filteredOutTitles = new ArrayList<>();

        for (String candidateQuery : buildCandidateQueries(query)) {
            int keywordRawCount = 0;
            int keywordAcceptedCount = 0;
            int keywordFilteredCount = 0;

            for (int page = 0; page < NEWS_FETCH_PAGE_COUNT && deduped.size() < NEWS_MAX_FETCH_COUNT; page++) {
                // Naver API start offset increases by display page size.
                int start = page * NEWS_DISPLAY_COUNT;
                List<NewsApiDocument> items;

                try {
                    items = requestNews(candidateQuery, start, sort);
                } catch (HttpClientErrorException.TooManyRequests e) {
                    log.warn("News API rate limit hit for query={} sort={} candidateQuery={} start={}. Continuing with partial result.",
                            query, sort, candidateQuery, start);
                    break;
                } catch (RestClientException e) {
                    log.error("Failed to fetch news for query={} sort={} candidateQuery={} start={}",
                            query, sort, candidateQuery, start, e);
                    break;
                }

                rawApiCount += items.size();
                keywordRawCount += items.size();
                log.info("News API returned {} items for candidateQuery={} originalQuery={} sort={} start={}",
                        items.size(), candidateQuery, query, sort, start);

                if (items.isEmpty()) {
                    break;
                }

                for (NewsApiDocument item : items) {
                    String title = stripHtml(firstNonBlank(item.title()));
                    String description = stripHtml(extractSummarySource(item));
                    String normalizedTitle = normalize(title);
                    String normalizedDescription = normalize(description);

                    // Filter out unrelated articles and articles that are mostly English.
                    if (!isDeepfakeRelated(title, description, candidateQuery)
                            || isMostlyEnglishArticle(title, description)) {
                        filteredOutCount++;
                        keywordFilteredCount++;
                        if (filteredOutTitles.size() < 5) {
                            filteredOutTitles.add("title=" + normalizedTitle + " | description=" + normalizedDescription);
                        }
                        continue;
                    }

                    NewsCacheDocument doc = NewsCacheDocument.builder()
                            .id(UUID.randomUUID().toString())
                            .query(cacheKey)
                            .title(title)
                            .originallink(firstNonBlank(
                                    item.originalUrl(),
                                    item.link()
                            ))
                            .link(firstNonBlank(
                                    item.link(),
                                    item.originalUrl()
                            ))
                            .description(description)
                            .provider(item.provider())
                            .pubDate(item.pubDate())
                            .createdAt(now)
                            .expireAt(expireAt)
                            .build();

                    if (!deduped.containsKey(uniqueKey(doc))) {
                        keywordAcceptedCount++;
                    }
                    deduped.putIfAbsent(uniqueKey(doc), doc);
                }
            }

            log.info("News keyword summary originalQuery={} candidateQuery={} raw={} accepted={} filtered={} dedupedTotal={}",
                    query, candidateQuery, keywordRawCount, keywordAcceptedCount, keywordFilteredCount, deduped.size());
        }

        log.info("News collection summary query={} dateRange=all rawApiCount={} dedupedAfterUrlKey={} filteredOut={} filteredAfter={}",
                query, rawApiCount, deduped.size(), filteredOutCount, deduped.size());

        return persistFetchedNews(cacheKey, deduped, filteredOutCount, filteredOutTitles);
    }

    private String fetchArticleContent(NewsCacheDocument news) {
        // Try the original publisher URL first, then the Naver link if it is different.
        List<String> candidateUrls = new ArrayList<>();
        if (!isBlank(news.getOriginallink())) {
            candidateUrls.add(news.getOriginallink());
        }
        if (!isBlank(news.getLink()) && !candidateUrls.contains(news.getLink())) {
            candidateUrls.add(news.getLink());
        }

        for (String articleUrl : candidateUrls) {
            try {
                String html = requestArticleHtml(articleUrl);
                if (isBlank(html)) {
                    continue;
                }

                // Extract readable article text from likely body containers in the HTML.
                String extractedContent = extractArticleText(html, articleUrl);
                if (!isBlank(extractedContent)) {
                    log.info("Extracted article body from articleUrl={} with length={}", articleUrl, extractedContent.length());
                    return extractedContent;
                }

                log.info("No readable article body was extracted from articleUrl={}.", articleUrl);
            } catch (RestClientException e) {
                log.warn("Failed to fetch article body from articleUrl={}.", articleUrl, e);
            }
        }

        return null;
    }

    private String requestArticleHtml(String articleUrl) {
        // Decode article HTML using response headers and meta charset because publishers use mixed encodings.
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
        headers.set(HttpHeaders.REFERER, articleUrl);

        ResponseEntity<byte[]> responseEntity = restClient.get()
                .uri(URI.create(articleUrl))
                .headers(requestHeaders -> requestHeaders.addAll(headers))
                .retrieve()
                .toEntity(byte[].class);
        ArticleHttpResponse response = new ArticleHttpResponse(responseEntity.getBody(), responseEntity.getHeaders());

        byte[] body = response == null ? null : response.body();
        if (body == null || body.length == 0) {
            return null;
        }

        Charset charset = detectResponseCharset(response.headers(), body);
        return new String(body, charset);
    }

    private Charset detectResponseCharset(HttpHeaders headers, byte[] body) {
        Charset headerCharset = extractCharsetFromContentTypeHeaders(headers);
        if (headerCharset != null) {
            return headerCharset;
        }

        String asciiPreview = new String(body, StandardCharsets.ISO_8859_1);
        String charsetName = firstMatchedGroup(META_CHARSET_PATTERN, asciiPreview);
        if (isBlank(charsetName)) {
            charsetName = firstMatchedGroup(META_CONTENT_TYPE_CHARSET_PATTERN, asciiPreview);
        }

        if (!isBlank(charsetName)) {
            try {
                return Charset.forName(charsetName.trim());
            } catch (Exception e) {
                log.debug("Unsupported charset={} detected from article html. Falling back.", charsetName, e);
            }
        }

        if (looksLikeUtf8(body)) {
            return StandardCharsets.UTF_8;
        }

        return Charset.forName("MS949");
    }

    private Charset extractCharsetFromContentTypeHeaders(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }

        List<String> contentTypes = headers.get(HttpHeaders.CONTENT_TYPE);
        if (contentTypes == null) {
            return null;
        }

        for (String value : contentTypes) {
            if (isBlank(value)) {
                continue;
            }

            int charsetIndex = value.toLowerCase().indexOf("charset=");
            if (charsetIndex < 0) {
                continue;
            }

            String charsetName = value.substring(charsetIndex + 8).trim();
            int separatorIndex = charsetName.indexOf(';');
            if (separatorIndex >= 0) {
                charsetName = charsetName.substring(0, separatorIndex).trim();
            }
            charsetName = charsetName.replace("\"", "").replace("'", "").trim();

            if (charsetName.isEmpty()) {
                continue;
            }

            try {
                return Charset.forName(charsetName);
            } catch (Exception e) {
                log.debug("Unsupported charset={} detected from Content-Type header.", charsetName, e);
            }
        }

        return null;
    }

    private boolean looksLikeUtf8(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            int b = bytes[i] & 0xFF;
            if ((b & 0x80) == 0) {
                i++;
                continue;
            }

            int expectedLength;
            if ((b & 0xE0) == 0xC0) {
                expectedLength = 2;
            } else if ((b & 0xF0) == 0xE0) {
                expectedLength = 3;
            } else if ((b & 0xF8) == 0xF0) {
                expectedLength = 4;
            } else {
                return false;
            }

            if (i + expectedLength > bytes.length) {
                return false;
            }

            for (int j = 1; j < expectedLength; j++) {
                int next = bytes[i + j] & 0xFF;
                if ((next & 0xC0) != 0x80) {
                    return false;
                }
            }

            i += expectedLength;
        }

        return true;
    }

    private String extractArticleText(String html, String articleUrl) {
        if (isBlank(html)) {
            return null;
        }

        try {
            Document document = Jsoup.parse(html, articleUrl);
            document.select(REMOVABLE_SELECTOR).remove();

            String extractedFromDocument = extractWithJsoup(document);
            if (!isBlank(extractedFromDocument)) {
                return extractedFromDocument;
            }
        } catch (RuntimeException e) {
            log.debug("Failed jsoup article parsing for articleUrl={}", articleUrl, e);
        }

        String sanitizedHtml = SCRIPT_STYLE_PATTERN.matcher(html).replaceAll(" ");
        String articleHtml = firstMatchedGroup(ARTICLE_PATTERN, sanitizedHtml);

        if (isBlank(articleHtml)) {
            articleHtml = firstMatchedGroup(MAIN_PATTERN, sanitizedHtml);
        }

        if (isBlank(articleHtml)) {
            articleHtml = extractHintedContainer(sanitizedHtml);
        }

        if (isBlank(articleHtml)) {
            articleHtml = firstMatchedGroup(BODY_PATTERN, sanitizedHtml);
        }

        List<String> paragraphs = extractParagraphs(articleHtml);
        if (paragraphs.size() < 2) {
            paragraphs = extractParagraphs(extractHintedContainer(sanitizedHtml));
        }

        if (paragraphs.size() < 2) {
            paragraphs = extractParagraphs(firstMatchedGroup(BODY_PATTERN, sanitizedHtml));
        }

        if (paragraphs.size() < 2) {
            paragraphs = extractParagraphs(sanitizedHtml);
        }

        if (!paragraphs.isEmpty()) {
            return String.join("\n\n", paragraphs);
        }

        String metaDescription = firstMatchedGroup(META_DESCRIPTION_PATTERN, sanitizedHtml);
        String normalizedDescription = normalizeExtractedText(metaDescription);
        return normalizedDescription.length() >= 40 ? normalizedDescription : null;
    }

    private String extractWithJsoup(Document document) {
        List<String> candidates = new ArrayList<>();

        Elements articleElements = document.select(ARTICLE_SELECTOR);
        for (Element element : articleElements) {
            String candidate = extractTextFromElement(element);
            if (!isBlank(candidate)) {
                candidates.add(candidate);
            }
        }

        if (candidates.isEmpty() && document.body() != null) {
            String bodyCandidate = extractTextFromElement(document.body());
            if (!isBlank(bodyCandidate)) {
                candidates.add(bodyCandidate);
            }
        }

        String bestCandidate = candidates.stream()
                .filter(value -> value.length() >= 120)
                .max((left, right) -> Integer.compare(left.length(), right.length()))
                .orElse(null);

        if (!isBlank(bestCandidate)) {
            return bestCandidate;
        }

        String metaDescription = document.select("meta[property=og:description], meta[name=description]")
                .stream()
                .map(element -> element.attr("content"))
                .filter(value -> !isBlank(value))
                .findFirst()
                .orElse(null);

        String normalizedDescription = normalizeExtractedText(metaDescription);
        return normalizedDescription.length() >= 40 ? normalizedDescription : null;
    }

    private String extractTextFromElement(Element root) {
        if (root == null) {
            return null;
        }

        Element cloned = root.clone();
        cloned.select(REMOVABLE_SELECTOR).remove();

        LinkedHashSet<String> paragraphs = new LinkedHashSet<>();
        for (Element paragraph : cloned.select("p, li")) {
            String text = normalizeExtractedText(paragraph.text());
            if (isMeaningfulParagraph(text)) {
                paragraphs.add(text);
            }
        }

        if (paragraphs.size() >= 2) {
            return String.join("\n\n", paragraphs);
        }

        String wholeText = normalizeExtractedText(cloned.text());
        return isMeaningfulBlock(wholeText) ? wholeText : null;
    }

    private boolean isMeaningfulParagraph(String text) {
        if (isBlank(text) || text.length() < 30) {
            return false;
        }
        return !isBoilerplateLine(text);
    }

    private boolean isMeaningfulBlock(String text) {
        if (isBlank(text) || text.length() < 120) {
            return false;
        }
        return !isBoilerplateLine(text);
    }

    private boolean isBoilerplateLine(String text) {
        String normalized = normalizeExtractedText(text);
        if (normalized.isBlank()) {
            return true;
        }
        if (EMAIL_PATTERN.matcher(normalized).matches()) {
            return true;
        }
        return REPORTER_PATTERN.matcher(normalized).matches() && normalized.length() < 80;
    }

    private String extractHintedContainer(String html) {
        if (isBlank(html)) {
            return null;
        }

        for (String hint : ARTICLE_CONTAINER_HINTS) {
            String pattern = "(?is)<([a-z0-9]+)[^>]*(?:id|class)=[\"'][^\"']*" + Pattern.quote(hint) + "[^\"']*[\"'][^>]*>(.*?)</\\1>";
            String matched = firstMatchedGroup(Pattern.compile(pattern), html);
            if (!isBlank(matched)) {
                return matched;
            }
        }

        return null;
    }

    private List<String> extractParagraphs(String html) {
        if (isBlank(html)) {
            return List.of();
        }

        LinkedHashSet<String> paragraphs = new LinkedHashSet<>();
        Matcher matcher = PARAGRAPH_PATTERN.matcher(html);

        while (matcher.find()) {
            String paragraph = normalizeExtractedText(matcher.group(1));
            if (paragraph.length() >= 40) {
                paragraphs.add(paragraph);
            }
        }

        if (!paragraphs.isEmpty()) {
            return new ArrayList<>(paragraphs);
        }

        String normalizedBlock = normalizeExtractedText(html);
        if (normalizedBlock.length() >= 80) {
            paragraphs.add(normalizedBlock);
        }

        return new ArrayList<>(paragraphs);
    }

    private String normalizeExtractedText(String htmlFragment) {
        if (isBlank(htmlFragment)) {
            return "";
        }

        String withLineBreaks = BLOCK_BREAK_PATTERN.matcher(htmlFragment).replaceAll("\n");
        String withoutTags = TAG_PATTERN.matcher(withLineBreaks).replaceAll(" ");
        String unescaped = HtmlUtils.htmlUnescape(withoutTags)
                .replace('\u00A0', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n\n")
                .trim();

        return unescaped;
    }

    private String firstMatchedGroup(Pattern pattern, String value) {
        if (isBlank(value)) {
            return null;
        }

        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private boolean isNewsApiMisconfigured() {
        return isBlank(naverClientId) || isBlank(naverClientSecret) || isBlank(naverNewsUrl);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }

        return "";
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = SCRIPT_STYLE_PATTERN.matcher(value).replaceAll(" ");
        cleaned = TAG_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = HtmlUtils.htmlUnescape(cleaned);
        cleaned = cleaned.replace('\u00A0', ' ');
        cleaned = cleaned.replace("&quot;", "\"");
        cleaned = cleaned.replace("&amp;", "&");
        cleaned = cleaned.replace("&lt;", "<");
        cleaned = cleaned.replace("&gt;", ">");
        cleaned = cleaned.replace("&nbsp;", " ");
        cleaned = cleaned.replaceAll("[\\r\\n]+", "\n");
        cleaned = cleaned.replaceAll("[ \\t\\x0B\\f]+", " ");
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        return removeSummaryNoise(cleaned).trim();
    }

    private String cleanNaverText(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value;
        cleaned = cleaned.replaceAll("(?i)</?b>", "");
        cleaned = cleaned.replaceAll("(?is)<script.*?>.*?</script>", " ");
        cleaned = cleaned.replaceAll("(?is)<style.*?>.*?</style>", " ");
        cleaned = cleaned.replaceAll("<[^>]*>", " ");
        cleaned = cleaned.replace("&quot;", "\"");
        cleaned = cleaned.replace("&amp;", "&");
        cleaned = cleaned.replace("&lt;", "<");
        cleaned = cleaned.replace("&gt;", ">");
        cleaned = cleaned.replace("&#39;", "'");
        cleaned = cleaned.replace("&apos;", "'");
        cleaned = cleaned.replace("&nbsp;", " ");
        cleaned = cleaned.replaceAll("[\\r\\n]+", " ");
        cleaned = cleaned.replaceAll("\\s{2,}", " ");
        return cleaned.trim();
    }

    private String removeSummaryNoise(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String line : value.split("\\R")) {
            String trimmed = line == null ? "" : line.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (startsWithMetadataLabel(lower)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private boolean startsWithMetadataLabel(String value) {
        if (value == null) {
            return false;
        }

        return value.startsWith("title:")
                || value.startsWith("provider:")
                || value.startsWith("publisher:")
                || value.startsWith("press:")
                || value.startsWith("date:")
                || value.startsWith("published_at:")
                || value.startsWith("pubdate:")
                || value.startsWith("\uC81C\uBAA9:")
                || value.startsWith("\uC5B8\uB860\uC0AC:")
                || value.startsWith("\uB0A0\uC9DC:")
                || value.startsWith("\uBC1C\uD589\uC77C:");
    }

    private String limitText(String value, int maxLength) {
        if (isBlank(value)) {
            return "";
        }

        String cleaned = value.trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        String cut = cleaned.substring(0, maxLength);
        int lastDot = Math.max(
                Math.max(cut.lastIndexOf("."), cut.lastIndexOf("\uB2E4.")),
                cut.lastIndexOf("\uC694.")
        );

        if (lastDot > 80) {
            return cut.substring(0, lastDot + 1).trim();
        }

        return cut.trim() + "...";
    }

    private String formatDate(String value) {
        if (isBlank(value)) {
            return "";
        }

        String trimmed = value.trim();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        try {
            return ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH))
                    .format(outputFormatter);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(trimmed).format(outputFormatter);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDate.parse(trimmed).format(outputFormatter);
        } catch (DateTimeParseException ignored) {
        }

        String digits = trimmed.replaceAll("[^0-9]", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 4) + "." + digits.substring(4, 6) + "." + digits.substring(6, 8);
        }

        return cleanText(trimmed);
    }

    private String buildDisplayCategory(NewsCacheDocument news) {
        if (news == null) {
            return "\uB300\uC751\uBC29\uBC95";
        }

        String text = normalize(firstNonBlank(news.getTitle(), news.getDescription()));
        if (containsAny(text, "\uBC94\uC8C4", "\uC218\uC0AC", "\uAC80\uAC70", "\uCC98\uBC8C", "\uD53C\uD574")) {
            return "\uBC94\uC8C4\uC0AC\uB840";
        }
        if (containsAny(text, "\uBC95", "\uADDC\uC81C", "\uCC98\uBC8C", "\uC785\uBC95", "\uAC1C\uC815")) {
            return "\uBC95\uB960/\uADDC\uC81C";
        }
        if (containsAny(text, "\uC608\uBC29", "\uB300\uC751", "\uC2E0\uACE0", "\uBCF4\uD638", "\uC8FC\uC758")) {
            return "\uB300\uC751\uBC29\uBC95";
        }
        if (containsAny(text, "\uD53C\uD574", "\uC720\uD3EC", "\uC0AC\uCE6D", "\uD611\uBC15")) {
            return "\uD53C\uD574\uC0AC\uB840";
        }
        return "\uAE30\uC220\uB3D9\uD5A5";
    }

    private List<String> buildDisplayTags(NewsCacheDocument news) {
        String text = normalize(news == null ? "" : firstNonBlank(news.getTitle(), news.getDescription()));
        List<String> tags = new ArrayList<>();

        addTagIfMatched(tags, text, "#\uB525\uD398\uC774\uD06C", "\uB525\uD398\uC774\uD06C", "deepfake", "deep fake");
        addTagIfMatched(tags, text, "#AI", "ai", "\uC778\uACF5\uC9C0\uB2A5", "\uC0DD\uC131\uD615");
        addTagIfMatched(tags, text, "#\uC601\uC0C1\uC870\uC791", "\uC601\uC0C1", "\uC870\uC791", "\uD569\uC131");
        addTagIfMatched(tags, text, "#\uAC00\uC9DC\uB274\uC2A4", "\uAC00\uC9DC\uB274\uC2A4", "\uD5C8\uC704");
        addTagIfMatched(tags, text, "#\uD53C\uD574\uC608\uBC29", "\uC608\uBC29", "\uB300\uC751", "\uC2E0\uACE0", "\uBCF4\uD638");

        if (tags.isEmpty()) {
            tags.add("#\uB525\uD398\uC774\uD06C");
            tags.add("#AI");
        }

        return tags.stream().limit(5).toList();
    }

    private void addTagIfMatched(List<String> tags, String text, String tag, String... keywords) {
        if (!tags.contains(tag) && containsAny(text, keywords)) {
            tags.add(tag);
        }
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || keywords == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (!isBlank(keyword) && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private List<String> buildCandidateQueries(String query) {
        List<String> candidates = new ArrayList<>();
        if (!isBlank(query)) {
            candidates.add(query);
        }
        for (String keyword : NEWS_SEARCH_KEYWORDS) {
            if (!candidates.contains(keyword)) {
                candidates.add(keyword);
            }
        }

        return candidates;
    }

    private List<NewsApiDocument> requestNews(String query, int start, String sort) {
        return requestNaverNews(query, start, sort);
    }

    private List<NewsApiDocument> requestNaverNews(String query, int start, String sort) {
        if (isBlank(naverClientId) || isBlank(naverClientSecret)) {
            log.warn("Naver news API credentials are missing. query={}", query);
            return List.of();
        }

        URI uri = UriComponentsBuilder.fromUriString(naverNewsUrl)
                .queryParam("query", query)
                .queryParam("display", NEWS_DISPLAY_COUNT)
                .queryParam("start", start + 1)
                .queryParam("sort", SORT_DATE.equalsIgnoreCase(sort) ? "date" : "sim")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-Naver-Client-Id", naverClientId);
        headers.set("X-Naver-Client-Secret", naverClientSecret);

        log.info("Naver news query={}", query);
        ResponseEntity<NaverNewsResponseDTO> response = restClient.get()
                .uri(uri)
                .headers(requestHeaders -> requestHeaders.addAll(headers))
                .retrieve()
                .toEntity(NaverNewsResponseDTO.class);
        log.info("Naver news response status={}", response.getStatusCode());

        List<NaverNewsItemDTO> items = response.getBody() == null || response.getBody().getItems() == null
                ? List.of()
                : response.getBody().getItems();
        log.info("Naver news items count={}", items.size());

        List<NewsApiDocument> documents = new ArrayList<>();
        for (NaverNewsItemDTO item : items) {
            if (item == null) {
                continue;
            }

            log.info("Naver raw title={}", item.getTitle());
            log.info("Naver raw description={}", item.getDescription());

            documents.add(new NewsApiDocument(
                    cleanNaverText(item.getTitle()),
                    cleanNaverText(item.getDescription()),
                    cleanNaverText(item.getDescription()),
                    firstNonBlank(item.getOriginallink(), item.getLink()),
                    item.getLink(),
                    "",
                    item.getPubDate()
            ));
        }

        return documents;
    }

    private boolean isDeepfakeRelated(String title, String description, String query) {
        String normalizedText = normalize(title) + " " + normalize(description);
        String normalizedQuery = normalize(query);

        if (!normalizedQuery.isBlank() && normalizedText.contains(normalizedQuery)) {
            return true;
        }

        for (String keyword : NEWS_SEARCH_KEYWORDS) {
            if (normalizedText.contains(normalize(keyword))) {
                return true;
            }
        }

        return false;
    }

    private boolean isMostlyEnglishArticle(String title, String description) {
        String text = (stripHtml(title) + " " + stripHtml(description)).trim();
        if (text.isEmpty()) {
            return false;
        }

        int englishLetters = 0;
        int koreanLetters = 0;

        for (char ch : text.toCharArray()) {
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
                englishLetters++;
            } else if (ch >= 0xAC00 && ch <= 0xD7A3) {
                koreanLetters++;
            }
        }

        if (englishLetters < 20) {
            return false;
        }

        return englishLetters > koreanLetters * 2;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return HtmlUtils.htmlUnescape(text)
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String stripHtml(String text) {
        if (text == null) return "";
        return HtmlUtils.htmlUnescape(text.replaceAll("<[^>]*>", "")).trim();
    }

    private String uniqueKey(NewsCacheDocument doc) {
        if (!isBlank(doc.getOriginallink())) {
            return "origin:" + doc.getOriginallink().trim();
        }
        if (!isBlank(doc.getLink())) {
            return "link:" + doc.getLink().trim();
        }
        return "title:" + normalize(doc.getTitle())
                + "|provider:" + normalize(firstNonBlank(doc.getDescription(), ""))
                + "|date:" + normalize(doc.getPubDate());
    }

    private String normalizeQuery(String query) {
        return isBlank(query) ? "\uB525\uD398\uC774\uD06C" : query.trim();
    }

    private String normalizeSort(String sort) {
        return SORT_DATE.equalsIgnoreCase(sort) ? SORT_DATE : SORT_SIM;
    }

    private String normalizeCategory(String category) {
        return isBlank(category) ? "전체" : category.trim();
    }

    private Comparator<NewsCacheDocument> buildDisplaySortComparator(String sort) {
        if (SORT_DATE.equals(sort)) {
            return Comparator
                    .comparing(this::parseDisplayPubDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed()
                    .thenComparing(NewsCacheDocument::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(NewsCacheDocument::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private ZonedDateTime parseDisplayPubDate(NewsCacheDocument news) {
        if (news == null || isBlank(news.getPubDate())) {
            return fallbackDisplayDate(news);
        }
        try {
            return ZonedDateTime.parse(
                    news.getPubDate().trim(),
                    DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH)
            );
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(news.getPubDate().trim()).toZonedDateTime();
            } catch (DateTimeParseException ignored) {
                return fallbackDisplayDate(news);
            }
        }
    }

    private ZonedDateTime fallbackDisplayDate(NewsCacheDocument news) {
        LocalDateTime createdAt = news == null ? null : news.getCreatedAt();
        return createdAt == null ? null : createdAt.atZone(java.time.ZoneId.systemDefault());
    }

    private boolean matchesCategory(NewsCacheDocument news, String selectedCategory) {
        if ("전체".equals(selectedCategory)) {
            return true;
        }
        return selectedCategory.equals(detectCategory(news));
    }

    private String buildDisplayDedupKey(NewsCacheDocument news) {
        String title = news == null || news.getTitle() == null ? "" : news.getTitle();
        String normalized = title.toLowerCase(Locale.ROOT)
                .replaceAll("^\\[[^\\]]+\\]\\s*", "")
                .replaceAll("\\b(속보|단독|종합|영상|포토|1보|2보|3보)\\b", " ")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.length() > 44) {
            normalized = normalized.substring(0, 44).trim();
        }

        if (!normalized.isBlank()) {
            return normalized;
        }

        return title.isBlank() ? String.valueOf(news == null ? "" : news.getId()) : title.trim();
    }

    private NewsCacheDocument pickPreferredArticle(NewsCacheDocument first, NewsCacheDocument second) {
        ZonedDateTime firstDate = parseDisplayPubDate(first);
        ZonedDateTime secondDate = parseDisplayPubDate(second);

        if (firstDate == null && secondDate == null) {
            return preferRicherArticle(first, second);
        }
        if (firstDate == null) {
            return second;
        }
        if (secondDate == null) {
            return first;
        }

        if (secondDate.isAfter(firstDate)) {
            return second;
        }
        if (firstDate.isAfter(secondDate)) {
            return first;
        }

        return preferRicherArticle(first, second);
    }

    private NewsCacheDocument preferRicherArticle(NewsCacheDocument first, NewsCacheDocument second) {
        int firstScore = articleRichness(first);
        int secondScore = articleRichness(second);
        return secondScore > firstScore ? second : first;
    }

    private int articleRichness(NewsCacheDocument news) {
        if (news == null) {
            return 0;
        }

        int score = 0;
        if (news.getTitle() != null) {
            score += news.getTitle().length();
        }
        if (news.getDescription() != null) {
            score += Math.min(news.getDescription().length(), 200);
        }
        if (news.getOriginallink() != null && !news.getOriginallink().isBlank()) {
            score += 20;
        }
        return score;
    }

    private String detectCategory(NewsCacheDocument news) {
        String text = ((news.getTitle() == null ? "" : news.getTitle()) + " " +
                (news.getDescription() == null ? "" : news.getDescription())).toLowerCase(Locale.ROOT);

        Map<String, List<String>> keywordMap = Map.of(
                "범죄사례", List.of("범죄", "검거", "체포", "사건", "성착취", "처벌", "사기", "경찰"),
                "법률/규제", List.of("법률", "규제", "법안", "입법", "개정", "시행", "관련법", "금지"),
                "기술동향", List.of("기술", "ai", "모델", "연구", "개발", "시스템", "업그레이드"),
                "피해사례", List.of("피해", "협박", "유출", "사칭", "폭로", "고통", "불안", "창피"),
                "대응방법", List.of("대응", "예방", "경고", "가이드", "방법", "체크리스트", "보호", "수칙")
        );

        String bestCategory = "기술동향";
        int bestScore = 0;

        for (Map.Entry<String, List<String>> entry : keywordMap.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestCategory = entry.getKey();
            }
        }

        return bestCategory;
    }

    private String buildCacheKey(String query, String sort) {
        return NEWS_CACHE_VERSION + "::" + query + "::" + normalizeSort(sort);
    }

    private String extractOriginalQuery(String cacheKey) {
        if (isBlank(cacheKey)) {
            return normalizeQuery(null);
        }

        if (cacheKey.startsWith(NEWS_CACHE_VERSION + "::")) {
            cacheKey = cacheKey.substring((NEWS_CACHE_VERSION + "::").length());
        }

        int separatorIndex = cacheKey.lastIndexOf("::");
        if (separatorIndex < 0) {
            return cacheKey;
        }

        return cacheKey.substring(0, separatorIndex);
    }

    private List<NewsCacheDocument> getMongoCache(String cacheKey) {
        // Reuse MongoDB cache only when cached articles still match the current query.
        // If the stored cache is stale or irrelevant, clear it and fall back to live API data.
        String originalQuery = extractOriginalQuery(cacheKey);
        try {
            List<NewsCacheDocument> cached = newsCacheRepository.findByQueryOrderByCreatedAtDesc(cacheKey);
            if (cached == null || cached.isEmpty()) {
                return List.of();
            }

            Map<String, NewsCacheDocument> filteredCached = new LinkedHashMap<>();
            for (NewsCacheDocument news : cached) {
                if (isDeepfakeRelated(news.getTitle(), news.getDescription(), originalQuery)
                        && !isMostlyEnglishArticle(news.getTitle(), news.getDescription())) {
                    filteredCached.putIfAbsent(uniqueKey(news), news);
                }
            }

            if (!filteredCached.isEmpty()) {
                return deduplicateArticles(new ArrayList<>(filteredCached.values()));
            }

            newsCacheRepository.deleteByQuery(cacheKey);
        } catch (RuntimeException e) {
            log.warn("Failed to read Mongo news cache for cacheKey={}. Falling back to local/live cache.", cacheKey, e);
        }

        return List.of();
    }

    private List<NewsCacheDocument> getLocalCache(String query) {
        CachedNewsBundle cached = localCache.get(query);
        if (cached == null) {
            return List.of();
        }
        if (cached.isExpired()) {
            localCache.remove(query);
            return List.of();
        }
        return deduplicateArticles(new ArrayList<>(cached.newsList()));
    }

    private void putLocalCache(String query, List<NewsCacheDocument> newsList) {
        if (newsList == null || newsList.isEmpty()) {
            return;
        }
        localCache.put(query, new CachedNewsBundle(
                List.copyOf(newsList),
                LocalDateTime.now().plusMinutes(LOCAL_CACHE_MINUTES)
        ));
    }

    private NewsCacheDocument getLocalCachedNewsById(String id) {
        if (isBlank(id)) {
            return null;
        }

        for (CachedNewsBundle cached : localCache.values()) {
            if (cached == null || cached.isExpired()) {
                continue;
            }

            for (NewsCacheDocument news : cached.newsList()) {
                if (news != null && id.equals(news.getId())) {
                    return news;
                }
            }
        }

        return null;
    }

    private List<NewsCacheDocument> persistFetchedNews(
            String query,
            Map<String, NewsCacheDocument> deduped,
            int filteredOutCount,
            List<String> filteredOutTitles
    ) {
        List<NewsCacheDocument> result = deduplicateArticles(new ArrayList<>(deduped.values()));
        int deduplicatedCount = result.size();
        result = sortByLatest(result).stream()
                .limit(NEWS_DISPLAY_COUNT)
                .toList();

        if (!result.isEmpty()) {
            try {
                newsCacheRepository.saveAll(result);
            } catch (RuntimeException e) {
                log.warn("Failed to save Mongo news cache for query={}. Returning uncached result.", query, e);
            }
        }

        log.info("Saved {} news items for query={} (deduplicated={}, filteredOut={}, displayLimit={})",
                result.size(), query, deduplicatedCount, filteredOutCount, NEWS_DISPLAY_COUNT);
        if (result.isEmpty() && !filteredOutTitles.isEmpty()) {
            log.info("Sample filtered titles for query={}: {}", query, filteredOutTitles);
        }

        return result;
    }

    private List<NewsCacheDocument> sortByLatest(List<NewsCacheDocument> articles) {
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }

        return articles.stream()
                .sorted((first, second) -> normalizeDateSortKey(second.getPubDate())
                        .compareTo(normalizeDateSortKey(first.getPubDate())))
                .toList();
    }

    private String normalizeDateSortKey(String value) {
        if (isBlank(value)) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }

    private List<NewsCacheDocument> deduplicateArticles(List<NewsCacheDocument> articles) {
        // Remove duplicate articles using normalized title keys and Jaccard token similarity.
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }

        List<NewsCacheDocument> uniqueArticles = new ArrayList<>();
        Set<String> seenTitleKeys = new HashSet<>();

        for (NewsCacheDocument candidate : articles) {
            if (candidate == null) {
                continue;
            }

            String titleKey = buildTitleKey(candidate.getTitle());
            if (!titleKey.isBlank() && seenTitleKeys.contains(titleKey)) {
                continue;
            }

            boolean duplicate = false;
            for (NewsCacheDocument existing : uniqueArticles) {
                if (isSimilarArticle(existing, candidate)) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                uniqueArticles.add(candidate);
                if (!titleKey.isBlank()) {
                    seenTitleKeys.add(titleKey);
                }
            }
        }

        return uniqueArticles;
    }

    private boolean isSimilarArticle(NewsCacheDocument first, NewsCacheDocument second) {
        if (first == null || second == null) {
            return false;
        }

        String firstTitleKey = buildTitleKey(first.getTitle());
        String secondTitleKey = buildTitleKey(second.getTitle());
        if (!firstTitleKey.isBlank() && firstTitleKey.equals(secondTitleKey)) {
            return true;
        }

        Set<String> firstTitleTokens = tokenizeTitle(first.getTitle());
        Set<String> secondTitleTokens = tokenizeTitle(second.getTitle());
        double titleSimilarity = calculateJaccardSimilarity(firstTitleTokens, secondTitleTokens);

        if (titleSimilarity >= 0.8d) {
            return true;
        }

        if (!firstTitleKey.isBlank()
                && !secondTitleKey.isBlank()
                && (firstTitleKey.contains(secondTitleKey) || secondTitleKey.contains(firstTitleKey))
                && Math.min(firstTitleKey.length(), secondTitleKey.length()) >= 12) {
            return true;
        }

        Set<String> firstDescriptionTokens = tokenizeDescription(first.getDescription());
        Set<String> secondDescriptionTokens = tokenizeDescription(second.getDescription());
        double descriptionSimilarity = calculateJaccardSimilarity(firstDescriptionTokens, secondDescriptionTokens);

        return titleSimilarity >= 0.6d && descriptionSimilarity >= 0.55d;
    }

    private String buildTitleKey(String title) {
        String normalizedTitle = normalize(title);
        if (normalizedTitle.isBlank()) {
            return "";
        }

        String cleaned = normalizedTitle;
        while (true) {
            String next = LEADING_BRACKET_PATTERN.matcher(cleaned).replaceFirst("");
            if (next.equals(cleaned)) {
                break;
            }
            cleaned = next.trim();
        }

        cleaned = TITLE_NOISE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = NON_WORD_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned;
    }

    private Set<String> tokenizeTitle(String title) {
        return tokenize(buildTitleKey(title), 2);
    }

    private Set<String> tokenizeDescription(String description) {
        return tokenize(normalize(description), 3);
    }

    private Set<String> tokenize(String text, int minTokenLength) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        for (String token : text.split("\\s+")) {
            String value = token.trim();
            if (value.length() >= minTokenLength) {
                tokens.add(value);
            }
        }
        return tokens;
    }

    private double calculateJaccardSimilarity(Set<String> first, Set<String> second) {
        if (first.isEmpty() || second.isEmpty()) {
            return 0d;
        }

        Set<String> intersection = new HashSet<>(first);
        intersection.retainAll(second);

        Set<String> union = new HashSet<>(first);
        union.addAll(second);

        if (union.isEmpty()) {
            return 0d;
        }

        return (double) intersection.size() / union.size();
    }

    private record CachedNewsBundle(List<NewsCacheDocument> newsList, LocalDateTime expireAt) {
        private boolean isExpired() {
            return expireAt == null || LocalDateTime.now().isAfter(expireAt);
        }
    }
}

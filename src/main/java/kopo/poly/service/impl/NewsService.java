package kopo.poly.service.impl;

import kopo.poly.document.NewsCacheDocument;
import kopo.poly.dto.NaverNewsResponseDTO;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 네이버 뉴스 검색, 기사 본문 추출, 중복 제거, 캐시 저장을 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {
    // 네이버 뉴스 API sort 파라미터와 동일한 값이다.
    public static final String SORT_SIM = "sim";
    public static final String SORT_DATE = "date";

    // 화면에는 30개만 보여주지만 중복 제거를 위해 API에서는 조금 더 넉넉히 가져온다.
    private static final int NEWS_DISPLAY_COUNT = 30;
    private static final int NEWS_MAX_FETCH_COUNT = 60;
    private static final int NEWS_FETCH_PAGE_COUNT = 2;
    private static final int LOCAL_CACHE_MINUTES = 10;
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
    private static final Pattern TITLE_NOISE_PATTERN = Pattern.compile("(?:\\b(?:속보|단독|종합|1보|2보|3보|영상|포토)\\b|[\"'`“”‘’]|\\.{2,}|…)");
    private static final Pattern NON_WORD_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}\\s]");
    private static final Pattern REPORTER_PATTERN = Pattern.compile(".*(?:기자|특파원|뉴스팀|연합뉴스|뉴시스|뉴스1|메일|무단전재|재배포).*");
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
            "딥페이크",
            "딥 페이크",
            "deepfake",
            "deep fake",
            "가짜 영상",
            "합성 영상",
            "조작 영상",
            "ai 조작"
    );

    private final RestTemplate restTemplate;
    private final NewsCacheRepository newsCacheRepository;
    // 짧은 시간 안의 같은 검색은 MongoDB까지 가지 않도록 애플리케이션 메모리에 한 번 더 캐시한다.
    private final Map<String, CachedNewsBundle> localCache = new ConcurrentHashMap<>();
    // 같은 검색어가 동시에 들어올 때 API를 여러 번 호출하지 않도록 검색어별 락을 둔다.
    private final Map<String, Object> queryLocks = new ConcurrentHashMap<>();

    @Value("${naver.search.client-id:}")
    private String clientId;

    @Value("${naver.search.client-secret:}")
    private String clientSecret;

    @Value("${naver.search.news-url:https://openapi.naver.com/v1/search/news.json}")
    private String newsUrl;

    private record ArticleHttpResponse(byte[] body, HttpHeaders headers) {
    }

    public List<NewsCacheDocument> getNews(String query, String sort) {
        String normalizedQuery = normalizeQuery(query);
        String normalizedSort = normalizeSort(sort);
        String cacheKey = buildCacheKey(normalizedQuery, normalizedSort);

        // 1차 캐시: 메모리 캐시로 반복적인 API/MongoDB 조회를 줄인다.
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

                // 2차 캐시: MongoDB 캐시는 앱을 재시작해도 뉴스 결과를 유지한다.
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
        String normalizedQuery = normalizeQuery(query);
        String normalizedSort = normalizeSort(sort);
        String cacheKey = buildCacheKey(normalizedQuery, normalizedSort);
        // 새로고침은 로컬 캐시와 MongoDB 캐시를 비우고 네이버 API에서 다시 받아온다.
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

    public NewsCacheDocument enrichNewsDetail(NewsCacheDocument news) {
        if (news == null) {
            return null;
        }

        // 상세 화면에 들어왔을 때 검색 API 설명문보다 긴 원문 본문을 읽어 보강한다.
        String articleContent = fetchArticleContent(news);
        if (!isBlank(articleContent)) {
            news.setArticleContent(articleContent);
        }

        return news;
    }

    private List<NewsCacheDocument> fetchAndCacheNews(String query, String sort) {

        if (isNewsApiMisconfigured()) {
            log.warn("News API is not configured. clientId/clientSecret/newsUrl is missing.");
            return new ArrayList<>();
        }

        // 네이버 검색 API 결과를 가져온 뒤 딥페이크 관련 기사만 남기고 MongoDB 캐시에 저장한다.
        String cacheKey = buildCacheKey(query, sort);
        Map<String, NewsCacheDocument> deduped = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusMinutes(30);
        int filteredOutCount = 0;
        List<String> filteredOutTitles = new ArrayList<>();

        for (String candidateQuery : buildCandidateQueries(query)) {
            for (int page = 0; page < NEWS_FETCH_PAGE_COUNT && deduped.size() < NEWS_MAX_FETCH_COUNT; page++) {
                // 네이버 뉴스 API는 start 값으로 페이지를 나누어 호출한다.
                int start = page * NEWS_DISPLAY_COUNT + 1;
                List<NaverNewsResponseDTO.Item> items;

                try {
                    items = requestNews(candidateQuery, start, sort);
                } catch (HttpClientErrorException.TooManyRequests e) {
                    log.warn("News API rate limit hit for query={} sort={} candidateQuery={} start={}. Returning partial result.",
                            query, sort, candidateQuery, start);
                    return persistFetchedNews(cacheKey, deduped, filteredOutCount, filteredOutTitles);
                } catch (RestClientException e) {
                    log.error("Failed to fetch news for query={} sort={} candidateQuery={} start={}",
                            query, sort, candidateQuery, start, e);
                    return persistFetchedNews(cacheKey, deduped, filteredOutCount, filteredOutTitles);
                }

                log.info("News API returned {} items for candidateQuery={} originalQuery={} sort={} start={}",
                        items.size(), candidateQuery, query, sort, start);

                for (NaverNewsResponseDTO.Item item : items) {
                    String title = stripHtml(item.getTitle());
                    String description = stripHtml(item.getDescription());
                    String normalizedTitle = normalize(title);
                    String normalizedDescription = normalize(description);

                    // 뉴스 화면에는 한국어 딥페이크 관련 기사만 남긴다.
                    if (!isDeepfakeRelated(title, description, query)
                            || isMostlyEnglishArticle(title, description)) {
                        filteredOutCount++;
                        if (filteredOutTitles.size() < 5) {
                            filteredOutTitles.add("title=" + normalizedTitle + " | description=" + normalizedDescription);
                        }
                        continue;
                    }

                    NewsCacheDocument doc = NewsCacheDocument.builder()
                            .id(UUID.randomUUID().toString())
                            .query(cacheKey)
                            .title(title)
                            .originallink(item.getOriginallink())
                            .link(item.getLink())
                            .description(description)
                            .pubDate(item.getPubDate())
                            .createdAt(now)
                            .expireAt(expireAt)
                            .build();

                    deduped.putIfAbsent(uniqueKey(doc), doc);
                }
            }
        }

        return persistFetchedNews(cacheKey, deduped, filteredOutCount, filteredOutTitles);
    }

    private String fetchArticleContent(NewsCacheDocument news) {
        // 원문 링크가 실패하면 네이버 링크도 후보로 사용한다.
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

                // 언론사 페이지 HTML에서 기사 본문 후보 영역만 추출한다.
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
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
        headers.set(HttpHeaders.REFERER, articleUrl);

        RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, URI.create(articleUrl));
        ResponseExtractor<ArticleHttpResponse> extractor = response -> {
            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((key, values) -> responseHeaders.put(key, new ArrayList<>(values)));
            return new ArticleHttpResponse(readAllBytes(response.getBody()), responseHeaders);
        };

        ArticleHttpResponse response = restTemplate.execute(
                requestEntity.getUrl(),
                requestEntity.getMethod(),
                request -> request.getHeaders().putAll(requestEntity.getHeaders()),
                extractor
        );

        byte[] body = response == null ? null : response.body();
        if (body == null || body.length == 0) {
            return null;
        }

        Charset charset = detectResponseCharset(response.headers(), body);
        return new String(body, charset);
    }

    private byte[] readAllBytes(java.io.InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
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
        return isBlank(clientId) || isBlank(clientSecret) || isBlank(newsUrl);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<String> buildCandidateQueries(String query) {
        List<String> candidates = new ArrayList<>();
        candidates.add(query);

        String normalizedQuery = normalize(query);
        if (normalizedQuery.contains(normalize("딥페이크")) || normalizedQuery.contains("deepfake")) {
            for (String keyword : DEEPFAKE_KEYWORDS) {
                if (!candidates.contains(keyword)) {
                    candidates.add(keyword);
                }
            }
        }

        return candidates;
    }

    private List<NaverNewsResponseDTO.Item> requestNews(String query, int start, String sort) {
        URI uri = UriComponentsBuilder.fromHttpUrl(newsUrl)
                .queryParam("query", query)
                .queryParam("display", NEWS_DISPLAY_COUNT)
                .queryParam("start", start)
                .queryParam("sort", normalizeSort(sort))
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.info("Requesting news API: uri={}, query={}", uri, query);

        ResponseEntity<NaverNewsResponseDTO> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                requestEntity,
                NaverNewsResponseDTO.class
        );

        log.info("News API response status={} for query={}", response.getStatusCode(), query);

        if (response.getBody() == null || response.getBody().getItems() == null) {
            return List.of();
        }

        return response.getBody().getItems();
    }

    private boolean isDeepfakeRelated(String title, String description, String query) {
        String normalizedText = normalize(title) + " " + normalize(description);
        String normalizedQuery = normalize(query);

        if (!normalizedQuery.isBlank() && normalizedText.contains(normalizedQuery)) {
            return true;
        }

        for (String keyword : DEEPFAKE_KEYWORDS) {
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
        return "title:" + normalize(doc.getTitle());
    }

    private String normalizeQuery(String query) {
        return isBlank(query) ? "딥페이크" : query.trim();
    }

    private String normalizeSort(String sort) {
        return SORT_DATE.equalsIgnoreCase(sort) ? SORT_DATE : SORT_SIM;
    }

    private String buildCacheKey(String query, String sort) {
        return query + "::" + normalizeSort(sort);
    }

    private String extractOriginalQuery(String cacheKey) {
        if (isBlank(cacheKey)) {
            return normalizeQuery(null);
        }

        int separatorIndex = cacheKey.lastIndexOf("::");
        if (separatorIndex < 0) {
            return cacheKey;
        }

        return cacheKey.substring(0, separatorIndex);
    }

    private List<NewsCacheDocument> getMongoCache(String cacheKey) {
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

        if (!result.isEmpty()) {
            try {
                newsCacheRepository.saveAll(result);
            } catch (RuntimeException e) {
                log.warn("Failed to save Mongo news cache for query={}. Returning uncached result.", query, e);
            }
        }

        log.info("Saved {} news items for query={} (filtered out {})", result.size(), query, filteredOutCount);
        if (result.isEmpty() && !filteredOutTitles.isEmpty()) {
            log.info("Sample filtered titles for query={}: {}", query, filteredOutTitles);
        }

        return result;
    }

    private List<NewsCacheDocument> deduplicateArticles(List<NewsCacheDocument> articles) {
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

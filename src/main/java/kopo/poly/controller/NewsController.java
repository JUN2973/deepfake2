package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import kopo.poly.document.NewsCacheDocument;
import kopo.poly.service.impl.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 딥페이크 관련 뉴스 목록, 새로고침, 상세 화면을 처리하는 컨트롤러다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/news")
public class NewsController {

    private static final String DEFAULT_QUERY = "딥페이크";
    private static final String CATEGORY_ALL = "전체";
    private static final int PAGE_SIZE = 12;

    private final NewsService newsService;

    @GetMapping
    public String newsPage(
            HttpServletRequest request,
            ModelMap model
    ) {
        // 프로젝트 요청 방식에 맞춰 쿼리스트링 값을 직접 읽는다.
        String query = request.getParameter("query");
        String category = request.getParameter("category");
        String sort = request.getParameter("sort");
        int page = parseInt(request.getParameter("page"), 1);
        String effectiveQuery = (query == null || query.isBlank()) ? DEFAULT_QUERY : query.trim();
        String effectiveCategory = normalizeCategory(category);
        String effectiveSort = normalizeSort(sort);

        List<NewsCacheDocument> deduplicatedNews = newsService.getNews(effectiveQuery, effectiveSort).stream()
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

        // JSP로 넘기기 전에 필터링, 정렬, 페이징을 적용한다.
        List<NewsCacheDocument> newsList = deduplicatedNews.stream()
                .sorted(buildSortComparator(effectiveSort))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) newsList.size() / PAGE_SIZE));
        int currentPage = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * PAGE_SIZE, newsList.size());
        int toIndex = Math.min(fromIndex + PAGE_SIZE, newsList.size());
        List<NewsCacheDocument> pagedNewsList = newsList.subList(fromIndex, toIndex);

        model.addAttribute("newsList", pagedNewsList);
        model.addAttribute("query", effectiveQuery);
        model.addAttribute("selectedCategory", effectiveCategory);
        model.addAttribute("selectedSort", effectiveSort);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", newsList.size());

        return "news";
    }

    @GetMapping("/refresh")
    public String refreshNews(
            HttpServletRequest request
    ) {
        String query = request.getParameter("query");
        String category = request.getParameter("category");
        String sort = request.getParameter("sort");
        String effectiveQuery = (query == null || query.isBlank()) ? DEFAULT_QUERY : query.trim();
        String effectiveCategory = normalizeCategory(category);
        String effectiveSort = normalizeSort(sort);

        newsService.refreshNews(effectiveQuery, effectiveSort);

        String redirectUrl = "redirect:/news?query=" + UriUtils.encode(effectiveQuery, StandardCharsets.UTF_8);
        if (!CATEGORY_ALL.equals(effectiveCategory)) {
            redirectUrl += "&category=" + UriUtils.encode(effectiveCategory, StandardCharsets.UTF_8);
        }
        redirectUrl += "&sort=" + UriUtils.encode(effectiveSort, StandardCharsets.UTF_8);

        return redirectUrl;
    }

    @GetMapping("/{id}")
    public String newsDetailPage(
            @PathVariable("id") String id,
            HttpServletRequest request,
            ModelMap model
    ) {
        NewsCacheDocument news = newsService.getNewsDetail(id);
        if (news == null) {
            news = NewsCacheDocument.builder()
                    .id(id)
                    .title(request.getParameter("title"))
                    .description(request.getParameter("description"))
                    .pubDate(request.getParameter("pubDate"))
                    .originallink(request.getParameter("originallink"))
                    .link(request.getParameter("link"))
                    .build();
        }
        news = newsService.enrichNewsDetail(news);
        model.addAttribute("news", news);
        return "news-detail";
    }

    private String normalizeCategory(String category) {
        return (category == null || category.isBlank()) ? CATEGORY_ALL : category.trim();
    }

    private String normalizeSort(String sort) {
        return NewsService.SORT_DATE.equalsIgnoreCase(sort) ? NewsService.SORT_DATE : NewsService.SORT_SIM;
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Comparator<NewsCacheDocument> buildSortComparator(String sort) {
        // 날짜순은 기사 발행일을 우선 사용하고, 발행일 파싱 실패 시 캐시 생성일로 보정한다.
        if (NewsService.SORT_DATE.equals(sort)) {
            return Comparator
                    .comparing(this::parsePubDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed()
                    .thenComparing(NewsCacheDocument::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(NewsCacheDocument::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private ZonedDateTime parsePubDate(NewsCacheDocument news) {
        if (news == null || news.getPubDate() == null || news.getPubDate().isBlank()) {
            return fallbackDate(news);
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
                return fallbackDate(news);
            }
        }
    }

    private ZonedDateTime fallbackDate(NewsCacheDocument news) {
        LocalDateTime createdAt = news == null ? null : news.getCreatedAt();
        return createdAt == null ? null : createdAt.atZone(java.time.ZoneId.systemDefault());
    }

    private boolean matchesCategory(NewsCacheDocument news, String selectedCategory) {
        if (CATEGORY_ALL.equals(selectedCategory)) {
            return true;
        }
        return selectedCategory.equals(detectCategory(news));
    }

    private String buildDisplayDedupKey(NewsCacheDocument news) {
        // 언론사별로 제목 앞머리만 조금 다른 중복 기사를 화면에서 하나로 묶기 위한 키다.
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
        // 중복 기사 중에서는 최신 기사와 내용이 더 풍부한 기사를 우선 노출한다.
        ZonedDateTime firstDate = parsePubDate(first);
        ZonedDateTime secondDate = parsePubDate(second);

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
        // 별도 카테고리 API가 없으므로 제목/설명 키워드로 화면용 카테고리를 추정한다.
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
}

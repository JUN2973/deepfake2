package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(뉴스): 카테고리별 뉴스 조회, 검색, 상세 보기 화면을 담당한다.
 */
import jakarta.servlet.http.HttpServletRequest;
import kopo.poly.dto.NewsPageDTO;
import kopo.poly.dto.NewsViewDTO;
import kopo.poly.service.impl.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * 딥페이크 관련 뉴스 목록, 새로고침, 상세 화면을 처리하는 컨트롤러다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/news")
public class NewsController {

    private static final String DEFAULT_QUERY = "딥페이크";
    private static final String CATEGORY_ALL = "전체";
    private static final int PAGE_SIZE = 6;

    private final NewsService newsService;

    @GetMapping
    public String newsPage(
            HttpServletRequest request,
            ModelMap model
    ) {
        // 발표 포인트: 뉴스 목록 화면의 진입점이다.
        // 검색어, 카테고리, 정렬, 페이지 번호를 request parameter로 받아 화면에 필요한 목록만 만든다.
        // 프로젝트 요청 방식에 맞춰 쿼리스트링 값을 직접 읽는다.
        String query = request.getParameter("query");
        String category = request.getParameter("category");
        String sort = request.getParameter("sort");
        int page = parseInt(request.getParameter("page"), 1);
        NewsPageDTO newsPage = newsService.getNewsPage(query, category, sort, page, PAGE_SIZE);

        model.addAttribute("newsList", newsPage.getNewsList());
        model.addAttribute("query", newsPage.getQuery());
        model.addAttribute("selectedCategory", newsPage.getSelectedCategory());
        model.addAttribute("selectedSort", newsPage.getSelectedSort());
        model.addAttribute("currentPage", newsPage.getCurrentPage());
        model.addAttribute("totalPages", newsPage.getTotalPages());
        model.addAttribute("totalCount", newsPage.getTotalCount());

        return "news";
    }

    @GetMapping("/refresh")
    public String refreshNews(
            HttpServletRequest request
    ) {
        // 발표 포인트: 새로고침 URL은 기존 캐시를 비우고 빅카인즈 API에서 다시 가져오게 만든다.
        // 새로 가져온 뒤에는 사용자가 보고 있던 검색/정렬 조건을 유지한 채 목록으로 redirect한다.
        String query = request.getParameter("query");
        String category = request.getParameter("category");
        String sort = request.getParameter("sort");
        String effectiveQuery = (query == null || query.isBlank()) ? DEFAULT_QUERY : query.trim();
        String effectiveCategory = newsService.normalizeNewsCategory(category);
        String effectiveSort = newsService.normalizeNewsSort(sort);

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
        // 발표 포인트: 상세 화면은 먼저 캐시/MongoDB에서 기사 id를 찾는다.
        // 캐시가 비었거나 id 조회에 실패해도 목록에서 넘어온 query parameter로 최소 정보는 보여준다.
        NewsViewDTO newsView = newsService.getNewsDetailView(
                id,
                request.getParameter("title"),
                request.getParameter("description"),
                request.getParameter("pubDate"),
                request.getParameter("originallink"),
                request.getParameter("link")
        );
        model.addAttribute("news", newsView);
        return "news-detail";
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
}

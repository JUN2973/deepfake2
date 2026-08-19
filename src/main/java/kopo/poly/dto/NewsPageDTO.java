package kopo.poly.dto;

import kopo.poly.document.NewsCacheDocument;

import java.util.List;

/**
 * 뉴스 목록 화면에 필요한 필터링, 정렬, 페이징 결과를 담는다.
 */
public class NewsPageDTO {
    private final List<NewsCacheDocument> newsList;
    private final String query;
    private final String selectedCategory;
    private final String selectedSort;
    private final int currentPage;
    private final int totalPages;
    private final int totalCount;

    public NewsPageDTO(List<NewsCacheDocument> newsList,
                       String query,
                       String selectedCategory,
                       String selectedSort,
                       int currentPage,
                       int totalPages,
                       int totalCount) {
        this.newsList = newsList;
        this.query = query;
        this.selectedCategory = selectedCategory;
        this.selectedSort = selectedSort;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalCount = totalCount;
    }

    public List<NewsCacheDocument> getNewsList() {
        return newsList;
    }

    public String getQuery() {
        return query;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public String getSelectedSort() {
        return selectedSort;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getTotalCount() {
        return totalCount;
    }
}

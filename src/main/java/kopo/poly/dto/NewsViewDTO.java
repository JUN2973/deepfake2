package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: API 연동 설계/구현(뉴스): 네이버 뉴스 API 응답과 화면 출력 데이터를 전달한다.
 */
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NewsViewDTO {

    private String id;
    private String displayTitle;
    private String displaySummary;
    private String displayProvider;
    private String displayDate;
    private String originalUrl;
    private String displayCategory;
    private List<String> displayTags;
}

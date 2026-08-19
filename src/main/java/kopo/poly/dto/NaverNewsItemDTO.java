package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: API 연동 설계/구현(뉴스): 네이버 뉴스 API 응답과 화면 출력 데이터를 전달한다.
 */
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NaverNewsItemDTO {
    private String title;
    private String originallink;
    private String link;
    private String description;
    private String pubDate;
}

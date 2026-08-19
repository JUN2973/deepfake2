package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: API 연동 설계/구현(뉴스): 네이버 뉴스 API 응답과 화면 출력 데이터를 전달한다.
 */
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NaverNewsResponseDTO {
    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<NaverNewsItemDTO> items;
}

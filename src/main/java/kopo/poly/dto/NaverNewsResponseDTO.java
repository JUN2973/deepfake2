package kopo.poly.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
@Getter
@Setter
public class NaverNewsResponseDTO {

    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        private String title;
        private String originallink;
        private String link;
        private String description;
        private String pubDate;
    }
}
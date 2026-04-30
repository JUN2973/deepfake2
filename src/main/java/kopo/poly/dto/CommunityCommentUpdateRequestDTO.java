package kopo.poly.dto;

/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class CommunityCommentUpdateRequestDTO {
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: 논리 ERD/구현(커뮤니티): 커뮤니티 요청/응답 데이터 전달 구조를 정의한다.
 */
/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class CommunityCommentUpdateRequestDTO {
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

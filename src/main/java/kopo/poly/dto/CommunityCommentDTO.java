package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: 논리 ERD/구현(커뮤니티): 커뮤니티 요청/응답 데이터 전달 구조를 정의한다.
 */
import java.util.ArrayList;
import java.util.List;

/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class CommunityCommentDTO {
    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private String postTitle;
    private String author;
    private String content;
    private Integer likeCount;
    private Boolean likedByCurrentUser;
    private String createdAt;
    private String updatedAt;
    private List<CommunityCommentDTO> replies = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Boolean getLikedByCurrentUser() { return likedByCurrentUser; }
    public void setLikedByCurrentUser(Boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public List<CommunityCommentDTO> getReplies() { return replies; }
    public void setReplies(List<CommunityCommentDTO> replies) { this.replies = replies; }
}

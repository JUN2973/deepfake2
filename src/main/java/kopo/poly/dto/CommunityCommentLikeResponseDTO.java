package kopo.poly.dto;

/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class CommunityCommentLikeResponseDTO {
    private boolean liked;
    private int likeCount;

    public CommunityCommentLikeResponseDTO() {
    }

    public CommunityCommentLikeResponseDTO(boolean liked, int likeCount) {
        this.liked = liked;
        this.likeCount = likeCount;
    }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
}

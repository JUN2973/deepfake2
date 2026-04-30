package kopo.poly.mapper;

import kopo.poly.dto.CommunityPostDTO;
import kopo.poly.dto.CommunityCommentDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 커뮤니티 게시글, 댓글, 좋아요 SQL을 호출하는 MyBatis 매퍼 인터페이스다.
 */
public interface ICommunityMapper {
    List<CommunityPostDTO> selectPosts(@Param("keyword") String keyword,
                                       @Param("currentUserId") Long currentUserId);
    CommunityPostDTO selectPostById(@Param("id") Long id,
                                    @Param("currentUserId") Long currentUserId);
    int insertPost(@Param("userId") Long userId,
                   @Param("author") String author,
                   @Param("topic") String topic,
                   @Param("title") String title,
                   @Param("content") String content);
    int updatePost(@Param("id") Long id,
                   @Param("topic") String topic,
                   @Param("title") String title,
                   @Param("content") String content);
    int deletePost(@Param("id") Long id);
    List<CommunityCommentDTO> selectCommentsByPostId(@Param("postId") Long postId,
                                                     @Param("currentUserId") Long currentUserId);
    CommunityCommentDTO selectCommentById(@Param("id") Long id,
                                          @Param("currentUserId") Long currentUserId);
    int insertComment(@Param("postId") Long postId,
                      @Param("parentId") Long parentId,
                      @Param("userId") Long userId,
                      @Param("author") String author,
                      @Param("content") String content);
    int updateComment(@Param("id") Long id,
                      @Param("content") String content);
    int deleteComment(@Param("id") Long id);
    int countPostLike(@Param("postId") Long postId);
    int existsPostLike(@Param("postId") Long postId,
                       @Param("userId") Long userId);
    int insertPostLike(@Param("postId") Long postId,
                       @Param("userId") Long userId);
    int deletePostLike(@Param("postId") Long postId,
                       @Param("userId") Long userId);
    int countCommentLike(@Param("commentId") Long commentId);
    int existsCommentLike(@Param("commentId") Long commentId,
                          @Param("userId") Long userId);
    int insertCommentLike(@Param("commentId") Long commentId,
                          @Param("userId") Long userId);
    int deleteCommentLike(@Param("commentId") Long commentId,
                          @Param("userId") Long userId);
    Long selectLastInsertId();
    int increaseViews(@Param("id") Long id);
}

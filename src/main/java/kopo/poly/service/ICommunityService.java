package kopo.poly.service;


/**
 * 체크리스트 기준 주석: 설계/API 연동 설계: 컨트롤러와 구현체 사이의 서비스 계약을 정의한다.
 */
import kopo.poly.dto.CommunityPostDTO;
import kopo.poly.dto.CommunityCommentDTO;
import kopo.poly.dto.CommunityCommentLikeResponseDTO;
import kopo.poly.dto.CommunityPostLikeResponseDTO;

import java.util.List;

/**
 * 커뮤니티 기능이 제공해야 하는 서비스 계약을 정의한다.
 */
public interface ICommunityService {
    List<CommunityPostDTO> getPosts(String keyword);
    List<CommunityPostDTO> getPosts(String keyword, Long currentUserId);
    CommunityPostDTO getPost(Long id);
    CommunityPostDTO getPost(Long id, Long currentUserId);
    CommunityPostDTO findPost(Long id);
    CommunityPostDTO findPost(Long id, Long currentUserId);
    Long createPost(Long userId, String author, String topic, String title, String content);
    boolean updatePost(Long postId, Long userId, String topic, String title, String content);
    boolean deletePost(Long postId, Long userId);
    CommunityPostLikeResponseDTO togglePostLike(Long postId, Long userId);
    List<CommunityCommentDTO> getComments(Long postId, Long currentUserId);
    List<CommunityCommentDTO> getCommentsByUser(Long userId);
    CommunityCommentDTO getComment(Long commentId, Long currentUserId);
    Long createComment(Long postId, Long parentId, Long userId, String author, String content);
    boolean updateComment(Long commentId, Long userId, String content);
    boolean deleteComment(Long commentId, Long userId);
    CommunityCommentLikeResponseDTO toggleCommentLike(Long commentId, Long userId);
}

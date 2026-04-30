package kopo.poly.service.impl;

import kopo.poly.dto.CommunityCommentDTO;
import kopo.poly.dto.CommunityCommentLikeResponseDTO;
import kopo.poly.dto.CommunityPostDTO;
import kopo.poly.dto.CommunityPostLikeResponseDTO;
import kopo.poly.mapper.ICommunityMapper;
import kopo.poly.service.ICommunityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 커뮤니티 게시글, 댓글, 좋아요의 핵심 비즈니스 로직을 처리한다.
 */
@Service
public class CommunityService implements ICommunityService {
    // topic이 비어 있거나 허용 목록 밖이면 자유 토론 기본값으로 저장한다.
    private static final String DEFAULT_TOPIC = "discussion";

    private final ICommunityMapper communityMapper;

    public CommunityService(ICommunityMapper communityMapper) {
        this.communityMapper = communityMapper;
    }

    @Override
    public List<CommunityPostDTO> getPosts(String keyword) {
        return getPosts(keyword, null);
    }

    @Override
    public List<CommunityPostDTO> getPosts(String keyword, Long currentUserId) {
        return communityMapper.selectPosts(keyword, currentUserId);
    }

    @Override
    @Transactional
    public CommunityPostDTO getPost(Long id) {
        return getPost(id, null);
    }

    @Override
    @Transactional
    public CommunityPostDTO getPost(Long id, Long currentUserId) {
        // 상세 조회는 조회수를 증가시키고, findPost()는 부수 효과 없이 조회만 한다.
        communityMapper.increaseViews(id);
        return communityMapper.selectPostById(id, currentUserId);
    }

    @Override
    public CommunityPostDTO findPost(Long id) {
        return findPost(id, null);
    }

    @Override
    public CommunityPostDTO findPost(Long id, Long currentUserId) {
        return communityMapper.selectPostById(id, currentUserId);
    }

    @Override
    @Transactional
    public Long createPost(Long userId, String author, String topic, String title, String content) {
        // insert 후 같은 커넥션에서 마지막 생성 id를 조회해 화면 이동에 사용한다.
        communityMapper.insertPost(userId, author, normalizeTopic(topic), title, content);
        return communityMapper.selectLastInsertId();
    }

    @Override
    @Transactional
    public boolean updatePost(Long postId, Long userId, String topic, String title, String content) {
        // 작성자 본인만 수정할 수 있게 DB 조회 결과의 userId와 세션 userId를 비교한다.
        CommunityPostDTO post = communityMapper.selectPostById(postId, userId);
        if (post == null || post.getUserId() == null || !post.getUserId().equals(userId)) {
            return false;
        }
        return communityMapper.updatePost(postId, normalizeTopic(topic), title, content) > 0;
    }

    @Override
    @Transactional
    public boolean deletePost(Long postId, Long userId) {
        CommunityPostDTO post = communityMapper.selectPostById(postId, userId);
        if (post == null || post.getUserId() == null || !post.getUserId().equals(userId)) {
            return false;
        }
        return communityMapper.deletePost(postId) > 0;
    }

    @Override
    @Transactional
    public CommunityPostLikeResponseDTO togglePostLike(Long postId, Long userId) {
        CommunityPostDTO post = communityMapper.selectPostById(postId, userId);
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }

        boolean exists = communityMapper.existsPostLike(postId, userId) > 0;
        // 이미 좋아요가 있으면 삭제, 없으면 추가해서 프론트에서는 한 버튼으로 처리한다.
        if (exists) {
            communityMapper.deletePostLike(postId, userId);
        } else {
            communityMapper.insertPostLike(postId, userId);
        }

        return new CommunityPostLikeResponseDTO(!exists, communityMapper.countPostLike(postId));
    }

    @Override
    public List<CommunityCommentDTO> getComments(Long postId, Long currentUserId) {
        // Mapper는 평면 목록을 반환하므로 서비스에서 부모 댓글/답글 구조로 변환한다.
        return buildCommentTree(communityMapper.selectCommentsByPostId(postId, currentUserId));
    }

    @Override
    public CommunityCommentDTO getComment(Long commentId, Long currentUserId) {
        return communityMapper.selectCommentById(commentId, currentUserId);
    }

    @Override
    @Transactional
    public Long createComment(Long postId, Long parentId, Long userId, String author, String content) {
        if (parentId != null) {
            // 답글은 같은 게시글의 부모 댓글에만 달 수 있고, 대댓글의 대댓글은 막는다.
            CommunityCommentDTO parent = communityMapper.selectCommentById(parentId, userId);
            if (parent == null || !postId.equals(parent.getPostId())) {
                throw new IllegalArgumentException("부모 댓글을 찾을 수 없습니다.");
            }
            if (parent.getParentId() != null) {
                throw new IllegalArgumentException("답글은 한 단계까지만 작성할 수 있습니다.");
            }
        }

        communityMapper.insertComment(postId, parentId, userId, author, content);
        return communityMapper.selectLastInsertId();
    }

    @Override
    @Transactional
    public boolean updateComment(Long commentId, Long userId, String content) {
        CommunityCommentDTO comment = communityMapper.selectCommentById(commentId, userId);
        if (comment == null || comment.getUserId() == null || !comment.getUserId().equals(userId)) {
            return false;
        }
        return communityMapper.updateComment(commentId, content) > 0;
    }

    @Override
    @Transactional
    public boolean deleteComment(Long commentId, Long userId) {
        CommunityCommentDTO comment = communityMapper.selectCommentById(commentId, userId);
        if (comment == null || comment.getUserId() == null || !comment.getUserId().equals(userId)) {
            return false;
        }
        return communityMapper.deleteComment(commentId) > 0;
    }

    @Override
    @Transactional
    public CommunityCommentLikeResponseDTO toggleCommentLike(Long commentId, Long userId) {
        CommunityCommentDTO comment = communityMapper.selectCommentById(commentId, userId);
        if (comment == null) {
            throw new IllegalArgumentException("댓글을 찾을 수 없습니다.");
        }

        boolean exists = communityMapper.existsCommentLike(commentId, userId) > 0;
        if (exists) {
            communityMapper.deleteCommentLike(commentId, userId);
        } else {
            communityMapper.insertCommentLike(commentId, userId);
        }

        return new CommunityCommentLikeResponseDTO(!exists, communityMapper.countCommentLike(commentId));
    }

    private List<CommunityCommentDTO> buildCommentTree(List<CommunityCommentDTO> comments) {
        // DB에서 받은 평면 댓글 목록을 JSP/JS가 바로 렌더링하기 쉬운 트리 구조로 바꾼다.
        Map<Long, CommunityCommentDTO> indexed = new LinkedHashMap<>();
        List<CommunityCommentDTO> roots = new ArrayList<>();

        for (CommunityCommentDTO comment : comments) {
            comment.setReplies(new ArrayList<>());
            indexed.put(comment.getId(), comment);
        }

        for (CommunityCommentDTO comment : comments) {
            if (comment.getParentId() == null) {
                roots.add(comment);
                continue;
            }

            CommunityCommentDTO parent = indexed.get(comment.getParentId());
            if (parent == null) {
                roots.add(comment);
                continue;
            }

            parent.getReplies().add(comment);
        }

        return roots;
    }

    private String normalizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return DEFAULT_TOPIC;
        }

        String normalized = topic.trim();
        return switch (normalized) {
            case "deepfake", "ai-tech", "news", "discussion" -> normalized;
            default -> DEFAULT_TOPIC;
        };
    }
}

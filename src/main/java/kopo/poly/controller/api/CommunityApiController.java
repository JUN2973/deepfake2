package kopo.poly.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.CommunityCommentCreateRequestDTO;
import kopo.poly.dto.CommunityCommentDTO;
import kopo.poly.dto.CommunityCommentLikeResponseDTO;
import kopo.poly.dto.CommunityCommentUpdateRequestDTO;
import kopo.poly.dto.CommunityCreateRequestDTO;
import kopo.poly.dto.CommunityPostDTO;
import kopo.poly.dto.CommunityPostLikeResponseDTO;
import kopo.poly.service.ICommunityService;
import kopo.poly.util.SessionUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/community/posts")
public class CommunityApiController {

    private static final List<String> ALLOWED_TOPICS = List.of("deepfake", "ai-tech", "news", "discussion");

    private final ICommunityService communityService;

    public CommunityApiController(ICommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public ApiResponse<List<CommunityPostDTO>> list(HttpServletRequest request, HttpSession session) {
        String keyword = request.getParameter("keyword");
        return ApiResponse.ok(communityService.getPosts(keyword, SessionUtil.getUserId(session)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CommunityPostDTO> detail(@PathVariable Long id, HttpSession session) {
        CommunityPostDTO post = communityService.getPost(id, SessionUtil.getUserId(session));
        if (post == null) {
            return ApiResponse.fail("CM-4040", "게시글을 찾을 수 없습니다.");
        }
        return ApiResponse.ok(post);
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody CommunityCreateRequestDTO req, HttpSession session) {
        SessionUser user = getSessionUser(session);
        if (user == null) {
            return ApiResponse.fail("CM-4010", "로그인 후 글을 작성할 수 있습니다.");
        }

        ApiResponse<Long> validationError = validatePostRequest(req);
        if (validationError != null) {
            return validationError;
        }

        Long postId = communityService.createPost(
                user.id(),
                user.name(),
                req.getTopic().trim(),
                req.getTitle().trim(),
                req.getContent().trim()
        );

        return ApiResponse.ok(postId);
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable Long id,
                                       @RequestBody CommunityCreateRequestDTO req,
                                       HttpSession session) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null) {
            return ApiResponse.fail("CM-4010", "로그인 후 글을 수정할 수 있습니다.");
        }

        ApiResponse<Boolean> validationError = validatePostRequest(req);
        if (validationError != null) {
            return validationError;
        }

        boolean updated = communityService.updatePost(
                id,
                currentUserId,
                req.getTopic().trim(),
                req.getTitle().trim(),
                req.getContent().trim()
        );
        if (!updated) {
            return ApiResponse.fail("CM-4030", "게시글 수정 권한이 없습니다.");
        }
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id, HttpSession session) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null) {
            return ApiResponse.fail("CM-4010", "로그인 후 글을 삭제할 수 있습니다.");
        }
        if (!communityService.deletePost(id, currentUserId)) {
            return ApiResponse.fail("CM-4033", "게시글 삭제 권한이 없습니다.");
        }
        return ApiResponse.ok(true);
    }

    @PostMapping("/{id}/like")
    public ApiResponse<CommunityPostLikeResponseDTO> togglePostLike(@PathVariable Long id, HttpSession session) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null) {
            return ApiResponse.fail("CM-4010", "로그인 후 좋아요를 사용할 수 있습니다.");
        }
        try {
            return ApiResponse.ok(communityService.togglePostLike(id, currentUserId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("CM-4040", e.getMessage());
        }
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<List<CommunityCommentDTO>> comments(@PathVariable Long id, HttpSession session) {
        CommunityPostDTO post = communityService.findPost(id);
        if (post == null) {
            return ApiResponse.fail("CM-4040", "게시글을 찾을 수 없습니다.");
        }

        Long currentUserId = SessionUtil.getUserId(session);
        return ApiResponse.ok(communityService.getComments(id, currentUserId));
    }

    @GetMapping("/my-comments")
    public ApiResponse<List<CommunityCommentDTO>> myComments(HttpSession session) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null) {
            return ApiResponse.fail("CM-4011", "로그인이 필요합니다.");
        }
        return ApiResponse.ok(communityService.getCommentsByUser(currentUserId));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<Long> createComment(@PathVariable Long id,
                                           @RequestBody CommunityCommentCreateRequestDTO req,
                                           HttpSession session) {
        SessionUser user = getSessionUser(session);
        if (user == null) {
            return ApiResponse.fail("CM-4011", "로그인 후 댓글을 작성할 수 있습니다.");
        }

        ApiResponse<Long> validationError = validateCommentContent(req == null ? null : req.getContent());
        if (validationError != null) {
            return validationError;
        }

        CommunityPostDTO post = communityService.findPost(id);
        if (post == null) {
            return ApiResponse.fail("CM-4040", "게시글을 찾을 수 없습니다.");
        }

        try {
            Long commentId = communityService.createComment(
                    id,
                    req.getParentId(),
                    user.id(),
                    user.name(),
                    req.getContent().trim()
            );
            return ApiResponse.ok(commentId);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("CM-4004", e.getMessage());
        }
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Boolean> updateComment(@PathVariable Long postId,
                                              @PathVariable Long commentId,
                                              @RequestBody CommunityCommentUpdateRequestDTO req,
                                              HttpSession session) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null) {
            return ApiResponse.fail("CM-4011", "로그인 후 댓글을 수정할 수 있습니다.");
        }

        ApiResponse<Boolean> validationError = validateCommentContent(req == null ? null : req.getContent());
        if (validationError != null) {
            return validationError;
        }

        CommunityCommentDTO comment = communityService.getComment(commentId, currentUserId);
        if (comment == null || !postId.equals(comment.getPostId())) {
            return ApiResponse.fail("CM-4041", "댓글을 찾을 수 없습니다.");
        }
        if (!communityService.updateComment(commentId, currentUserId, req.getContent().trim())) {
            return ApiResponse.fail("CM-4031", "댓글 수정 권한이 없습니다.");
        }
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Boolean> deleteComment(@PathVariable Long postId,
                                              @PathVariable Long commentId,
                                              HttpSession session) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null) {
            return ApiResponse.fail("CM-4011", "로그인 후 댓글을 삭제할 수 있습니다.");
        }

        CommunityCommentDTO comment = communityService.getComment(commentId, currentUserId);
        if (comment == null || !postId.equals(comment.getPostId())) {
            return ApiResponse.fail("CM-4041", "댓글을 찾을 수 없습니다.");
        }
        if (!communityService.deleteComment(commentId, currentUserId)) {
            return ApiResponse.fail("CM-4032", "댓글 삭제 권한이 없습니다.");
        }
        return ApiResponse.ok(true);
    }

    @PostMapping("/{postId}/comments/{commentId}/like")
    public ApiResponse<CommunityCommentLikeResponseDTO> toggleCommentLike(@PathVariable Long postId,
                                                                          @PathVariable Long commentId,
                                                                          HttpSession session) {
        Long currentUserId = SessionUtil.getUserId(session);
        if (currentUserId == null) {
            return ApiResponse.fail("CM-4011", "로그인 후 댓글 좋아요를 사용할 수 있습니다.");
        }

        CommunityCommentDTO comment = communityService.getComment(commentId, currentUserId);
        if (comment == null || !postId.equals(comment.getPostId())) {
            return ApiResponse.fail("CM-4041", "댓글을 찾을 수 없습니다.");
        }

        try {
            return ApiResponse.ok(communityService.toggleCommentLike(commentId, currentUserId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("CM-4005", e.getMessage());
        }
    }

    private <T> ApiResponse<T> validatePostRequest(CommunityCreateRequestDTO req) {
        if (req == null || req.getTitle() == null || req.getTitle().isBlank()) {
            return ApiResponse.fail("CM-4001", "제목을 입력해 주세요.");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ApiResponse.fail("CM-4002", "내용을 입력해 주세요.");
        }
        if (req.getTopic() == null || req.getTopic().isBlank() || !ALLOWED_TOPICS.contains(req.getTopic().trim())) {
            return ApiResponse.fail("CM-4003", "주제를 선택해 주세요.");
        }
        return null;
    }

    private <T> ApiResponse<T> validateCommentContent(String content) {
        if (content == null || content.isBlank()) {
            return ApiResponse.fail("CM-4003", "댓글 내용을 입력해 주세요.");
        }
        return null;
    }

    private SessionUser getSessionUser(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        Object userNameObj = session.getAttribute("USER_NAME");
        if (userId == null || userNameObj == null) {
            return null;
        }
        return new SessionUser(userId, String.valueOf(userNameObj));
    }

    private record SessionUser(Long id, String name) {
    }
}

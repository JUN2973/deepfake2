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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 커뮤니티 게시글, 댓글, 좋아요 API를 제공하는 REST 컨트롤러다.
 */
@RestController
@RequestMapping("/api/v1/community/posts")
public class CommunityApiController {
    // 화면의 탭 값과 DB 저장 값을 맞추기 위해 허용된 주제만 받는다.
    private static final List<String> ALLOWED_TOPICS = List.of("deepfake", "ai-tech", "news", "discussion");

    private final ICommunityService communityService;

    public CommunityApiController(ICommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public ApiResponse<List<CommunityPostDTO>> list(HttpServletRequest request, HttpSession session) {
        // 로그인 사용자가 있으면 좋아요 여부까지 함께 내려주고, 비로그인은 목록만 조회한다.
        String keyword = request.getParameter("keyword");
        return ApiResponse.ok(communityService.getPosts(keyword, getSessionUserId(session)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CommunityPostDTO> detail(@PathVariable Long id, HttpSession session) {
        CommunityPostDTO post = communityService.getPost(id, getSessionUserId(session));
        if (post == null) {
            return ApiResponse.fail("CM-4040", "게시글을 찾을 수 없습니다.");
        }
        return ApiResponse.ok(post);
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody CommunityCreateRequestDTO req, HttpSession session) {
        // 게시글 작성자는 클라이언트 입력값이 아니라 로그인 세션에서만 가져온다.
        Object userIdObj = session.getAttribute("USER_ID");
        Object userNameObj = session.getAttribute("USER_NAME");

        if (userIdObj == null || userNameObj == null) {
            return ApiResponse.fail("CM-4010", "로그인 후 글을 작성할 수 있습니다.");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            return ApiResponse.fail("CM-4001", "제목을 입력해 주세요.");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ApiResponse.fail("CM-4002", "내용을 입력해 주세요.");
        }
        if (req.getTopic() == null || req.getTopic().isBlank() || !ALLOWED_TOPICS.contains(req.getTopic().trim())) {
            return ApiResponse.fail("CM-4003", "주제를 선택해 주세요.");
        }

        Long postId = communityService.createPost(
                Long.valueOf(String.valueOf(userIdObj)),
                String.valueOf(userNameObj),
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
        // 서비스 계층에서 작성자 본인인지 다시 확인하므로 여기서는 로그인/입력값 검증을 담당한다.
        Long currentUserId = getSessionUserId(session);
        if (currentUserId == null) {
            return ApiResponse.fail("CM-4010", "로그인 후 글을 수정할 수 있습니다.");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            return ApiResponse.fail("CM-4001", "제목을 입력해 주세요.");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ApiResponse.fail("CM-4002", "내용을 입력해 주세요.");
        }
        if (req.getTopic() == null || req.getTopic().isBlank() || !ALLOWED_TOPICS.contains(req.getTopic().trim())) {
            return ApiResponse.fail("CM-4003", "주제를 선택해 주세요.");
        }
        if (!communityService.updatePost(id, currentUserId, req.getTopic().trim(), req.getTitle().trim(), req.getContent().trim())) {
            return ApiResponse.fail("CM-4030", "게시글 수정 권한이 없습니다.");
        }
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id, HttpSession session) {
        Long currentUserId = getSessionUserId(session);
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
        // 좋아요는 같은 API를 다시 호출하면 취소되는 토글 방식이다.
        Long currentUserId = getSessionUserId(session);
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
        // 댓글 조회 전 게시글 존재 여부를 확인해 잘못된 postId 요청을 구분한다.
        CommunityPostDTO post = communityService.findPost(id);
        if (post == null) {
            return ApiResponse.fail("CM-4040", "게시글을 찾을 수 없습니다.");
        }

        Long currentUserId = getSessionUserId(session);
        return ApiResponse.ok(communityService.getComments(id, currentUserId));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<Long> createComment(@PathVariable Long id,
                                           @RequestBody CommunityCommentCreateRequestDTO req,
                                           HttpSession session) {
        // 댓글과 답글은 같은 엔드포인트를 쓰고, parentId가 있으면 답글로 처리한다.
        Object userIdObj = session.getAttribute("USER_ID");
        Object userNameObj = session.getAttribute("USER_NAME");

        if (userIdObj == null || userNameObj == null) {
            return ApiResponse.fail("CM-4011", "로그인 후 댓글을 작성할 수 있습니다.");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ApiResponse.fail("CM-4003", "댓글 내용을 입력해 주세요.");
        }

        CommunityPostDTO post = communityService.findPost(id);
        if (post == null) {
            return ApiResponse.fail("CM-4040", "게시글을 찾을 수 없습니다.");
        }

        try {
            Long commentId = communityService.createComment(
                    id,
                    req.getParentId(),
                    Long.valueOf(String.valueOf(userIdObj)),
                    String.valueOf(userNameObj),
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
        // URL의 postId와 실제 댓글의 postId가 일치해야 다른 글의 댓글을 수정하지 않는다.
        Long currentUserId = getSessionUserId(session);
        if (currentUserId == null) {
            return ApiResponse.fail("CM-4011", "로그인 후 댓글을 수정할 수 있습니다.");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            return ApiResponse.fail("CM-4003", "댓글 내용을 입력해 주세요.");
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
        Long currentUserId = getSessionUserId(session);
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
        Long currentUserId = getSessionUserId(session);
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

    private Long getSessionUserId(HttpSession session) {
        // 일반 로그인과 OAuth 로그인 모두 USER_ID 세션 값을 기준으로 현재 사용자를 판단한다.
        Object userIdObj = session.getAttribute("USER_ID");
        if (userIdObj == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(userIdObj));
    }
}

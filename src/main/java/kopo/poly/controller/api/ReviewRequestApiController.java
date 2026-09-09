package kopo.poly.controller.api;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.ReviewRequestCreateDTO;
import kopo.poly.dto.ReviewRequestResponseDTO;
import kopo.poly.service.IReviewRequestService;
import kopo.poly.service.ReviewRequestServiceException;
import kopo.poly.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 로그인 사용자의 오탐 신고 및 재검토 요청 등록·조회 API를 제공한다.
 */
@RestController
@RequestMapping("/api/v1")
public class ReviewRequestApiController {

    private static final Logger log = LoggerFactory.getLogger(ReviewRequestApiController.class);

    private final IReviewRequestService reviewRequestService;

    public ReviewRequestApiController(IReviewRequestService reviewRequestService) {
        this.reviewRequestService = reviewRequestService;
    }

    @PostMapping("/verifications/{verificationId}/review-requests")
    public ApiResponse<ReviewRequestResponseDTO> create(
            @PathVariable Long verificationId,
            @RequestBody(required = false) ReviewRequestCreateDTO request,
            HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("REVIEW-4010", "로그인 후 재검토를 요청할 수 있습니다.");
        }

        try {
            return ApiResponse.ok(reviewRequestService.createRequest(verificationId, userId, request));
        } catch (ReviewRequestServiceException e) {
            log.warn("Review request create failed. code={}, verificationId={}, userId={}",
                    e.getCode(), verificationId, userId);
            return ApiResponse.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected review request create error. verificationId={}, userId={}",
                    verificationId, userId, e);
            return ApiResponse.fail("REVIEW-5000", "재검토 요청을 등록하는 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/review-requests")
    public ApiResponse<List<ReviewRequestResponseDTO>> list(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("REVIEW-4010", "로그인 후 재검토 요청을 확인할 수 있습니다.");
        }

        try {
            return ApiResponse.ok(reviewRequestService.getRequests(userId));
        } catch (ReviewRequestServiceException e) {
            return ApiResponse.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected review request list error. userId={}", userId, e);
            return ApiResponse.fail("REVIEW-5001", "재검토 요청 목록을 불러오지 못했습니다.");
        }
    }

    @GetMapping("/review-requests/{id}")
    public ApiResponse<ReviewRequestResponseDTO> detail(
            @PathVariable Long id,
            HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("REVIEW-4010", "로그인 후 재검토 요청을 확인할 수 있습니다.");
        }

        try {
            return ApiResponse.ok(reviewRequestService.getRequest(id, userId));
        } catch (ReviewRequestServiceException e) {
            return ApiResponse.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected review request detail error. id={}, userId={}", id, userId, e);
            return ApiResponse.fail("REVIEW-5002", "재검토 요청을 불러오지 못했습니다.");
        }
    }
}

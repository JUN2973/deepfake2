package kopo.poly.controller.api;

import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.ReviewRequestCreateDTO;
import kopo.poly.dto.ReviewRequestResponseDTO;
import kopo.poly.service.IReviewRequestService;
import kopo.poly.service.ReviewRequestServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewRequestApiControllerTest {

    private IReviewRequestService reviewRequestService;
    private ReviewRequestApiController controller;

    @BeforeEach
    void setUp() {
        reviewRequestService = mock(IReviewRequestService.class);
        controller = new ReviewRequestApiController(reviewRequestService);
    }

    @Test
    void createUsesLoggedInUserId() {
        ReviewRequestCreateDTO request = new ReviewRequestCreateDTO();
        request.setRequestType(ReviewRequestCreateDTO.TYPE_RECHECK);
        request.setReason("다시 확인해 주세요.");
        ReviewRequestResponseDTO response = response(11L);
        when(reviewRequestService.createRequest(7L, 3L, request)).thenReturn(response);
        MockHttpSession session = loggedInSession(3L);

        ApiResponse<ReviewRequestResponseDTO> result = controller.create(7L, request, session);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(response);
        verify(reviewRequestService).createRequest(7L, 3L, request);
    }

    @Test
    void createRejectsAnonymousUserBeforeCallingService() {
        ReviewRequestCreateDTO request = new ReviewRequestCreateDTO();

        ApiResponse<ReviewRequestResponseDTO> result = controller.create(
                7L, request, new MockHttpSession()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("REVIEW-4010");
        verify(reviewRequestService, never()).createRequest(7L, null, request);
    }

    @Test
    void createMapsServiceValidationError() {
        ReviewRequestCreateDTO request = new ReviewRequestCreateDTO();
        when(reviewRequestService.createRequest(7L, 3L, request)).thenThrow(
                new ReviewRequestServiceException("REVIEW-REASON", "재검토 요청 사유를 입력해 주세요.")
        );

        ApiResponse<ReviewRequestResponseDTO> result = controller.create(
                7L, request, loggedInSession(3L)
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("REVIEW-REASON");
        assertThat(result.getError().getMessage()).contains("사유");
    }

    @Test
    void listReturnsCurrentUsersRequests() {
        List<ReviewRequestResponseDTO> responses = List.of(response(11L), response(12L));
        when(reviewRequestService.getRequests(3L)).thenReturn(responses);

        ApiResponse<List<ReviewRequestResponseDTO>> result = controller.list(loggedInSession(3L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
        verify(reviewRequestService).getRequests(3L);
    }

    @Test
    void detailReturnsOnlyCurrentUsersRequest() {
        ReviewRequestResponseDTO response = response(11L);
        when(reviewRequestService.getRequest(11L, 3L)).thenReturn(response);

        ApiResponse<ReviewRequestResponseDTO> result = controller.detail(11L, loggedInSession(3L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(response);
        verify(reviewRequestService).getRequest(11L, 3L);
    }

    @Test
    void detailMapsMissingRequestError() {
        when(reviewRequestService.getRequest(99L, 3L)).thenThrow(
                new ReviewRequestServiceException("REVIEW-4041", "재검토 요청을 찾을 수 없습니다.")
        );

        ApiResponse<ReviewRequestResponseDTO> result = controller.detail(99L, loggedInSession(3L));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("REVIEW-4041");
    }

    private MockHttpSession loggedInSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", userId);
        return session;
    }

    private ReviewRequestResponseDTO response(Long id) {
        ReviewRequestResponseDTO response = new ReviewRequestResponseDTO();
        response.setId(id);
        response.setVerificationId(7L);
        response.setUserId(3L);
        response.setRequestType(ReviewRequestCreateDTO.TYPE_RECHECK);
        response.setReason("다시 확인해 주세요.");
        response.setStatus(ReviewRequestResponseDTO.STATUS_PENDING);
        return response;
    }
}

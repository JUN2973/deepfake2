package kopo.poly.controller.api;

import kopo.poly.dto.AiImageVerificationResponseDTO;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.IAiImageVerificationService;
import kopo.poly.service.IVerifyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiImageVerificationApiControllerTest {

    private IVerifyService verifyService;
    private IAiImageVerificationService imageVerificationService;
    private AiImageVerificationApiController controller;

    @BeforeEach
    void setUp() {
        verifyService = mock(IVerifyService.class);
        imageVerificationService = mock(IAiImageVerificationService.class);
        controller = new AiImageVerificationApiController(verifyService, imageVerificationService);
    }

    @Test
    void reviewImageAllowsOwner() {
        VerifyDTO verification = verification(7L, 11L);
        AiImageVerificationResponseDTO response = new AiImageVerificationResponseDTO();
        response.setVerificationId(7L);
        when(verifyService.getOne(7L)).thenReturn(verification);
        when(imageVerificationService.verify(verification)).thenReturn(response);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 11L);

        ApiResponse<AiImageVerificationResponseDTO> result = controller.reviewImage(7L, session);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(response);
    }

    @Test
    void reviewImageRejectsAnotherUser() {
        when(verifyService.getOne(7L)).thenReturn(verification(7L, 11L));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 22L);

        ApiResponse<AiImageVerificationResponseDTO> result = controller.reviewImage(7L, session);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("AI-4030");
        verify(imageVerificationService, never()).verify(any());
    }

    @Test
    void getSavedReviewReturnsLatestResultToOwner() {
        VerifyDTO verification = verification(7L, 11L);
        AiImageVerificationResponseDTO saved = new AiImageVerificationResponseDTO();
        saved.setVerificationId(7L);
        when(verifyService.getOne(7L)).thenReturn(verification);
        when(imageVerificationService.getLatest(7L)).thenReturn(saved);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 11L);

        ApiResponse<AiImageVerificationResponseDTO> result = controller.getSavedReview(7L, session);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(saved);
    }

    @Test
    void reviewImageReturnsNotFoundWhenVerificationIsMissing() {
        when(verifyService.getOne(99L)).thenReturn(null);

        ApiResponse<AiImageVerificationResponseDTO> result = controller.reviewImage(
                99L,
                new MockHttpSession()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("AI-4040");
        verify(imageVerificationService, never()).verify(any());
    }

    private VerifyDTO verification(Long id, Long userId) {
        VerifyDTO verification = new VerifyDTO();
        verification.setId(id);
        verification.setUserId(userId);
        return verification;
    }
}

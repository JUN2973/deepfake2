package kopo.poly.controller.api;

import kopo.poly.dto.AiAnalysisRequestDTO;
import kopo.poly.dto.AiAnalysisResponseDTO;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.AiAnalysisServiceException;
import kopo.poly.service.IAiAnalysisService;
import kopo.poly.service.IVerifyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAnalysisApiControllerTest {

    private IVerifyService verifyService;
    private IAiAnalysisService aiAnalysisService;
    private AiAnalysisApiController controller;

    @BeforeEach
    void setUp() {
        verifyService = mock(IVerifyService.class);
        aiAnalysisService = mock(IAiAnalysisService.class);
        controller = new AiAnalysisApiController(verifyService, aiAnalysisService);
    }

    @Test
    void generateExplanationChecksOwnerAndCallsService() {
        VerifyDTO verification = verification(7L, 11L);
        AiAnalysisResponseDTO aiResponse = new AiAnalysisResponseDTO();
        aiResponse.setVerificationId(7L);
        aiResponse.setSummary("summary");
        when(verifyService.getOne(7L)).thenReturn(verification);
        when(aiAnalysisService.analyze(any(), any())).thenReturn(aiResponse);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 11L);

        ApiResponse<AiAnalysisResponseDTO> result = controller.generateExplanation(
                7L,
                null,
                session
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(aiResponse);

        ArgumentCaptor<AiAnalysisRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(AiAnalysisRequestDTO.class);
        verify(aiAnalysisService).analyze(requestCaptor.capture(), any(VerifyDTO.class));
        assertThat(requestCaptor.getValue().getVerificationId()).isEqualTo(7L);
    }

    @Test
    void getSavedExplanationReturnsLatestResultToOwner() {
        VerifyDTO verification = verification(7L, 11L);
        AiAnalysisResponseDTO saved = new AiAnalysisResponseDTO();
        saved.setVerificationId(7L);
        saved.setSummary("saved summary");
        when(verifyService.getOne(7L)).thenReturn(verification);
        when(aiAnalysisService.getLatest(7L)).thenReturn(saved);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 11L);

        ApiResponse<AiAnalysisResponseDTO> result = controller.getSavedExplanation(7L, session);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(saved);
    }

    @Test
    void generateExplanationReturnsNotFoundWhenVerificationIsMissing() {
        when(verifyService.getOne(99L)).thenReturn(null);

        ApiResponse<AiAnalysisResponseDTO> result = controller.generateExplanation(
                99L,
                new AiAnalysisRequestDTO(),
                new MockHttpSession()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("AI-4040");
        verify(aiAnalysisService, never()).analyze(any(), any());
    }

    @Test
    void generateExplanationRejectsAnotherUsersVerification() {
        when(verifyService.getOne(7L)).thenReturn(verification(7L, 11L));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 22L);

        ApiResponse<AiAnalysisResponseDTO> result = controller.generateExplanation(
                7L,
                new AiAnalysisRequestDTO(),
                session
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("AI-4030");
        verify(aiAnalysisService, never()).analyze(any(), any());
    }

    @Test
    void generateExplanationRejectsMismatchedBodyId() {
        when(verifyService.getOne(7L)).thenReturn(verification(7L, 11L));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 11L);
        AiAnalysisRequestDTO request = new AiAnalysisRequestDTO();
        request.setVerificationId(8L);

        ApiResponse<AiAnalysisResponseDTO> result = controller.generateExplanation(
                7L,
                request,
                session
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("AI-4000");
        verify(aiAnalysisService, never()).analyze(any(), any());
    }

    @Test
    void generateExplanationMapsProviderErrorCode() {
        VerifyDTO verification = verification(7L, 11L);
        when(verifyService.getOne(7L)).thenReturn(verification);
        when(aiAnalysisService.analyze(any(), any())).thenThrow(
                new AiAnalysisServiceException("AI-RATE-LIMIT", "quota exceeded")
        );
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", 11L);

        ApiResponse<AiAnalysisResponseDTO> result = controller.generateExplanation(
                7L,
                new AiAnalysisRequestDTO(),
                session
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("AI-RATE-LIMIT");
        assertThat(result.getError().getMessage()).contains("사용량 한도");
    }

    private VerifyDTO verification(Long id, Long userId) {
        VerifyDTO verification = new VerifyDTO();
        verification.setId(id);
        verification.setUserId(userId);
        verification.setOriginalName("sample.mp4");
        verification.setVerdict("SUSPICIOUS");
        verification.setScore(0.82d);
        return verification;
    }
}

package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.AiAnalysisResultDTO;
import kopo.poly.dto.AiImageVerificationResponseDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IAiAnalysisResultMapper;
import kopo.poly.service.AiAnalysisServiceException;
import kopo.poly.service.IObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiImageVerificationServiceTest {

    private ObjectMapper objectMapper;
    private MockRestServiceServer server;
    private IObjectStorageService objectStorageService;
    private IAiAnalysisResultMapper resultMapper;
    private GeminiImageVerificationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        objectStorageService = mock(IObjectStorageService.class);
        resultMapper = mock(IAiAnalysisResultMapper.class);
        service = new GeminiImageVerificationService(
                builder.build(), objectMapper, objectStorageService, resultMapper,
                "test-api-key", "gemini-2.5-flash", 600, 8 * 1024 * 1024
        );
    }

    @Test
    void verifySendsImageAndCrossChecksDetectorResult() throws Exception {
        byte[] imageBytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        when(objectStorageService.readObject("verification/7/sample.png")).thenReturn(imageBytes);
        server.expect(once(), requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    JsonNode json = objectMapper.readTree(body);
                    JsonNode parts = json.path("contents").path(0).path("parts");
                    assertThat(parts.path(0).path("inline_data").path("mime_type").asText())
                            .isEqualTo("image/png");
                    assertThat(parts.path(0).path("inline_data").path("data").asText())
                            .isNotBlank();
                    assertThat(parts.path(1).path("text").asText()).contains("HIGH_RISK");
                    assertThat(json.path("generationConfig").path("responseMimeType").asText())
                            .isEqualTo("application/json");
                })
                .andRespond(withSuccess(providerResponse(), MediaType.APPLICATION_JSON));

        AiImageVerificationResponseDTO result = service.verify(verification());

        assertThat(result.getVisualAssessment()).isEqualTo("GENERATED_LIKELY");
        assertThat(result.getCrossCheckStatus()).isEqualTo("AGREES");
        assertThat(result.getCombinedConclusion()).contains("모두 생성 또는 조작 가능성");
        assertThat(result.getTotalTokens()).isEqualTo(210);
        verify(resultMapper).upsert(any(AiAnalysisResultDTO.class));
        server.verify();
    }

    @Test
    void verifyReturnsSavedResultWithoutReadingImage() throws Exception {
        AiImageVerificationResponseDTO response = new AiImageVerificationResponseDTO();
        response.setVerificationId(7L);
        response.setVisualAssessment("UNCERTAIN");
        AiAnalysisResultDTO saved = new AiAnalysisResultDTO();
        saved.setResponseJson(objectMapper.writeValueAsString(response));
        when(resultMapper.selectByRequestHash(any(), any())).thenReturn(saved);

        AiImageVerificationResponseDTO result = service.verify(verification());

        assertThat(result.getCached()).isTrue();
        assertThat(result.getVisualAssessment()).isEqualTo("UNCERTAIN");
        verify(objectStorageService, never()).readObject(any());
        server.verify();
    }

    @Test
    void verifyRejectsOversizedImageBeforeApiCall() {
        VerifyDTO verification = verification();
        verification.setFileSize(9L * 1024 * 1024);

        assertThatThrownBy(() -> service.verify(verification))
                .isInstanceOf(AiAnalysisServiceException.class)
                .extracting(error -> ((AiAnalysisServiceException) error).getCode())
                .isEqualTo("AI-IMAGE-SIZE");
    }

    private VerifyDTO verification() {
        VerifyDTO verification = new VerifyDTO();
        verification.setId(7L);
        verification.setObjectKey("verification/7/sample.png");
        verification.setMimeType("image/png");
        verification.setFileSize(1024L);
        verification.setApiProvider("Reality Defender");
        verification.setVerdict("HIGH_RISK");
        verification.setScore(0.91);
        return verification;
    }

    private String providerResponse() throws Exception {
        String output = objectMapper.writeValueAsString(Map.of(
                "visualAssessment", "GENERATED_LIKELY",
                "confidenceLevel", "MEDIUM",
                "visualSummary", "일부 시각적 불일치가 관찰됩니다.",
                "visualIndicators", List.of("반사 방향이 일정하지 않습니다."),
                "limitations", "압축 흔적만으로 생성 여부를 확정할 수 없습니다."
        ));
        return objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "finishReason", "STOP",
                        "content", Map.of("parts", List.of(Map.of("text", output)))
                )),
                "usageMetadata", Map.of(
                        "promptTokenCount", 150,
                        "candidatesTokenCount", 60,
                        "totalTokenCount", 210
                )
        ));
    }
}

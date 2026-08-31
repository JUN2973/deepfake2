package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.AiAnalysisRequestDTO;
import kopo.poly.dto.AiAnalysisResponseDTO;
import kopo.poly.dto.AiAnalysisResultDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IAiAnalysisResultMapper;
import kopo.poly.service.AiAnalysisServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiAnalysisServiceTest {

    private ObjectMapper objectMapper;
    private MockRestServiceServer server;
    private IAiAnalysisResultMapper resultMapper;
    private GeminiAnalysisService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        resultMapper = mock(IAiAnalysisResultMapper.class);
        service = new GeminiAnalysisService(
                builder.build(),
                objectMapper,
                resultMapper,
                "test-api-key",
                "gemini-1.5-flash",
                450
        );
    }

    @Test
    void analyzeSendsStructuredRequestAndMapsResponse() throws Exception {
        String providerResponse = providerResponse();

        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    JsonNode requestJson = objectMapper.readTree(body);
                    String inputText = requestJson.path("contents").path(0).path("parts").path(0).path("text").asText();
                    JsonNode inputJson = objectMapper.readTree(inputText);

                    assertThat(requestJson.path("generationConfig").path("maxOutputTokens").asInt()).isEqualTo(450);
                    assertThat(requestJson.path("generationConfig").path("responseMimeType").asText())
                            .isEqualTo("application/json");
                    assertThat(requestJson.path("generationConfig").path("responseSchema").path("type").asText())
                            .isEqualTo("OBJECT");
                    assertThat(requestJson.path("systemInstruction").path("parts").path(0).path("text").asText())
                            .contains("Korean users");
                    assertThat(inputJson.path("detectionResult").path("confidencePercent").asInt())
                            .isEqualTo(82);
                    assertThat(inputJson.path("request").path("includeReportDraft").asBoolean())
                            .isTrue();
                })
                .andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

        AiAnalysisRequestDTO request = new AiAnalysisRequestDTO();
        request.setVerificationId(7L);
        request.setIncludeReportDraft(true);

        AiAnalysisResponseDTO result = service.analyze(request, verification(7L));

        assertThat(result.getVerificationId()).isEqualTo(7L);
        assertThat(result.getSummary()).isEqualTo("summary");
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
        assertThat(result.getModel()).isEqualTo("gemini-1.5-flash");
        assertThat(result.getPromptTokens()).isEqualTo(120);
        assertThat(result.getCompletionTokens()).isEqualTo(80);
        assertThat(result.getTotalTokens()).isEqualTo(200);
        verify(resultMapper).upsert(any(AiAnalysisResultDTO.class));
        server.verify();
    }

    @Test
    void analyzeCompactsSourceAndCachesIdenticalRequest() throws Exception {
        VerifyDTO verification = verification(7L);
        verification.setApiRaw("{\"rawOnlyMarker\":true}");
        verification.setAnalysisJson(objectMapper.writeValueAsString(Map.of(
                "score", 0.82,
                "evidence", "keep this evidence",
                "heatmap", Map.of("data", "A".repeat(3_000))
        )));

        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    JsonNode requestJson = objectMapper.readTree(body);
                    String inputText = requestJson.path("contents").path(0).path("parts").path(0).path("text").asText();
                    JsonNode inputJson = objectMapper.readTree(inputText);
                    JsonNode detectionResult = inputJson.path("detectionResult");
                    JsonNode compactedSource = objectMapper.readTree(
                            detectionResult.path("analysisJson").asText()
                    );

                    assertThat(inputJson.path("request").path("includeReportDraft").asBoolean())
                            .isFalse();
                    assertThat(detectionResult.has("apiRaw")).isFalse();
                    assertThat(compactedSource.path("evidence").asText())
                            .isEqualTo("keep this evidence");
                    assertThat(compactedSource.has("heatmap")).isFalse();
                    assertThat(inputText)
                            .doesNotContain("rawOnlyMarker");
                })
                .andRespond(withSuccess(providerResponse(), MediaType.APPLICATION_JSON));

        AiAnalysisRequestDTO request = new AiAnalysisRequestDTO();
        request.setVerificationId(7L);

        AiAnalysisResponseDTO first = service.analyze(request, verification);
        AiAnalysisResponseDTO second = service.analyze(request, verification);

        assertThat(first.getCached()).isFalse();
        assertThat(second.getCached()).isTrue();
        assertThat(second.getTotalTokens()).isEqualTo(200);
        server.verify();
    }

    @Test
    void analyzeReturnsPersistedResultWithoutApiCall() throws Exception {
        AiAnalysisResponseDTO savedResponse = new AiAnalysisResponseDTO();
        savedResponse.setVerificationId(7L);
        savedResponse.setSummary("saved summary");
        savedResponse.setExplanation("saved explanation");

        AiAnalysisResultDTO saved = new AiAnalysisResultDTO();
        saved.setId(3L);
        saved.setResponseJson(objectMapper.writeValueAsString(savedResponse));
        when(resultMapper.selectByRequestHash(eq(7L), anyString())).thenReturn(saved);

        AiAnalysisRequestDTO request = new AiAnalysisRequestDTO();
        request.setVerificationId(7L);
        AiAnalysisResponseDTO result = service.analyze(request, verification(7L));

        assertThat(result.getSummary()).isEqualTo("saved summary");
        assertThat(result.getCached()).isTrue();
        server.verify();
    }

    @Test
    void analyzeUsesDifferentCacheKeysForDifferentQuestions() throws Exception {
        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andRespond(withSuccess(providerResponse(), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andRespond(withSuccess(providerResponse(), MediaType.APPLICATION_JSON));

        AiAnalysisRequestDTO first = new AiAnalysisRequestDTO();
        first.setVerificationId(7L);
        first.setUserQuestion("첫 번째 질문");
        AiAnalysisRequestDTO second = new AiAnalysisRequestDTO();
        second.setVerificationId(7L);
        second.setUserQuestion("두 번째 질문");

        service.analyze(first, verification(7L));
        service.analyze(second, verification(7L));

        server.verify();
    }

    @Test
    void analyzeRejectsMissingApiKeyBeforeCallingProvider() {
        GeminiAnalysisService serviceWithoutKey = new GeminiAnalysisService(
                RestClient.create("https://generativelanguage.googleapis.com/v1beta"),
                objectMapper,
                "",
                "gemini-1.5-flash",
                450
        );

        assertThatThrownBy(() -> serviceWithoutKey.analyze(new AiAnalysisRequestDTO(), verification(7L)))
                .isInstanceOf(AiAnalysisServiceException.class)
                .extracting(error -> ((AiAnalysisServiceException) error).getCode())
                .isEqualTo("AI-CONFIG");
    }

    @Test
    void analyzeRejectsMismatchedVerificationId() {
        AiAnalysisRequestDTO request = new AiAnalysisRequestDTO();
        request.setVerificationId(8L);

        assertThatThrownBy(() -> service.analyze(request, verification(7L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Verification ID");
    }

    private VerifyDTO verification(Long id) {
        VerifyDTO verification = new VerifyDTO();
        verification.setId(id);
        verification.setOriginalName("sample.mp4");
        verification.setVerdict("SUSPICIOUS");
        verification.setScore(0.82d);
        verification.setApiProvider("Reality Defender");
        verification.setApiRaw("{\"status\":\"ready\"}");
        verification.setAnalysisJson("{\"score\":0.82}");
        verification.setRegDt("2026-08-21 10:00:00");
        return verification;
    }

    private String providerResponse() throws Exception {
        String outputText = objectMapper.writeValueAsString(Map.of(
                "summary", "summary",
                "explanation", "explanation",
                "reportDraft", "report draft",
                "actionGuide", "action guide",
                "riskLevel", "HIGH",
                "disclaimer", "not definitive proof"
        ));
        return objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "finishReason", "STOP",
                        "content", Map.of(
                                "parts", List.of(Map.of("text", outputText))
                        )
                )),
                "usageMetadata", Map.of(
                        "promptTokenCount", 120,
                        "candidatesTokenCount", 80,
                        "totalTokenCount", 200
                )
        ));
    }
}

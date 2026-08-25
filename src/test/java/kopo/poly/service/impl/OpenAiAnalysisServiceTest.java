package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.AiAnalysisRequestDTO;
import kopo.poly.dto.AiAnalysisResponseDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.AiAnalysisServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiAnalysisServiceTest {

    private ObjectMapper objectMapper;
    private MockRestServiceServer server;
    private OpenAiAnalysisService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.openai.com/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new OpenAiAnalysisService(
                builder.build(),
                objectMapper,
                "test-api-key",
                "gpt-4.1-mini",
                450
        );
    }

    @Test
    void analyzeSendsStructuredRequestAndMapsResponse() throws Exception {
        String providerResponse = providerResponse();

        server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    JsonNode requestJson = objectMapper.readTree(body);
                    JsonNode inputJson = objectMapper.readTree(requestJson.path("input").asText());

                    assertThat(requestJson.path("store").asBoolean()).isFalse();
                    assertThat(requestJson.path("max_output_tokens").asInt()).isEqualTo(450);
                    assertThat(requestJson.path("text").path("format").path("type").asText())
                            .isEqualTo("json_schema");
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
        assertThat(result.getModel()).isEqualTo("gpt-4.1-mini-2025-04-14");
        assertThat(result.getPromptTokens()).isEqualTo(120);
        assertThat(result.getCompletionTokens()).isEqualTo(80);
        assertThat(result.getTotalTokens()).isEqualTo(200);
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

        server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    JsonNode requestJson = objectMapper.readTree(body);
                    JsonNode inputJson = objectMapper.readTree(requestJson.path("input").asText());
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
                    assertThat(requestJson.path("input").asText())
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
    void analyzeRejectsMissingApiKeyBeforeCallingProvider() {
        OpenAiAnalysisService serviceWithoutKey = new OpenAiAnalysisService(
                RestClient.create("https://api.openai.com/v1"),
                objectMapper,
                "",
                "gpt-4.1-mini",
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
                "status", "completed",
                "model", "gpt-4.1-mini-2025-04-14",
                "output", List.of(Map.of(
                        "type", "message",
                        "content", List.of(Map.of(
                                "type", "output_text",
                                "text", outputText
                        ))
                )),
                "usage", Map.of(
                        "input_tokens", 120,
                        "output_tokens", 80,
                        "total_tokens", 200
                )
        ));
    }
}

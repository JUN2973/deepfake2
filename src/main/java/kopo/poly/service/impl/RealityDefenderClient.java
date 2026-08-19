package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별): Reality Defender 외부 API 요청과 응답 변환을 담당한다.
 */

/**
 * 발표용 설명: Reality Defender 외부 API와 직접 통신하는 어댑터입니다.
 * 서비스 내부 로직이 API 세부 규격에 의존하지 않도록 요청/응답 변환을 이 파일에 모았습니다.
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.DeepfakeResultDTO;
import kopo.poly.service.IDeepfakeClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.Map;

/**
 * Reality Defender 실제 API에 파일을 업로드하고 분석 결과를 조회하는 클라이언트다.
 */
@Component
@ConditionalOnProperty(name = "deepfake.client.mode", havingValue = "real")
public class RealityDefenderClient implements IDeepfakeClient {

    private static final Logger log = LoggerFactory.getLogger(RealityDefenderClient.class);

    @Value("${rd.baseUrl}")
    private String baseUrl;

    @Value("${rd.apiKey:}")
    private String apiKey;

    @Value("${rd.poll.max-attempts:100}")
    private int pollMaxAttempts;

    @Value("${rd.poll.interval-ms:3000}")
    private long pollIntervalMs;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public DeepfakeResultDTO analyze(Path savedFilePath) throws Exception {
        // 실제 API 모드다. 업로드 URL을 받고 파일을 올린 뒤 결과가 나올 때까지 조회한다.
        validateConfiguration();

        JsonNode presign = requestPresignedUrl(savedFilePath.getFileName().toString());
        String uploadUrl = firstText(
                presign,
                "/url",
                "/uploadUrl",
                "/presignedUrl",
                "/signedUrl",
                "/response/url",
                "/response/uploadUrl",
                "/response/presignedUrl",
                "/response/signedUrl",
                "/data/url"
        );
        String requestId = firstText(
                presign,
                "/requestId",
                "/request_id",
                "/id",
                "/mediaId",
                "/response/requestId",
                "/response/request_id",
                "/response/id",
                "/response/mediaId",
                "/data/requestId",
                "/data/request_id"
        );

        if (!StringUtils.hasText(uploadUrl) || !StringUtils.hasText(requestId)) {
            throw new IllegalStateException("Reality Defender presigned response missing uploadUrl/requestId: " + presign);
        }

        uploadFile(uploadUrl, savedFilePath);

        JsonNode result = pollResult(requestId);
        String status = firstText(
                result,
                "/resultsSummary/status",
                "/status",
                "/result/status",
                "/data/status"
        );
        String reason = firstText(
                result,
                "/resultsSummary/reason",
                "/resultsSummary/metadata/reason",
                "/reason",
                "/message",
                "/data/reason",
                "/error/message"
        );
        String verdict = normalizeVerdict(status, reason);
        Double finalScore100 = extractFinalScore(result);
        Double riskScore = toManipulationRiskScore(verdict, finalScore100);

        return new DeepfakeResultDTO(
                verdict,
                isNotApplicable(verdict) ? null : riskScore,
                om.writeValueAsString(result)
        );
    }

    private JsonNode requestPresignedUrl(String originalName) throws Exception {
        String url = trimTrailingSlash(baseUrl) + "/api/files/aws-presigned";

        HttpHeaders headers = apiHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String payload = om.writeValueAsString(Map.of("fileName", originalName));

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(url)
                    .headers(requestHeaders -> requestHeaders.addAll(headers))
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);
            return parseBody(response.getBody(), "presigned URL");
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Reality Defender presigned URL request failed: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    private void uploadFile(String uploadUrl, Path savedFilePath) throws Exception {
        byte[] bytes = Files.readAllBytes(savedFilePath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        try {
            ResponseEntity<String> response = restClient.put()
                    .uri(URI.create(uploadUrl))
                    .headers(requestHeaders -> requestHeaders.addAll(headers))
                    .body(bytes)
                    .retrieve()
                    .toEntity(String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Reality Defender upload failed: " + response.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Reality Defender upload failed: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    private JsonNode pollResult(String requestId) throws Exception {
        String url = trimTrailingSlash(baseUrl) + "/api/media/users/" + requestId;
        HttpHeaders headers = apiHeaders();

        // Reality Defender 분석은 비동기이므로 최종 상태가 될 때까지 반복 조회한다.
        JsonNode lastBody = null;
        String lastStatus = null;

        for (int i = 0; i < pollMaxAttempts; i++) {
            try {
                ResponseEntity<String> response = restClient.get()
                        .uri(url)
                        .headers(requestHeaders -> requestHeaders.addAll(headers))
                        .retrieve()
                        .toEntity(String.class);
                JsonNode body = parseBody(response.getBody(), "media detail");
                lastBody = body;
                String status = firstText(
                        body,
                        "/resultsSummary/status",
                        "/status",
                        "/result/status",
                        "/data/status"
                );
                lastStatus = status;
                if (isTerminalStatus(status)) {
                    log.info("Reality Defender result ready. requestId={}, status={}", requestId, status);
                    return body;
                }
                log.info("Reality Defender result pending. requestId={}, attempt={}/{}, status={}",
                        requestId, i + 1, pollMaxAttempts, status);
            } catch (RestClientResponseException e) {
                throw new IllegalStateException("Reality Defender result request failed: "
                        + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
            }

            Thread.sleep(pollIntervalMs);
        }

        String lastBodyText = lastBody == null ? "null" : lastBody.toString();
        throw new IllegalStateException("Reality Defender result polling timeout for requestId="
                + requestId + ", attempts=" + pollMaxAttempts + ", intervalMs=" + pollIntervalMs
                + ", lastStatus=" + lastStatus + ", lastBody=" + lastBodyText);
    }

    private static boolean isTerminalStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }

        String normalized = status.toUpperCase();
        return !normalized.equals("ANALYZING")
                && !normalized.equals("PROCESSING")
                && !normalized.equals("PENDING")
                && !normalized.equals("QUEUED")
                && !normalized.equals("UPLOADED");
    }

    private HttpHeaders apiHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        return headers;
    }

    private JsonNode parseBody(String body, String operation) throws Exception {
        if (!StringUtils.hasText(body)) {
            throw new IllegalStateException("Reality Defender " + operation + " response is empty.");
        }
        return om.readTree(body);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Reality Defender base URL is not configured.");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Reality Defender API key is not configured. Set RD_API_KEY.");
        }
    }

    private static String normalizeVerdict(String status, String reason) {
        if (looksNotApplicable(reason)) {
            return "NOT_APPLICABLE";
        }

        if (!StringUtils.hasText(status)) {
            return "UNKNOWN";
        }
        return switch (status.toUpperCase()) {
            case "AUTHENTIC" -> "REAL";
            case "REAL" -> "REAL";
            case "FAKE" -> "FAKE";
            case "MANIPULATED" -> "FAKE";
            case "SUSPICIOUS" -> "SUSPICIOUS";
            case "NOT_APPLICABLE" -> "NOT_APPLICABLE";
            case "UNABLE_TO_EVALUATE" -> "UNABLE_TO_EVALUATE";
            default -> status.toUpperCase();
        };
    }

    private static boolean looksNotApplicable(String reason) {
        if (!StringUtils.hasText(reason)) {
            return false;
        }

        String normalized = reason.toLowerCase();
        return (normalized.contains("face") && (normalized.contains("not found")
                || normalized.contains("missing")
                || normalized.contains("too small")
                || normalized.contains("multiple")))
                || normalized.contains("not applicable")
                || normalized.contains("unable to evaluate")
                || normalized.contains("blurry")
                || normalized.contains("blurred")
                || normalized.contains("얼굴")
                || normalized.contains("사람")
                || normalized.contains("흐릿");
    }

    private static boolean isNotApplicable(String verdict) {
        return "NOT_APPLICABLE".equals(verdict) || "UNABLE_TO_EVALUATE".equals(verdict);
    }

    private static Double extractFinalScore(JsonNode result) {
        return firstNumber(
                result,
                "/resultsSummary/metadata/finalScore",
                "/resultsSummary/finalScore",
                "/finalScore",
                "/score",
                "/data/finalScore",
                "/data/score",
                "/result/finalScore",
                "/result/score"
        );
    }

    private static Double toManipulationRiskScore(String verdict, Double finalScore100) {
        if (finalScore100 == null) {
            return null;
        }

        double normalized = finalScore100 > 1.0 ? finalScore100 / 100.0 : finalScore100;
        normalized = clamp(normalized);

        // detail.jsp는 score를 "조작 판단률"로 표시한다.
        // API가 AUTHENTIC/REAL 신뢰도에 높은 점수를 주면 그대로 쓰면 REAL 이미지가 고위험처럼 보이므로 위험도로 뒤집는다.
        if ("REAL".equals(verdict)) {
            return clamp(1.0 - normalized);
        }

        return normalized;
    }

    private static String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    private static String firstText(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            String value = text(node, pointer);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String pointer) {
        JsonNode found = node.at(pointer);
        if (found.isMissingNode() || found.isNull()) {
            return null;
        }
        String value = found.asText();
        return StringUtils.hasText(value) ? value : null;
    }

    private static Double doubleValue(JsonNode node, String pointer) {
        JsonNode found = node.at(pointer);
        if (found.isMissingNode() || found.isNull()) {
            return null;
        }
        if (found.isNumber()) {
            return found.doubleValue();
        }
        try {
            return Double.parseDouble(found.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double firstNumber(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            Double value = doubleValue(node, pointer);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Double clamp(Double value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            return 0.0;
        }
        if (value > 1) {
            return 1.0;
        }
        return value;
    }
}

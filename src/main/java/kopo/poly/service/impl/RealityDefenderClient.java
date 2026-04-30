package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.DeepfakeResultDTO;
import kopo.poly.service.IDeepfakeClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate rt = new RestTemplate();
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
        Double finalScore100 = doubleValue(result, "/resultsSummary/metadata/finalScore");
        String verdict = normalizeVerdict(status, reason);

        return new DeepfakeResultDTO(
                verdict,
                isNotApplicable(verdict) || finalScore100 == null ? null : clamp(finalScore100 / 100.0),
                om.writeValueAsString(result)
        );
    }

    private JsonNode requestPresignedUrl(String originalName) throws Exception {
        String url = trimTrailingSlash(baseUrl) + "/api/files/aws-presigned";

        HttpHeaders headers = apiHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String payload = om.writeValueAsString(Map.of("fileName", originalName));

        try {
            ResponseEntity<String> response = rt.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            return parseBody(response.getBody(), "presigned URL");
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("Reality Defender presigned URL request failed: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    private void uploadFile(String uploadUrl, Path savedFilePath) throws Exception {
        byte[] bytes = Files.readAllBytes(savedFilePath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        try {
            ResponseEntity<String> response = rt.exchange(
                    URI.create(uploadUrl),
                    HttpMethod.PUT,
                    new HttpEntity<>(bytes, headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Reality Defender upload failed: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("Reality Defender upload failed: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    private JsonNode pollResult(String requestId) throws Exception {
        String url = trimTrailingSlash(baseUrl) + "/api/media/users/" + requestId;
        HttpEntity<Void> entity = new HttpEntity<>(apiHeaders());

        // Reality Defender 분석은 비동기이므로 최종 상태가 될 때까지 반복 조회한다.
        for (int i = 0; i < 60; i++) {
            try {
                ResponseEntity<String> response = rt.exchange(url, HttpMethod.GET, entity, String.class);
                JsonNode body = parseBody(response.getBody(), "media detail");
                String status = text(body, "/resultsSummary/status");
                if (isTerminalStatus(status)) {
                    log.info("Reality Defender result ready. requestId={}, status={}", requestId, status);
                    return body;
                }
            } catch (HttpStatusCodeException e) {
                throw new IllegalStateException("Reality Defender result request failed: "
                        + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
            }

            Thread.sleep(1000);
        }

        throw new IllegalStateException("Reality Defender result polling timeout for requestId=" + requestId);
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

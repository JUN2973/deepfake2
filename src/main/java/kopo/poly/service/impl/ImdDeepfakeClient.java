package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별): IMD 오픈소스 분석 서비스 호출과 결과 변환을 담당한다.
 */

/**
 * 발표용 설명: IMD 오픈소스 분석 모델과 연동하는 클라이언트입니다.
 * 외부 모델 응답을 프로젝트 공통 결과 DTO로 변환해 화면과 DB 로직이 동일한 방식으로 처리하게 합니다.
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.DeepfakeResultDTO;
import kopo.poly.service.IDeepfakeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "deepfake.client.mode", havingValue = "imd")
public class ImdDeepfakeClient implements IDeepfakeClient {

    // Python FastAPI IMD 서버 주소다. 기본값은 로컬 서버이고, 환경변수/설정으로 바꿀 수 있다.
    @Value("${imd.base-url:http://127.0.0.1:18080}")
    private String baseUrl;

    @Value("${imd.request-timeout-ms:45000}")
    private long requestTimeoutMs;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ImdLocalServiceManager imdLocalServiceManager;

    public ImdDeepfakeClient(RestClient restClient,
                             ObjectMapper objectMapper,
                             ImdLocalServiceManager imdLocalServiceManager) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.imdLocalServiceManager = imdLocalServiceManager;
    }

    @Override
    public DeepfakeResultDTO analyze(Path savedFilePath) throws Exception {
        // 이 클래스는 IMD를 "주 분석기"로 사용할 때만 동작한다.
        // 파일을 Python 서버의 /analyze로 보내고, 응답을 프로젝트 공통 결과 DTO로 바꾼다.
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("IMD base URL is not configured.");
        }

        String requestUrl = trimTrailingSlash(baseUrl) + "/analyze";
        // 로컬 IMD 서버가 꺼져 있으면 설정에 따라 start.ps1로 먼저 실행을 시도한다.
        imdLocalServiceManager.ensureAvailable(baseUrl);
        byte[] bytes = Files.readAllBytes(savedFilePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return savedFilePath.getFileName().toString();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        String responseBody;
        try {
            // FastAPI는 multipart/form-data의 file 필드로 이미지를 받는다.
            byte[] responseBytes = imdRequestClient().post()
                    .uri(requestUrl)
                    .headers(requestHeaders -> requestHeaders.addAll(headers))
                    .body(body)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            String errorBody = new String(StreamUtils.copyToByteArray(response.getBody()), StandardCharsets.UTF_8);
                            throw new IllegalStateException("IMD analysis request failed: "
                                    + response.getStatusCode() + " " + errorBody);
                        }
                        return StreamUtils.copyToByteArray(response.getBody());
                    });
            responseBody = responseBodyAsString(responseBytes);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("IMD analysis request failed: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }

        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalStateException("IMD analysis response is empty.");
        }

        JsonNode root = objectMapper.readTree(responseBody);
        // Python 서버 응답 필드명이 조금 바뀌어도 대응할 수 있도록 여러 후보 경로를 확인한다.
        String verdict = normalizeVerdict(firstText(root, "/verdict", "/status", "/result/verdict", "/data/verdict"));
        Double score = normalizeScore(firstNumber(root, "/score", "/riskScore", "/finalScore", "/data/score"));

        if (isNotApplicable(verdict)) {
            score = null;
        }

        return new DeepfakeResultDTO(verdict, score, responseBody);
    }

    private String responseBodyAsString(byte[] body) {
        return body == null || body.length == 0 ? "" : new String(body, StandardCharsets.UTF_8);
    }

    private String normalizeVerdict(String value) {
        // IMD/외부 모델이 사용하는 판정 문자열을 화면에서 쓰는 REAL/FAKE/SUSPICIOUS 형식으로 맞춘다.
        if (!StringUtils.hasText(value)) {
            return "UNKNOWN";
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "AUTHENTIC", "REAL", "SAFE" -> "REAL";
            case "MANIPULATED", "FAKE", "HIGH_RISK" -> "FAKE";
            case "SUSPECT", "SUSPICIOUS" -> "SUSPICIOUS";
            case "NOT_APPLICABLE", "UNABLE_TO_EVALUATE" -> normalized;
            default -> normalized;
        };
    }

    private boolean isNotApplicable(String verdict) {
        return "NOT_APPLICABLE".equals(verdict) || "UNABLE_TO_EVALUATE".equals(verdict);
    }

    private Double normalizeScore(Double score) {
        // 모델 점수는 0~1 또는 0~100으로 올 수 있으므로 항상 0~1 범위로 정리한다.
        if (score == null) {
            return null;
        }
        double normalized = score > 1.0 ? score / 100.0 : score;
        if (normalized < 0.0) {
            return 0.0;
        }
        if (normalized > 1.0) {
            return 1.0;
        }
        return normalized;
    }

    private String firstText(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            JsonNode found = node.at(pointer);
            if (!found.isMissingNode() && !found.isNull() && StringUtils.hasText(found.asText())) {
                return found.asText();
            }
        }
        return null;
    }

    private Double firstNumber(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            JsonNode found = node.at(pointer);
            if (found.isMissingNode() || found.isNull()) {
                continue;
            }
            if (found.isNumber()) {
                return found.doubleValue();
            }
            try {
                return Double.parseDouble(found.asText());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    private RestClient imdRequestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(3000));
        factory.setReadTimeout(Duration.ofMillis(Math.max(5000, requestTimeoutMs)));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}

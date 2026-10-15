package kopo.poly.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * 체크리스트 기준 주석
 * - 외부 API 실패 처리: Gemini가 일시 장애(500/502/503/504)를 돌려주면 지수 백오프로 재시도한다.
 * - 주 모델이 계속 과부하(503)면 설정된 대체 모델(gemini.fallback-model, 쉼표로 여러 개)로 차례대로 시도한다.
 * - 429(한도 초과)는 같은 모델로 재시도하지 않고 바로 다음 대체 모델로 넘어간다(무료 한도는 모델별로 따로 계산됨).
 * - 401/403/400 같은 재시도해도 소용없는 오류는 즉시 그대로 던진다.
 *
 * 발표용 설명
 * - Gemini 503은 "모델 과부하(UNAVAILABLE)"로, 잠시 뒤 다시 보내면 대부분 성공한다.
 * - 1초 → 2초 → 4초(최대치)처럼 기다리는 시간을 늘리고 약간의 무작위 지연(jitter)을 더해 동시 재시도가 몰리지 않게 한다.
 */
class GeminiRetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(GeminiRetryPolicy.class);
    // 일시 장애로 보고 재시도하는 HTTP 상태 코드
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(500, 502, 503, 504);
    // 같은 모델 재시도는 의미 없지만 다른 모델로는 넘어갈 수 있는 상태 코드(모델별 한도 초과)
    private static final int QUOTA_EXCEEDED = 429;

    // 테스트에서 실제로 기다리지 않도록 대기 동작을 바꿔 끼울 수 있게 분리
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final int maxAttempts;       // 모델 하나당 최대 시도 횟수(첫 시도 포함)
    private final long initialBackoffMs; // 첫 재시도 전 대기 시간
    private final long maxBackoffMs;     // 대기 시간 상한
    private final List<String> fallbackModels; // 주 모델이 끝내 실패하면 순서대로 쓸 대체 모델(비어 있으면 사용 안 함)
    private final Sleeper sleeper;

    GeminiRetryPolicy(int maxAttempts, long initialBackoffMs, long maxBackoffMs, String fallbackModel) {
        this(maxAttempts, initialBackoffMs, maxBackoffMs, fallbackModel, Thread::sleep);
    }

    GeminiRetryPolicy(int maxAttempts, long initialBackoffMs, long maxBackoffMs, String fallbackModel, Sleeper sleeper) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMs = Math.max(0, initialBackoffMs);
        this.maxBackoffMs = Math.max(this.initialBackoffMs, maxBackoffMs);
        this.fallbackModels = new ArrayList<>();
        if (fallbackModel != null) {
            for (String name : fallbackModel.split(",")) {
                if (!name.isBlank()) {
                    this.fallbackModels.add(name.trim());
                }
            }
        }
        this.sleeper = sleeper;
    }

    // 재시도 없이 한 번만 호출하는 정책(테스트·기존 생성자 호환용)
    static GeminiRetryPolicy noRetry() {
        return new GeminiRetryPolicy(1, 0, 0, "");
    }

    /**
     * 주 모델 → 대체 모델들 순서로 call을 실행한다. call에는 이번에 사용할 모델 이름이 전달된다.
     * 모든 모델이 실패하면 마지막 예외를 그대로 던진다.
     */
    <T> T execute(String primaryModel, Function<String, T> call) {
        List<String> models = new ArrayList<>();
        models.add(primaryModel);
        for (String fallback : fallbackModels) {
            if (!models.contains(fallback)) {
                models.add(fallback);
            }
        }

        RestClientResponseException lastError = null;
        for (int index = 0; index < models.size(); index++) {
            String model = models.get(index);
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return call.apply(model);
                } catch (RestClientResponseException e) {
                    int status = e.getStatusCode().value();
                    if (status == QUOTA_EXCEEDED) {
                        lastError = e;
                        log.warn("Gemini quota exceeded model={}", model);
                        break; // 이 모델의 한도는 끝났으니 바로 다음 모델로
                    }
                    if (!RETRYABLE_STATUSES.contains(status)) {
                        throw e; // 키 오류·요청 오류는 재시도해도 같은 결과
                    }
                    lastError = e;
                    log.warn("Gemini temporary failure status={} model={} attempt={}/{}",
                            status, model, attempt, maxAttempts);
                    if (attempt < maxAttempts) {
                        waitBeforeRetry(attempt, e);
                    }
                }
            }
            if (index < models.size() - 1) {
                log.warn("Gemini model {} failed. Switching to fallback model {}.", model, models.get(index + 1));
            }
        }
        throw lastError;
    }

    // 모델 세대에 따라 생각(추론) 설정 이름이 달라서 나눠 처리한다. 둘 다 "생각을 최소화"하는 설정.
    static Map<String, Object> thinkingConfigFor(String model) {
        if (model != null && model.toLowerCase().startsWith("gemini-3")) {
            return Map.of("thinkingLevel", "minimal");
        }
        return Map.of("thinkingBudget", 0);
    }

    // 대체 모델로 보낼 때는 그 모델 세대에 맞는 thinkingConfig로 바꾼 사본을 만든다(원본·캐시 키는 그대로).
    @SuppressWarnings("unchecked")
    static Map<String, Object> bodyForModel(Map<String, Object> body, String model) {
        Object config = body.get("generationConfig");
        if (!(config instanceof Map<?, ?>)) {
            return body;
        }
        Map<String, Object> generationConfig = new LinkedHashMap<>((Map<String, Object>) config);
        generationConfig.put("thinkingConfig", thinkingConfigFor(model));
        Map<String, Object> copy = new LinkedHashMap<>(body);
        copy.put("generationConfig", generationConfig);
        return copy;
    }

    // 지수 백오프 + jitter. 서버가 Retry-After(초)를 주면 상한 안에서 그 값을 우선한다.
    private void waitBeforeRetry(int attempt, RestClientResponseException e) {
        long delay = Math.min(maxBackoffMs, initialBackoffMs << Math.min(attempt - 1, 20));
        Long retryAfter = retryAfterMillis(e);
        if (retryAfter != null) {
            delay = Math.min(maxBackoffMs, Math.max(delay, retryAfter));
        }
        if (delay > 0) {
            delay += ThreadLocalRandom.current().nextLong(Math.max(1, delay / 4) + 1);
        }
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw e; // 대기 중 요청이 취소되면 더 시도하지 않는다
        }
    }

    private Long retryAfterMillis(RestClientResponseException e) {
        HttpHeaders headers = e.getResponseHeaders();
        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim()) * 1000L;
        } catch (NumberFormatException ignored) {
            return null; // HTTP 날짜 형식은 무시하고 기본 백오프 사용
        }
    }
}

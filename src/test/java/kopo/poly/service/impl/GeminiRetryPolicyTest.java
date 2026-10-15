package kopo.poly.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiRetryPolicyTest {

    private final List<Long> sleeps = new ArrayList<>();

    @Test
    void retriesOn503ThenSucceeds() {
        GeminiRetryPolicy policy = new GeminiRetryPolicy(3, 1000, 4000, "", sleeps::add);
        List<String> calls = new ArrayList<>();

        String result = policy.execute("primary", model -> {
            calls.add(model);
            if (calls.size() < 3) {
                throw HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "busy", null, null, null);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).containsExactly("primary", "primary", "primary");
        assertThat(sleeps).hasSize(2);
        // 1초 → 2초로 늘어나고 jitter는 최대 25%
        assertThat(sleeps.get(0)).isBetween(1000L, 1250L);
        assertThat(sleeps.get(1)).isBetween(2000L, 2500L);
    }

    @Test
    void switchesToFallbackModelAfterPrimaryExhausted() {
        GeminiRetryPolicy policy = new GeminiRetryPolicy(2, 0, 0, "backup", sleeps::add);
        List<String> calls = new ArrayList<>();

        String result = policy.execute("primary", model -> {
            calls.add(model);
            if ("primary".equals(model)) {
                throw HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "busy", null, null, null);
            }
            return model;
        });

        assertThat(result).isEqualTo("backup");
        assertThat(calls).containsExactly("primary", "primary", "backup");
    }

    @Test
    void doesNotRetryClientErrors() {
        GeminiRetryPolicy policy = new GeminiRetryPolicy(3, 1000, 4000, "backup", sleeps::add);
        List<String> calls = new ArrayList<>();

        assertThatThrownBy(() -> policy.execute("primary", model -> {
            calls.add(model);
            throw HttpClientErrorException.create(HttpStatus.FORBIDDEN, "bad key", null, null, null);
        })).isInstanceOf(HttpClientErrorException.class);

        assertThat(calls).containsExactly("primary");
        assertThat(sleeps).isEmpty();
    }

    @Test
    void quotaExceededSkipsToNextFallbackWithoutWaiting() {
        GeminiRetryPolicy policy = new GeminiRetryPolicy(3, 1000, 4000, "backup1, backup2", sleeps::add);
        List<String> calls = new ArrayList<>();

        String result = policy.execute("primary", model -> {
            calls.add(model);
            if (!"backup2".equals(model)) {
                throw HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "quota", null, null, null);
            }
            return model;
        });

        // 429는 같은 모델로 재시도하지 않고 다음 모델로 바로 넘어간다.
        assertThat(result).isEqualTo("backup2");
        assertThat(calls).containsExactly("primary", "backup1", "backup2");
        assertThat(sleeps).isEmpty();
    }

    @Test
    void quotaExceededOnAllModelsThrowsLastError() {
        GeminiRetryPolicy policy = new GeminiRetryPolicy(3, 1000, 4000, "backup", sleeps::add);

        assertThatThrownBy(() -> policy.execute("primary", model -> {
            throw HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "quota", null, null, null);
        })).isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    void honorsRetryAfterWithinMaxBackoff() {
        GeminiRetryPolicy policy = new GeminiRetryPolicy(2, 500, 3000, "", sleeps::add);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "10");

        assertThatThrownBy(() -> policy.execute("primary", model -> {
            throw HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "busy", headers, null, null);
        })).isInstanceOf(HttpServerErrorException.class);

        // Retry-After 10초는 상한 3초로 잘린다(+ jitter)
        assertThat(sleeps).hasSize(1);
        assertThat(sleeps.get(0)).isBetween(3000L, 3750L);
    }

    @Test
    void bodyForModelSwapsThinkingConfigWithoutTouchingOriginal() {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("thinkingConfig", GeminiRetryPolicy.thinkingConfigFor("gemini-2.5-flash"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("generationConfig", generationConfig);

        Map<String, Object> copy = GeminiRetryPolicy.bodyForModel(body, "gemini-3.1-flash-lite");

        assertThat(((Map<?, ?>) copy.get("generationConfig")).get("thinkingConfig"))
                .isEqualTo(Map.of("thinkingLevel", "minimal"));
        assertThat(generationConfig.get("thinkingConfig")).isEqualTo(Map.of("thinkingBudget", 0));
    }
}

package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 개발환경 세팅: 로컬 IMD 분석 서비스 프로세스 실행 상태를 관리한다.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@Component
public class ImdLocalServiceManager {

    private static final Logger log = LoggerFactory.getLogger(ImdLocalServiceManager.class);

    private final RestClient restClient;
    private final boolean autoStartEnabled;
    private final Path serviceDirectory;
    private final Path repoPath;
    private final Duration startupTimeout;

    private volatile boolean startAttempted;

    public ImdLocalServiceManager(
            RestClient restClient,
            @Value("${imd.auto-start.enabled:true}") boolean autoStartEnabled,
            @Value("${imd.service-dir:imd-service}") String serviceDirectory,
            @Value("${imd.repo-path:C:\\SpringBootWorks\\IMD}") String repoPath,
            @Value("${imd.startup-timeout-ms:15000}") long startupTimeoutMs) {
        this.restClient = restClient;
        this.autoStartEnabled = autoStartEnabled;
        this.serviceDirectory = Path.of(serviceDirectory).toAbsolutePath().normalize();
        this.repoPath = Path.of(repoPath).toAbsolutePath().normalize();
        this.startupTimeout = Duration.ofMillis(Math.max(1000, startupTimeoutMs));
    }

    public void ensureAvailable(String baseUrl) {
        // IMD 서버가 외부 서버이거나 자동 시작이 꺼져 있으면 상태만 확인하고 바로 빠진다.
        // 로컬 서버가 내려간 경우에만 start.ps1 실행을 시도한다.
        if (!autoStartEnabled || !StringUtils.hasText(baseUrl) || !isLocalhost(baseUrl) || isHealthy(baseUrl)) {
            return;
        }

        synchronized (this) {
            // 동시에 여러 이미지 분석 요청이 들어와도 서버 시작은 한 번만 시도한다.
            if (isHealthy(baseUrl)) {
                return;
            }
            if (!startAttempted) {
                startAttempted = true;
                startService(baseUrl);
            }
        }

        waitUntilHealthy(baseUrl);
    }

    private void startService(String baseUrl) {
        // imd-service/start.ps1을 실행해 FastAPI 서버를 백그라운드로 띄운다.
        // 실제 IMD 모델 저장소 위치는 IMD_REPO 환경변수로 Python 프로세스에 넘긴다.
        Path script = serviceDirectory.resolve("start.ps1");
        if (!Files.isRegularFile(script)) {
            throw new IllegalStateException("IMD start script not found: " + script);
        }
        if (!Files.isDirectory(repoPath)) {
            throw new IllegalStateException("IMD repository not found: " + repoPath);
        }

        try {
            Path logFile = serviceDirectory.resolve("imd-service.log");
            ProcessBuilder builder = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    script.toString()
            );
            builder.directory(serviceDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

            Map<String, String> env = builder.environment();
            env.putIfAbsent("IMD_REPO", repoPath.toString());
            env.put("IMD_BASE_URL", baseUrl);

            builder.start();
            log.info("Started local IMD service from {}", script);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start local IMD service.", e);
        }
    }

    private void waitUntilHealthy(String baseUrl) {
        // 서버 프로세스를 실행한 뒤 /health가 2xx를 반환할 때까지 짧게 반복 확인한다.
        long deadline = System.nanoTime() + startupTimeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (isHealthy(baseUrl)) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for IMD service startup.", e);
            }
        }
        throw new IllegalStateException("IMD service did not become healthy within "
                + startupTimeout.toMillis() + " ms. Check " + serviceDirectory.resolve("imd-service.log"));
    }

    private boolean isHealthy(String baseUrl) {
        // Python FastAPI 서버의 health check 엔드포인트다.
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(trimTrailingSlash(baseUrl) + "/health")
                    .retrieve()
                    .toEntity(String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }

    private boolean isLocalhost(String baseUrl) {
        // 자동 시작은 개발 PC의 로컬 IMD 서버에만 적용한다.
        // 외부 URL까지 임의로 실행하려고 하면 안 되므로 localhost 계열만 허용한다.
        try {
            String host = URI.create(baseUrl).getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            return "localhost".equals(normalized) || "127.0.0.1".equals(normalized) || "::1".equals(normalized);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }
}

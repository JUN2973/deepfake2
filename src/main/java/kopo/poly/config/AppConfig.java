package kopo.poly.config;


/**
 * 체크리스트 기준 주석: 개발환경 세팅/설계: 보안, MVC, DB 스키마, 공통 Bean 설정을 담당한다.
 */
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 여러 서비스에서 공통으로 사용할 RestClient 설정 클래스다.
 */
@Configuration
public class AppConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder,
                                 @Value("${app.http.connect-timeout-ms:3000}") int connectTimeoutMs,
                                 @Value("${app.http.read-timeout-ms:5000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder
                .requestFactory(factory)
                .build();
    }
}

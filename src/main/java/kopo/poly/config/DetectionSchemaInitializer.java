package kopo.poly.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 탐지 결과 저장에 필요한 DB 스키마를 시작 시 확인하고 생성한다.
 */
@Configuration
public class DetectionSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(DetectionSchemaInitializer.class);

    @Bean
    public ApplicationRunner detectionSchemaRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS verification_record (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            user_id BIGINT NULL,
                            original_filename VARCHAR(255) NOT NULL,
                            saved_path VARCHAR(500) NOT NULL,
                            detect_status VARCHAR(40) NOT NULL,
                            final_score INT NULL,
                            explanation_text TEXT NULL,
                            detailed_reason TEXT NULL,
                            not_applicable_reason TEXT NULL,
                            overlay_source VARCHAR(30) NOT NULL DEFAULT 'NONE',
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                log.info("Ensured verification_record schema is ready.");
            } catch (Exception e) {
                // DB 접속이 불안정해도 애플리케이션 전체 기동은 유지한다.
                log.warn("Skipping verification_record schema init because DB is unavailable.", e);
            }
        };
    }
}

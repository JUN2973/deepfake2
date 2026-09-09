package kopo.poly.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 오탐 신고 및 재검토 요청 테이블을 애플리케이션 시작 시 준비한다.
 */
@Configuration
public class ReviewRequestSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(ReviewRequestSchemaInitializer.class);

    @Bean
    public ApplicationRunner reviewRequestSchemaRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS review_request (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            verification_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            request_type VARCHAR(30) NOT NULL,
                            reason VARCHAR(1000) NOT NULL,
                            status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                            reviewer_note VARCHAR(1000) NULL,
                            reg_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            upd_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_review_request_user
                        ON review_request(user_id, id)
                        """);

                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_review_request_verification
                        ON review_request(verification_id, status)
                        """);

                log.info("Ensured review request schema is ready.");
            } catch (Exception e) {
                log.warn("Skipping review request schema init because DB is unavailable.", e);
            }
        };
    }
}

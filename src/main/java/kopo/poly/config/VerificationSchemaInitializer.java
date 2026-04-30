package kopo.poly.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 업로드 검증 이력 테이블을 시작 시 준비하는 설정 클래스다.
 */
@Configuration
public class VerificationSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(VerificationSchemaInitializer.class);

    @Bean
    public ApplicationRunner verificationSchemaRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                        ALTER TABLE verification
                        ADD COLUMN IF NOT EXISTS user_id BIGINT NULL
                        """);

                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_verification_user_id
                        ON verification(user_id)
                        """);

                log.info("Ensured verification schema includes user_id.");
            } catch (Exception e) {
                log.warn("Skipping verification schema init because DB is unavailable.", e);
            }
        };
    }
}

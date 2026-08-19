package kopo.poly.config;


/**
 * 체크리스트 기준 주석: 개발환경 세팅/설계: 보안, MVC, DB 스키마, 공통 Bean 설정을 담당한다.
 */
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
                        CREATE TABLE IF NOT EXISTS verification (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            user_id BIGINT NULL,
                            original_name VARCHAR(255) NOT NULL,
                            mime_type VARCHAR(100) NULL,
                            file_size BIGINT NULL,
                            object_key VARCHAR(500) NULL,
                            public_url VARCHAR(1000) NULL,
                            verdict VARCHAR(40) NULL,
                            score DOUBLE NULL,
                            api_provider VARCHAR(50) NULL,
                            api_raw LONGTEXT NULL,
                            reg_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                jdbcTemplate.execute("""
                        ALTER TABLE verification
                        ADD COLUMN IF NOT EXISTS user_id BIGINT NULL
                        """);

                jdbcTemplate.execute("""
                        ALTER TABLE verification
                        MODIFY COLUMN api_raw LONGTEXT NULL
                        """);

                jdbcTemplate.execute("""
                        ALTER TABLE verification
                        MODIFY COLUMN score DOUBLE NULL
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

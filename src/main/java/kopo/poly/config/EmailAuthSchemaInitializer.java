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
 * 이메일 인증번호 저장 테이블을 시작 시 준비하는 설정 클래스다.
 */
@Configuration
public class EmailAuthSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(EmailAuthSchemaInitializer.class);

    @Bean
    public ApplicationRunner emailAuthSchemaRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS email_auth (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            email VARCHAR(255) NOT NULL,
                            auth_code VARCHAR(10) NOT NULL,
                            verified CHAR(1) NOT NULL DEFAULT 'N',
                            expires_at DATETIME NOT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NULL
                        )
                        """);

                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_email_auth_email_id
                        ON email_auth(email, id)
                        """);

                log.info("Ensured email_auth schema is ready.");
            } catch (Exception e) {
                // DB 접속이 불안정해도 애플리케이션 전체 기동은 유지한다.
                log.warn("Skipping email_auth schema init because DB is unavailable.", e);
            }
        };
    }
}

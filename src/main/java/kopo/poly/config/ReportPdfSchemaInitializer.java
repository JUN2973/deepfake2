package kopo.poly.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 신고 제출용 PDF 생성 이력을 저장할 테이블을 애플리케이션 시작 시 준비한다.
 */
@Configuration
public class ReportPdfSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(ReportPdfSchemaInitializer.class);

    @Bean
    public ApplicationRunner reportPdfSchemaRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS generated_report (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            verification_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            report_reason TEXT NOT NULL,
                            source_url VARCHAR(1000) NULL,
                            report_draft TEXT NULL,
                            original_image_included BOOLEAN NOT NULL DEFAULT FALSE,
                            heatmap_included BOOLEAN NOT NULL DEFAULT FALSE,
                            ai_report_draft_included BOOLEAN NOT NULL DEFAULT FALSE,
                            original_name VARCHAR(255) NULL,
                            verdict VARCHAR(50) NULL,
                            score DOUBLE NULL,
                            file_name VARCHAR(255) NOT NULL,
                            content_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
                            status VARCHAR(20) NOT NULL DEFAULT 'READY',
                            reg_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_generated_report_user
                        ON generated_report(user_id, id)
                        """);

                jdbcTemplate.execute("""
                        CREATE INDEX IF NOT EXISTS idx_generated_report_verification
                        ON generated_report(verification_id, user_id, id)
                        """);

                log.info("Ensured generated report schema is ready.");
            } catch (Exception e) {
                log.warn("Skipping generated report schema init because DB is unavailable.", e);
            }
        };
    }
}

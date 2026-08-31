package kopo.poly.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AiAnalysisSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisSchemaInitializer.class);

    @Bean
    public ApplicationRunner aiAnalysisSchemaRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS ai_analysis_result (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            verification_id BIGINT NOT NULL,
                            request_hash CHAR(64) NOT NULL,
                            response_json LONGTEXT NOT NULL,
                            model VARCHAR(100) NULL,
                            prompt_tokens INT NULL,
                            completion_tokens INT NULL,
                            total_tokens INT NULL,
                            reg_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            upd_dt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uk_ai_analysis_request (verification_id, request_hash),
                            INDEX idx_ai_analysis_verification (verification_id)
                        )
                        """);
                log.info("Ensured AI analysis result schema is ready.");
            } catch (Exception e) {
                log.warn("Skipping AI analysis result schema init because DB is unavailable.", e);
            }
        };
    }
}

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
 * 회원과 커뮤니티 테이블이 없을 때 애플리케이션 시작 시 필요한 스키마를 준비한다.
 */
@Configuration
public class AppUserSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(AppUserSchemaInitializer.class);

    @Bean
    public ApplicationRunner appUserSchemaRunner(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS app_user (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            email VARCHAR(255) NOT NULL,
                            password_hash VARCHAR(255) NOT NULL,
                            phone_number VARCHAR(30) NULL,
                            address VARCHAR(255) NULL,
                            oauth_provider VARCHAR(30) NULL,
                            oauth_provider_id VARCHAR(150) NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NULL,
                            CONSTRAINT uk_app_user_email UNIQUE (email)
                        )
                        """);

                jdbcTemplate.execute("""
                        ALTER TABLE app_user
                        ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30) NULL
                        """);

                jdbcTemplate.execute("""
                        ALTER TABLE app_user
                        ADD COLUMN IF NOT EXISTS address VARCHAR(255) NULL
                        """);

                jdbcTemplate.execute("""
                        ALTER TABLE app_user
                        ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(30) NULL
                        """);

                jdbcTemplate.execute("""
                        ALTER TABLE app_user
                        ADD COLUMN IF NOT EXISTS oauth_provider_id VARCHAR(150) NULL
                        """);

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS community_post (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            user_id BIGINT NULL,
                            author VARCHAR(100) NOT NULL,
                            topic VARCHAR(30) NOT NULL DEFAULT 'discussion',
                            title VARCHAR(200) NOT NULL,
                            content TEXT NOT NULL,
                            views INT NOT NULL DEFAULT 0,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                jdbcTemplate.execute("""
                        ALTER TABLE community_post
                        ADD COLUMN IF NOT EXISTS topic VARCHAR(30) NOT NULL DEFAULT 'discussion'
                        """);

                jdbcTemplate.execute("""
                        UPDATE community_post
                        SET topic = 'discussion'
                        WHERE topic IS NULL OR topic = ''
                        """);

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS community_comment (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            post_id BIGINT NOT NULL,
                            user_id BIGINT NULL,
                            parent_id BIGINT NULL,
                            author VARCHAR(100) NOT NULL,
                            content TEXT NOT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_community_comment_post
                                FOREIGN KEY (post_id) REFERENCES community_post(id)
                                ON DELETE CASCADE
                        )
                        """);

                jdbcTemplate.execute("""
                        ALTER TABLE community_comment
                        ADD COLUMN IF NOT EXISTS parent_id BIGINT NULL
                        """);

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS community_comment_like (
                            comment_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (comment_id, user_id),
                            CONSTRAINT fk_community_comment_like_comment
                                FOREIGN KEY (comment_id) REFERENCES community_comment(id)
                                ON DELETE CASCADE
                        )
                        """);

                jdbcTemplate.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS uk_app_user_oauth_provider
                        ON app_user(oauth_provider, oauth_provider_id)
                        """);

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS community_post_like (
                            post_id BIGINT NOT NULL,
                            user_id BIGINT NOT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (post_id, user_id),
                            CONSTRAINT fk_community_post_like_post
                                FOREIGN KEY (post_id) REFERENCES community_post(id)
                                ON DELETE CASCADE
                        )
                        """);

                log.info("Ensured app_user, community_post, community_comment, community_comment_like and community_post_like schema is ready.");
            } catch (Exception e) {
                log.warn("Skipping app_user/community_post/community_comment schema init because DB is unavailable.", e);
            }
        };
    }
}

package kopo.poly.config;

import kopo.poly.service.impl.GoogleOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 로그인, OAuth2, 접근 권한 등 Spring Security 동작을 설정한다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // 시연 편의 설정이다. 운영 환경에서는 개인정보/쓰기 기능 URL에 인증 권한을 걸어야 한다.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .successHandler(googleOAuth2SuccessHandler)
                )
                .logout(Customizer.withDefaults());

        return http.build();
    }
}

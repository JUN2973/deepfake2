package kopo.poly.service.impl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kopo.poly.mapper.IUserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

/**
 * 구글 OAuth2 로그인 성공 후 사용자 정보를 세션과 DB에 반영한다.
 */
@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final IUserMapper userMapper;

    public GoogleOAuth2SuccessHandler(IUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        OAuth2User principal = token.getPrincipal();
        String provider = token.getAuthorizedClientRegistrationId();
        String providerId = value(principal.getAttribute("sub"));
        String email = value(principal.getAttribute("email")).trim();
        String name = firstNonBlank(principal.getAttribute("name"), email);

        if (providerId.isBlank() || email.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/login?oauthError=google");
            return;
        }

        try {
            Map<String, Object> user = userMapper.selectUserByOauth(provider, providerId);
            if (user == null) {
                // 먼저 이메일 기준으로 기존 회원을 연결하고, 없으면 소셜 로그인 회원을 새로 만든다.
                user = userMapper.selectUserByEmail(email);
                if (user != null) {
                    userMapper.updateOauthByEmail(email, name, provider, providerId);
                } else {
                    userMapper.insertSocialUser(
                            name,
                            email,
                            sha256(UUID.randomUUID().toString()),
                            null,
                            null,
                            provider,
                            providerId
                    );
                }
                user = userMapper.selectUserByEmail(email);
            }

            if (user != null) {
                // OAuth 로그인도 일반 로그인과 같은 세션 값 규칙을 사용한다.
                HttpSession session = request.getSession(true);
                session.setAttribute("USER_ID", user.get("id"));
                session.setAttribute("USER_NAME", firstNonBlank(user.get("name"), name));
                session.setAttribute("USER_EMAIL", firstNonBlank(user.get("email"), email));
            }

            response.sendRedirect(request.getContextPath() + "/");
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/login?oauthError=google");
        }
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = value(value).trim();
            if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return "";
    }

    private String sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

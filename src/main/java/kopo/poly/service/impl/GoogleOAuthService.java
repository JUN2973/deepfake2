package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import kopo.poly.dto.AuthenticatedUserDTO;
import kopo.poly.mapper.IUserMapper;
import kopo.poly.service.IGoogleOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * 구글 OAuth 외부 통신과 서비스 사용자 연동을 담당한다.
 */
@Service
public class GoogleOAuthService implements IGoogleOAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private final IUserMapper userMapper;
    private final RestClient restClient;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    public GoogleOAuthService(IUserMapper userMapper) {
        this.userMapper = userMapper;
        this.restClient = RestClient.create();
    }

    @Override
    public boolean hasGoogleConfig() {
        // 로컬 개발에서 disabled 값이 들어온 경우도 설정 없음으로 처리한다.
        return StringUtils.hasText(googleClientId)
                && StringUtils.hasText(googleClientSecret)
                && !"disabled".equalsIgnoreCase(googleClientId)
                && !"disabled".equalsIgnoreCase(googleClientSecret);
    }

    @Override
    public String createState() {
        // OAuth CSRF 방지를 위해 예측하기 어려운 state 값을 만든다.
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String buildAuthorizationUrl(String redirectUri, String state) {
        // 구글 인증 화면으로 보낼 표준 OAuth 파라미터를 구성한다.
        return UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .queryParam("prompt", "select_account")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public AuthenticatedUserDTO loginWithCode(String code, String redirectUri) {
        // 콜백으로 받은 authorization code를 access token으로 교환한다.
        JsonNode token = requestToken(code, redirectUri);
        String accessToken = token.path("access_token").asText("");
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("Google access token is empty.");
        }

        // access token으로 구글 프로필을 조회하고 서비스 사용자와 매핑한다.
        JsonNode profile = requestProfile(accessToken);
        String provider = "google";
        String providerId = profile.path("sub").asText("").trim();
        String email = profile.path("email").asText("").trim();
        String name = firstNonBlank(profile.path("name").asText(""), email);

        if (!StringUtils.hasText(providerId) || !StringUtils.hasText(email)) {
            throw new IllegalStateException("Google profile is missing required fields.");
        }

        Map<String, Object> user = findOrCreateUser(provider, providerId, email, name);
        return new AuthenticatedUserDTO(
                user.get("id"),
                firstNonBlank(user.get("name"), name),
                firstNonBlank(user.get("email"), email)
        );
    }

    private JsonNode requestToken(String code, String redirectUri) {
        // 구글 토큰 엔드포인트는 x-www-form-urlencoded 본문을 요구한다.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        return restClient.post()
                .uri(GOOGLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
    }

    private JsonNode requestProfile(String accessToken) {
        // userinfo 엔드포인트에서 sub, email, name 등 로그인에 필요한 값을 가져온다.
        return restClient.get()
                .uri(GOOGLE_USERINFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);
    }

    private Map<String, Object> findOrCreateUser(String provider, String providerId, String email, String name) {
        // 이미 연결된 소셜 계정이면 그대로 로그인한다.
        Map<String, Object> user = userMapper.selectUserByOauth(provider, providerId);
        if (user != null) {
            return user;
        }

        // 동일 이메일의 일반 계정이 있으면 소셜 계정 정보를 연결한다.
        user = userMapper.selectUserByEmail(email);
        if (user != null) {
            userMapper.updateOauthByEmail(email, name, provider, providerId);
            return userMapper.selectUserByEmail(email);
        }

        // 처음 방문한 구글 사용자면 비밀번호 없는 소셜 계정으로 신규 가입시킨다.
        userMapper.insertSocialUser(
                name,
                email,
                "",
                null,
                null,
                provider,
                providerId
        );
        return userMapper.selectUserByEmail(email);
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return "";
    }
}

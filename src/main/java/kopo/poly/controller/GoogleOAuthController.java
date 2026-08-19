package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.AuthenticatedUserDTO;
import kopo.poly.service.IGoogleOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;

/**
 * 구글 OAuth 로그인 시작 요청과 콜백 요청을 처리하는 페이지 컨트롤러다.
 */
@Controller
public class GoogleOAuthController {

    // 구글 OAuth 처리에 필요한 엔드포인트와 CSRF 방지용 state 세션 키를 상수로 관리한다.
    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthController.class);
    private static final String GOOGLE_STATE_SESSION_KEY = "GOOGLE_OAUTH_STATE";

    private final IGoogleOAuthService googleOAuthService;

    public GoogleOAuthController(IGoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    // 로그인 페이지에서 구글 로그인 버튼을 눌렀을 때 최초로 진입하는 요청이다.
    // state 값을 세션에 저장한 뒤 구글 인증 화면으로 리다이렉트한다.
    @GetMapping("/oauth2/authorization/google")
    public void authorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!googleOAuthService.hasGoogleConfig()) {
            response.sendRedirect(request.getContextPath() + "/login?oauthError=google");
            return;
        }

        // 콜백 요청이 실제 이 서버에서 시작된 인증 흐름인지 검증하기 위한 난수 값이다.
        String state = googleOAuthService.createState();
        request.getSession(true).setAttribute(GOOGLE_STATE_SESSION_KEY, state);

        // 구글 OAuth 인증 URL에 필요한 파라미터를 붙여 사용자 브라우저를 이동시킨다.
        String redirectUrl = googleOAuthService.buildAuthorizationUrl(buildRedirectUri(request), state);

        response.sendRedirect(redirectUrl);
    }

    // 구글 인증이 끝난 뒤 redirect_uri로 돌아오는 콜백 요청을 처리한다.
    // 인증 코드로 액세스 토큰을 받고, 사용자 프로필을 조회해 서비스 로그인 세션을 만든다.
    @GetMapping("/login/oauth2/code/google")
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        // state는 한 번만 사용해야 하므로 읽은 뒤 즉시 세션에서 제거한다.
        HttpSession session = request.getSession(false);
        String expectedState = session == null ? null : (String) session.getAttribute(GOOGLE_STATE_SESSION_KEY);
        if (session != null) {
            session.removeAttribute(GOOGLE_STATE_SESSION_KEY);
        }

        // 오류 파라미터, 누락된 인증 코드, state 불일치, 설정 누락은 모두 로그인 실패로 처리한다.
        if (StringUtils.hasText(error) || !StringUtils.hasText(code) || !StringUtils.hasText(state)
                || expectedState == null || !expectedState.equals(state) || !googleOAuthService.hasGoogleConfig()) {
            response.sendRedirect(request.getContextPath() + "/login?oauthError=google");
            return;
        }

        try {
            // 기존 소셜 계정, 동일 이메일 계정, 신규 소셜 계정 순서로 사용자를 확보한다.
            AuthenticatedUserDTO user = googleOAuthService.loginWithCode(code, buildRedirectUri(request));
            // 로그인 직전 세션 ID를 교체해 세션 고정 공격 위험을 낮춘다.
            request.changeSessionId();
            HttpSession loginSession = request.getSession(true);
            loginSession.setAttribute("USER_ID", user.getId());
            loginSession.setAttribute("USER_NAME", user.getName());
            loginSession.setAttribute("USER_EMAIL", user.getEmail());

            response.sendRedirect(request.getContextPath() + "/?login=google");
        } catch (RestClientResponseException e) {
            log.warn("Google OAuth HTTP error. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            response.sendRedirect(request.getContextPath() + "/login?oauthError=google");
        } catch (Exception e) {
            log.error("Google OAuth login failed", e);
            response.sendRedirect(request.getContextPath() + "/login?oauthError=google");
        }
    }

    // 구글 콘솔에 등록한 redirect_uri와 동일한 콜백 주소를 현재 요청 기준으로 생성한다.
    private String buildRedirectUri(HttpServletRequest request) {
        return buildBaseUrl(request) + request.getContextPath() + "/login/oauth2/code/google";
    }

    // 프록시 뒤에서 동작할 때는 X-Forwarded-* 헤더를 우선 사용해 외부 접속 기준 URL을 만든다.
    private String buildBaseUrl(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (StringUtils.hasText(forwardedProto) && StringUtils.hasText(forwardedHost)) {
            return forwardedProto + "://" + forwardedHost;
        }

        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }

}

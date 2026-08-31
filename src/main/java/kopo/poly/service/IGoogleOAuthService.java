package kopo.poly.service;

import kopo.poly.dto.AuthenticatedUserDTO;

/**
 * 구글 OAuth 인증 URL 생성과 콜백 로그인을 처리하는 서비스 계약이다.
 */
public interface IGoogleOAuthService {
    boolean hasGoogleConfig();

    String createState();

    String buildAuthorizationUrl(String redirectUri, String state);

    AuthenticatedUserDTO loginWithCode(String code, String redirectUri);
}


package kopo.poly.service;

/**
 * 인증 서비스에서 실패 코드와 메시지를 컨트롤러로 전달하기 위한 예외다.
 */
public class AuthServiceException extends RuntimeException {
    private final String code;

    public AuthServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

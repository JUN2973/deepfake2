package kopo.poly.service;

/**
 * 이메일 안내/인증 관련 서비스 계약을 정의한다.
 */
public interface IEmailVerificationService {
    void sendCode(String email) throws Exception;

    boolean verifyCode(String email, String code) throws Exception;

    boolean isVerified(String email);

    void sendSignupCompletedMail(String email, String name) throws Exception;
}

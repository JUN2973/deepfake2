package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: 구현(인증/회원): 로그인, 회원가입, 이메일 인증, 계정 찾기 요청/응답 데이터를 전달한다.
 */
/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class EmailAuthResponseDTO {

    private boolean success;
    private String message;
    private String email;
    private String verified;

    public EmailAuthResponseDTO() {
    }

    public EmailAuthResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerified() {
        return verified;
    }

    public void setVerified(String verified) {
        this.verified = verified;
    }
}

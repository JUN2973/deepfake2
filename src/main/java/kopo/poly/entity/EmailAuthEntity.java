package kopo.poly.entity;


/**
 * 체크리스트 기준 주석: 테이블 명세서(RDBMS): 이메일 인증 테이블과 매핑되는 JPA Entity를 정의한다.
 */
/**
 * 이메일 인증번호 테이블 한 행을 표현하는 엔티티다.
 */
public class EmailAuthEntity {

    private Long id;
    private String email;
    private String authCode;
    private String verified;
    private String expiresAt;
    private String createdAt;
    private String updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getVerified() {
        return verified;
    }

    public void setVerified(String verified) {
        this.verified = verified;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}

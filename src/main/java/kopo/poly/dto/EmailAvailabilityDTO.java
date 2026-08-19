package kopo.poly.dto;

/**
 * 회원가입 이메일 중복 확인 결과를 담는다.
 */
public class EmailAvailabilityDTO {
    private final boolean available;
    private final String email;

    public EmailAvailabilityDTO(boolean available, String email) {
        this.available = available;
        this.email = email;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getEmail() {
        return email;
    }
}

package kopo.poly.dto;

/**
 * 로그인 상태에서 비밀번호를 변경할 때 사용하는 요청 DTO다.
 */
public class PasswordChangeRequestDTO {
    private String currentPassword;
    private String newPassword;
    private String password;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

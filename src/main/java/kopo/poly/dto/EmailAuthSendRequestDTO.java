package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: 구현(인증/회원): 로그인, 회원가입, 이메일 인증, 계정 찾기 요청/응답 데이터를 전달한다.
 */
/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class EmailAuthSendRequestDTO {

    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

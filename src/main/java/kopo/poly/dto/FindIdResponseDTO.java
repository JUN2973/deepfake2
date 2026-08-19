package kopo.poly.dto;

/**
 * 아이디 찾기 결과로 화면에 반환할 사용자 식별 정보를 담는다.
 */
public class FindIdResponseDTO {
    private final Object email;
    private final Object name;

    public FindIdResponseDTO(Object email, Object name) {
        this.email = email;
        this.name = name;
    }

    public Object getEmail() {
        return email;
    }

    public Object getName() {
        return name;
    }
}

package kopo.poly.dto;

/**
 * 로그인 성공 후 세션에 저장할 최소 사용자 정보를 담는 DTO다.
 */
public class AuthenticatedUserDTO {
    private final Object id;
    private final Object name;
    private final Object email;

    public AuthenticatedUserDTO(Object id, Object name, Object email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Object getId() {
        return id;
    }

    public Object getName() {
        return name;
    }

    public Object getEmail() {
        return email;
    }
}

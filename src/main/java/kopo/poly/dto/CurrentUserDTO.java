package kopo.poly.dto;

/**
 * 현재 로그인 사용자의 마이페이지 표시 정보를 담는다.
 */
public class CurrentUserDTO {
    private final Object id;
    private final Object name;
    private final Object email;
    private final String phone;
    private final String address;
    private final String oauthProvider;
    private final boolean socialLogin;

    public CurrentUserDTO(Object id, Object name, Object email, String phone, String address, String oauthProvider) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.oauthProvider = oauthProvider;
        this.socialLogin = oauthProvider != null && !oauthProvider.isBlank();
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

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public boolean isSocialLogin() {
        return socialLogin;
    }
}

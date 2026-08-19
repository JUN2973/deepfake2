package kopo.poly.dto;

/**
 * 프로필 수정 후 갱신된 사용자 정보를 담는다.
 */
public class ProfileResponseDTO {
    private final String name;
    private final String phone;
    private final String address;

    public ProfileResponseDTO(String name, String phone, String address) {
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }
}

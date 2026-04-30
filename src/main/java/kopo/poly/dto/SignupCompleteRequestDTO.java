package kopo.poly.dto;

/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class SignupCompleteRequestDTO {
    private String name;
    private String email;
    private String password;
    private String phoneNumber;
    private String address;
    private String detailAddress;
    private String zonecode;

    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }
    public String getPassword(){ return password; }
    public void setPassword(String password){ this.password = password; }
    public String getPhoneNumber(){ return phoneNumber; }
    public void setPhoneNumber(String phoneNumber){ this.phoneNumber = phoneNumber; }
    public String getAddress(){ return address; }
    public void setAddress(String address){ this.address = address; }
    public String getDetailAddress(){ return detailAddress; }
    public void setDetailAddress(String detailAddress){ this.detailAddress = detailAddress; }
    public String getZonecode(){ return zonecode; }
    public void setZonecode(String zonecode){ this.zonecode = zonecode; }
}

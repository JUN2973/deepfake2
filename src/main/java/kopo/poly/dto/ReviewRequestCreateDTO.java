package kopo.poly.dto;

/**
 * 사용자가 기존 검증 결과의 오탐 신고 또는 재검토를 요청할 때 사용하는 DTO다.
 * 검증 번호와 사용자 번호는 요청 본문이 아닌 URL 및 로그인 세션에서 결정한다.
 */
public class ReviewRequestCreateDTO {

    public static final String TYPE_FALSE_POSITIVE = "FALSE_POSITIVE";
    public static final String TYPE_FALSE_NEGATIVE = "FALSE_NEGATIVE";
    public static final String TYPE_RECHECK = "RECHECK";

    private String requestType;
    private String reason;

    public ReviewRequestCreateDTO() {
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

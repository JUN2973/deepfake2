package kopo.poly.service;

/**
 * 재검토 요청 처리 중 발생한 검증 및 저장 오류를 API 오류 코드와 함께 전달한다.
 */
public class ReviewRequestServiceException extends RuntimeException {

    private final String code;

    public ReviewRequestServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

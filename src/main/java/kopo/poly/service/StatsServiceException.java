package kopo.poly.service;

/**
 * 검증 통계 조회 조건이 올바르지 않을 때 오류 코드와 메시지를 전달한다.
 */
public class StatsServiceException extends RuntimeException {

    private final String code;

    public StatsServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

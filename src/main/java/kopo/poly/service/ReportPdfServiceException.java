package kopo.poly.service;

/**
 * 신고 제출용 PDF 생성 및 조회 과정에서 발생한 사용자 처리 가능 오류다.
 */
public class ReportPdfServiceException extends RuntimeException {

    private final String code;

    public ReportPdfServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ReportPdfServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

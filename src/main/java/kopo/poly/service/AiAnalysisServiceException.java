package kopo.poly.service;

public class AiAnalysisServiceException extends RuntimeException {

    private final String code;

    public AiAnalysisServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AiAnalysisServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

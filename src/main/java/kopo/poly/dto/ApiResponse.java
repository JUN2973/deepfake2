package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: API 연동 설계: 비동기 API의 공통 응답 형식을 정의한다.
 */
/**
 * API 응답 성공/실패 형식을 통일하기 위한 공통 응답 DTO다.
 */
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ApiError error;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.error = new ApiError(code, message);
        return r;
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public ApiError getError() { return error; }

    public static class ApiError {
        private String code;
        private String message;
        public ApiError() {}
        public ApiError(String code, String message) {
            this.code = code; this.message = message;
        }
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
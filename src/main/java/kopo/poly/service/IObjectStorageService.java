package kopo.poly.service;


/**
 * 체크리스트 기준 주석: 설계/API 연동 설계: 컨트롤러와 구현체 사이의 서비스 계약을 정의한다.
 */
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 파일 저장소 구현체가 따라야 하는 공통 인터페이스다.
 */
public interface IObjectStorageService {
    UploadResult uploadPublic(MultipartFile file, String objectKey) throws Exception;

    class UploadResult {
        private final String objectKey;
        private final String publicUrl;

        public UploadResult(String objectKey, String publicUrl) {
            this.objectKey = objectKey;
            this.publicUrl = publicUrl;
        }
        public String getObjectKey() { return objectKey; }
        public String getPublicUrl() { return publicUrl; }
    }
}
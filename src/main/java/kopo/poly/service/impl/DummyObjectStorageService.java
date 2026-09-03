package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(이미지 업로드/검증기록): 업로드 파일 저장소와 공개 URL 생성을 담당한다.
 */
import kopo.poly.service.IObjectStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 외부 스토리지 없이 로컬 uploads 폴더에 파일을 저장하는 구현체다.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class DummyObjectStorageService implements IObjectStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public UploadResult uploadPublic(MultipartFile file, String objectKey) throws Exception {
        Path targetPath = resolveSafePath(objectKey);

        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        file.transferTo(targetPath);

        // WebConfig에서 /uploads/** 경로를 이 로컬 폴더에 매핑한다.
        String url = "/uploads/" + objectKey.replace("\\", "/");
        return new UploadResult(objectKey, url);
    }

    @Override
    public byte[] readObject(String objectKey) throws Exception {
        return Files.readAllBytes(resolveSafePath(objectKey));
    }

    private Path resolveSafePath(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key is required.");
        }
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(objectKey).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new IllegalArgumentException("Invalid upload path.");
        }
        return targetPath;
    }
}

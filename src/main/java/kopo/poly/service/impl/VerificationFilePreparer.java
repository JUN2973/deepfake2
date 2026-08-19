package kopo.poly.service.impl;

import kopo.poly.service.IObjectStorageService;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 업로드된 이미지 파일을 검증하고 분석용 임시 파일과 공개 저장소 객체로 준비한다.
 */
final class VerificationFilePreparer {

    private final IObjectStorageService objectStorageService;

    VerificationFilePreparer(IObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    PreparedUpload prepare(MultipartFile file) throws Exception {
        validateImageOnly(file);

        String originalName = file.getOriginalFilename();
        String safeName = sanitizeFileName(originalName);
        String objectKey = buildObjectKey(safeName);
        IObjectStorageService.UploadResult uploadResult = objectStorageService.uploadPublic(file, objectKey);

        Path tempFilePath = Files.createTempFile("df_", "_" + safeName);
        try {
            file.transferTo(tempFilePath.toFile());
        } catch (Exception e) {
            Files.deleteIfExists(tempFilePath);
            throw e;
        }

        return new PreparedUpload(originalName, uploadResult, tempFilePath);
    }

    private void validateImageOnly(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 선택해 주세요.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";

        boolean supportedExtension = extension.equals("png")
                || extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("bmp")
                || extension.equals("webp")
                || extension.equals("heic")
                || extension.equals("heif");
        if (!supportedExtension) {
            throw new IllegalArgumentException("지원하지 않는 이미지 확장자입니다.");
        }

        long maxSize = 32L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("이미지 파일은 32MB 이하만 업로드할 수 있습니다.");
        }
    }

    private String sanitizeFileName(String originalName) {
        return originalName == null ? "image" : originalName.replaceAll("[\\\\/]", "_");
    }

    private String buildObjectKey(String safeName) {
        LocalDate now = LocalDate.now();
        return String.format("%04d/%02d/%02d/%s_%s",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                safeName);
    }

    record PreparedUpload(String originalName,
                          IObjectStorageService.UploadResult uploadResult,
                          Path tempFilePath) implements AutoCloseable {
        @Override
        public void close() {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (Exception ignored) {
            }
        }
    }
}

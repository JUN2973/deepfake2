package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(이미지 업로드/검증기록): 업로드 파일 저장소와 공개 URL 생성을 담당한다.
 */
import kopo.poly.service.IObjectStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * S3에 업로드 파일을 저장하고 브라우저에서 접근 가능한 공개 URL을 생성한다.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3ObjectStorageService implements IObjectStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;
    private final String publicBaseUrl;
    private final boolean publicReadAcl;

    public S3ObjectStorageService(@Value("${app.s3.region}") String region,
                                  @Value("${app.s3.bucket}") String bucket,
                                  @Value("${app.s3.access-key:}") String accessKey,
                                  @Value("${app.s3.secret-key:}") String secretKey,
                                  @Value("${app.s3.use-default-credentials:false}") boolean useDefaultCredentials,
                                  @Value("${app.s3.public-base-url:}") String publicBaseUrl,
                                  @Value("${app.s3.public-read-acl:false}") boolean publicReadAcl) {
        // S3 저장소를 선택한 경우 필수 설정은 애플리케이션 시작 시점에 바로 검증한다.
        this.region = requireText(region, "app.s3.region");
        this.bucket = requireText(bucket, "app.s3.bucket");
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
        this.publicReadAcl = publicReadAcl;

        var builder = S3Client.builder().region(Region.of(this.region));
        if (hasText(accessKey) && hasText(secretKey)) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            ));
            this.s3Client = builder.build();
        } else if (useDefaultCredentials) {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
            this.s3Client = builder.build();
        } else {
            throw new IllegalStateException("app.storage.type=s3 requires AWS credentials. "
                    + "Set AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY or AWS_USE_DEFAULT_CREDENTIALS=true.");
        }
    }

    @Override
    public UploadResult uploadPublic(MultipartFile file, String objectKey) throws Exception {
        // objectKey를 정규화해 상위 경로 접근 같은 잘못된 키를 차단한다.
        String safeObjectKey = normalizeObjectKey(objectKey);

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(safeObjectKey)
                .contentLength(file.getSize());

        if (hasText(file.getContentType())) {
            requestBuilder.contentType(file.getContentType());
        }

        if (publicReadAcl) {
            requestBuilder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(inputStream, file.getSize()));
        }

        return new UploadResult(safeObjectKey, buildPublicUrl(safeObjectKey));
    }

    private String buildPublicUrl(String objectKey) {
        // S3 URL 경로 세그먼트마다 인코딩해 한글 파일명도 안전하게 노출한다.
        String encodedKey = Arrays.stream(objectKey.split("/"))
                .map(S3ObjectStorageService::urlEncodePathSegment)
                .collect(Collectors.joining("/"));

        if (hasText(publicBaseUrl)) {
            return publicBaseUrl + "/" + encodedKey;
        }

        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + encodedKey;
    }

    private static String normalizeObjectKey(String objectKey) {
        // S3 key는 상대 경로만 허용하고, 디렉터리 탈출 패턴은 거부한다.
        if (!hasText(objectKey)) {
            throw new IllegalArgumentException("S3 object key is required.");
        }

        String normalized = objectKey.replace("\\", "/");
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.contains("..\\")) {
            throw new IllegalArgumentException("Invalid S3 object key.");
        }
        return normalized;
    }

    private static String urlEncodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String requireText(String value, String propertyName) {
        if (!hasText(value)) {
            throw new IllegalStateException(propertyName + " is required when app.storage.type=s3.");
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String trimTrailingSlash(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.trim().replaceAll("/+$", "");
    }
}

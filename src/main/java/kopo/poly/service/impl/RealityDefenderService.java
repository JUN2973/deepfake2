package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별): Reality Defender 분석 흐름을 서비스 계층에서 중계한다.
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.DetectionResultDTO;
import kopo.poly.dto.SuspiciousRegionDTO;
import kopo.poly.dto.VerificationRecordDTO;
import kopo.poly.mapper.IRealityDefenderMapper;
import kopo.poly.service.IRealityDefenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Reality Defender 분석 요청, 결과 보강, DB 저장, 이력 조회를 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealityDefenderService implements IRealityDefenderService {

    // 외부 분석 API로 보내기 전 서비스 계층에서 한 번 더 제한하는 최대 파일 크기다.
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final IRealityDefenderMapper realityDefenderMapper;

    @Value("${reality.defender.base-url:}")
    private String baseUrl;

    @Value("${reality.defender.api-key:}")
    private String apiKey;

    @Value("${reality.defender.mock-mode:true}")
    private boolean mockMode;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public DetectionResultDTO analyzeImage(MultipartFile file, Long userId) throws Exception {
        // 업로드 검증 -> 파일 저장 -> 외부 API 분석 -> DB/스냅샷 저장까지 이어지는 분석 메인 흐름이다.
        validateFile(file);

        SavedImage savedImage = saveImage(file);
        DetectionResultDTO result;

        try {
            result = callRealityDefender(file, savedImage);
        } catch (Exception e) {
            log.error("Reality Defender API call failed. Falling back to mock response.", e);
            if (!isMockAllowed()) {
                throw new IllegalStateException("외부 분석 서버를 사용할 수 없습니다.");
            }
            // 시연이나 개발 환경에서는 외부 API 장애가 있어도 화면 흐름을 확인할 수 있도록 mock 결과를 만든다.
            result = buildMockResult(savedImage, "외부 분석 응답을 사용할 수 없어 임시 분석 결과를 사용했습니다.");
        }

        result.setUserId(userId);
        result.setOriginalFilename(file.getOriginalFilename());
        result.setSavedPath(savedImage.absolutePath.toString());
        result.setImageUrl(savedImage.relativeUrl);
        result.setImageWidth(savedImage.width);
        result.setImageHeight(savedImage.height);

        enrichResult(result, savedImage);
        persistResult(result);
        // 상세 화면에서 원본 API 응답 구조를 다시 계산하지 않도록 분석 결과 전체를 JSON 파일로 보관한다.
        writeSnapshot(result);

        return result;
    }

    @Override
    public DetectionResultDTO getDetectionResult(Long id) throws Exception {
        VerificationRecordDTO record = realityDefenderMapper.selectVerificationRecordById(id);
        if (record == null) {
            return null;
        }

        // DB에는 요약값을 저장하고, 자세한 탐지 영역 정보는 업로드 파일 옆의 스냅샷 JSON에서 복원한다.
        Path snapshotPath = Paths.get(record.getSavedPath() + ".analysis.json");
        DetectionResultDTO result;
        if (Files.exists(snapshotPath)) {
            result = objectMapper.readValue(snapshotPath.toFile(), DetectionResultDTO.class);
        } else {
            result = new DetectionResultDTO();
            result.setId(record.getId());
            result.setUserId(record.getUserId());
            result.setOriginalFilename(record.getOriginalFilename());
            result.setSavedPath(record.getSavedPath());
            result.setImageUrl(toImageUrl(record.getSavedPath()));
            result.setStatus(record.getDetectStatus());
            result.setFinalScore(record.getFinalScore());
            result.setExplanationText(record.getExplanationText());
            result.setDetailedReason(record.getDetailedReason());
            result.setNotApplicableReason(record.getNotApplicableReason());
            result.setOverlaySource(record.getOverlaySource());
            result.setCreatedAt(record.getCreatedAt());
            addFallbackRegionsIfPossible(result);
        }

        if (!StringUtils.hasText(result.getImageUrl()) && StringUtils.hasText(result.getSavedPath())) {
            result.setImageUrl(toImageUrl(result.getSavedPath()));
        }
        return result;
    }

    @Override
    public List<DetectionResultDTO> getDetectionHistory(Long userId) throws Exception {
        List<VerificationRecordDTO> rows = realityDefenderMapper.selectVerificationRecordList(userId);
        List<DetectionResultDTO> results = new ArrayList<>();

        for (VerificationRecordDTO row : rows) {
            DetectionResultDTO dto = getDetectionResult(row.getId());
            if (dto != null) {
                results.add(dto);
            }
        }

        return results;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 선택해 주세요.");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("이미지 크기는 10MB 이하여야 합니다.");
        }
    }

    private SavedImage saveImage(MultipartFile file) throws Exception {
        // detect/yyyy/MM/dd 구조로 저장해 업로드 파일이 한 폴더에 몰리지 않게 한다.
        String safeName = sanitizeFilename(file.getOriginalFilename());
        LocalDate today = LocalDate.now();
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = basePath.resolve("detect")
                .resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(String.format("%02d", today.getDayOfMonth()));
        Files.createDirectories(targetDir);

        String filename = UUID.randomUUID().toString().replace("-", "") + "_" + safeName;
        Path targetPath = targetDir.resolve(filename);
        file.transferTo(targetPath);

        BufferedImage bufferedImage = readImage(targetPath);
        if (bufferedImage == null) {
            // 확장자만 이미지인 잘못된 파일은 저장 직후 삭제하고 분석을 중단한다.
            Files.deleteIfExists(targetPath);
            throw new IllegalArgumentException("업로드한 파일을 이미지로 읽을 수 없습니다.");
        }

        SavedImage savedImage = new SavedImage();
        savedImage.absolutePath = targetPath;
        savedImage.width = bufferedImage.getWidth();
        savedImage.height = bufferedImage.getHeight();
        savedImage.relativeUrl = toImageUrl(targetPath.toString());
        return savedImage;
    }

    private DetectionResultDTO callRealityDefender(MultipartFile file, SavedImage savedImage) throws Exception {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey)) {
            if (isMockAllowed()) {
                return buildMockResult(savedImage, "Mock fallback was used because the API key is not configured.");
            }
            throw new IllegalStateException("Reality Defender 설정이 누락되었습니다.");
        }

        // Reality Defender의 파일 스캔 API는 multipart/form-data로 원본 이미지를 전송한다.
        String requestUrl = trimTrailingSlash(baseUrl) + "/api/files/scan";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-API-KEY", apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return sanitizeFilename(file.getOriginalFilename());
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        ResponseEntity<String> response;
        try {
            response = restClient.post()
                    .uri(requestUrl)
                    .headers(requestHeaders -> requestHeaders.addAll(headers))
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Reality Defender 요청에 실패했습니다.", e);
        }

        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
            throw new IllegalStateException("Reality Defender 응답이 비어 있습니다.");
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        return convertApiResponse(root, savedImage);
    }

    private DetectionResultDTO convertApiResponse(JsonNode root, SavedImage savedImage) {
        DetectionResultDTO result = new DetectionResultDTO();
        result.setMockMode(false);

        // API 응답 버전별로 필드 위치가 달라질 수 있어 여러 JSON Pointer 후보를 순서대로 읽는다.
        String status = firstText(root,
                "/resultsSummary/status",
                "/status",
                "/result/status",
                "/data/status");
        String reason = firstText(root,
                "/resultsSummary/reason",
                "/resultsSummary/metadata/reason",
                "/reason",
                "/message",
                "/data/reason");

        Integer score = normalizeScore(firstNumber(root,
                "/resultsSummary/metadata/finalScore",
                "/resultsSummary/finalScore",
                "/finalScore",
                "/score",
                "/data/finalScore"));

        result.setStatus(normalizeStatus(status, score, reason, savedImage));
        result.setFinalScore(score);
        result.setNotApplicableReason("NOT_APPLICABLE".equals(result.getStatus())
                ? defaultNotApplicableReason(reason)
                : null);

        List<SuspiciousRegionDTO> suspiciousRegions = extractSuspiciousRegions(root, savedImage.width, savedImage.height);
        if (suspiciousRegions.isEmpty() && !"NOT_APPLICABLE".equals(result.getStatus())) {
            // 외부 API가 점수만 주고 영역 좌표를 주지 않는 경우 결과 화면 표시용 임시 영역을 만든다.
            suspiciousRegions = buildMockRegions(savedImage.width, savedImage.height);
            result.setOverlaySource("MOCK");
        } else if (suspiciousRegions.isEmpty()) {
            result.setOverlaySource("NONE");
        } else {
            result.setOverlaySource("REALITY_DEFENDER");
        }

        result.setSuspiciousRegions(suspiciousRegions);
        result.setExplanationText(buildExplanationText(result.getStatus(), result.getFinalScore(), result.getNotApplicableReason()));
        result.setDetailedReason(buildDetailedReason(result.getStatus(), result.getFinalScore(), suspiciousRegions, result.getNotApplicableReason()));
        return result;
    }

    private DetectionResultDTO buildMockResult(SavedImage savedImage, String reason) {
        DetectionResultDTO result = new DetectionResultDTO();
        result.setMockMode(true);

        if (savedImage.width < 220 || savedImage.height < 220) {
            result.setStatus("NOT_APPLICABLE");
            result.setFinalScore(null);
            result.setNotApplicableReason("Face area is missing or too small for reliable analysis.");
            result.setOverlaySource("NONE");
            result.setSuspiciousRegions(new ArrayList<>());
        } else {
            result.setStatus("SUSPICIOUS");
            result.setFinalScore(68);
            result.setOverlaySource("MOCK");
            result.setSuspiciousRegions(buildMockRegions(savedImage.width, savedImage.height));
        }

        result.setExplanationText(buildExplanationText(result.getStatus(), result.getFinalScore(), result.getNotApplicableReason()));
        result.setDetailedReason(buildDetailedReason(result.getStatus(), result.getFinalScore(), result.getSuspiciousRegions(), reason));
        return result;
    }

    private void enrichResult(DetectionResultDTO result, SavedImage savedImage) {
        // 외부 응답 또는 mock 응답에서 빠진 값이 있어도 JSP가 null 처리에 실패하지 않도록 기본값을 채운다.
        if (!StringUtils.hasText(result.getStatus())) {
            result.setStatus("NOT_APPLICABLE");
        }

        if ("NOT_APPLICABLE".equals(result.getStatus())) {
            result.setFinalScore(null);
            if (!StringUtils.hasText(result.getNotApplicableReason())) {
                result.setNotApplicableReason("Face area is missing or too small for reliable analysis.");
            }
            result.setOverlaySource("NONE");
            result.setSuspiciousRegions(new ArrayList<>());
        }

        if (result.getSuspiciousRegions() == null) {
            result.setSuspiciousRegions(new ArrayList<>());
        }

        if (!StringUtils.hasText(result.getExplanationText())) {
            result.setExplanationText(buildExplanationText(result.getStatus(), result.getFinalScore(), result.getNotApplicableReason()));
        }

        if (!StringUtils.hasText(result.getDetailedReason())) {
            result.setDetailedReason(buildDetailedReason(result.getStatus(), result.getFinalScore(), result.getSuspiciousRegions(), result.getNotApplicableReason()));
        }

        if (!StringUtils.hasText(result.getOverlaySource())) {
            result.setOverlaySource(result.getSuspiciousRegions().isEmpty() ? "NONE" : "MOCK");
        }

        if (savedImage.width < 220 || savedImage.height < 220) {
            result.setStatus("NOT_APPLICABLE");
            result.setFinalScore(null);
            result.setNotApplicableReason("Face area is missing or too small for reliable analysis.");
            result.setOverlaySource("NONE");
            result.setSuspiciousRegions(new ArrayList<>());
            result.setExplanationText(buildExplanationText(result.getStatus(), result.getFinalScore(), result.getNotApplicableReason()));
            result.setDetailedReason(buildDetailedReason(result.getStatus(), null, result.getSuspiciousRegions(), result.getNotApplicableReason()));
        }
    }

    private void persistResult(DetectionResultDTO result) {
        // 목록/이력 조회에 필요한 요약값은 relational DB에 저장한다.
        VerificationRecordDTO record = new VerificationRecordDTO();
        record.setUserId(result.getUserId());
        record.setOriginalFilename(result.getOriginalFilename());
        record.setSavedPath(result.getSavedPath());
        record.setDetectStatus(result.getStatus());
        record.setFinalScore(result.getFinalScore());
        record.setExplanationText(result.getExplanationText());
        record.setDetailedReason(result.getDetailedReason());
        record.setNotApplicableReason(result.getNotApplicableReason());
        record.setOverlaySource(result.getOverlaySource());

        realityDefenderMapper.insertVerificationRecord(record);
        result.setId(record.getId());

        VerificationRecordDTO savedRecord = realityDefenderMapper.selectVerificationRecordById(record.getId());
        if (savedRecord != null) {
            result.setCreatedAt(savedRecord.getCreatedAt());
        }
    }

    private void writeSnapshot(DetectionResultDTO result) throws IOException {
        if (!StringUtils.hasText(result.getSavedPath())) {
            return;
        }
        Path snapshotPath = Paths.get(result.getSavedPath() + ".analysis.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(snapshotPath.toFile(), result);
    }

    private void addFallbackRegionsIfPossible(DetectionResultDTO result) throws IOException {
        if (result.getSuspiciousRegions() != null && !result.getSuspiciousRegions().isEmpty()) {
            return;
        }
        if (!StringUtils.hasText(result.getSavedPath())) {
            return;
        }

        BufferedImage image = readImage(Paths.get(result.getSavedPath()));
        if (image == null) {
            return;
        }

        result.setImageWidth(image.getWidth());
        result.setImageHeight(image.getHeight());
        if (!"NOT_APPLICABLE".equals(result.getStatus())) {
            result.setSuspiciousRegions(buildMockRegions(image.getWidth(), image.getHeight()));
            result.setOverlaySource("MOCK");
        }
    }

    private List<SuspiciousRegionDTO> extractSuspiciousRegions(JsonNode root, int imageWidth, int imageHeight) {
        // suspiciousRegions, heatmap 등 실제/더미 응답에서 쓰일 수 있는 여러 좌표 배열 이름을 지원한다.
        String[] candidatePointers = {
                "/suspiciousRegions",
                "/resultsSummary/suspiciousRegions",
                "/resultsSummary/metadata/suspiciousRegions",
                "/results/suspiciousRegions",
                "/data/suspiciousRegions",
                "/heatmap",
                "/resultsSummary/heatmap",
                "/resultsSummary/metadata/heatmap",
                "/data/heatmap"
        };

        for (String pointer : candidatePointers) {
            JsonNode arrayNode = root.at(pointer);
            if (!arrayNode.isArray()) {
                continue;
            }

            List<SuspiciousRegionDTO> regions = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                SuspiciousRegionDTO region = toRegion(node, imageWidth, imageHeight);
                if (region != null) {
                    regions.add(region);
                }
            }

            if (!regions.isEmpty()) {
                return regions;
            }
        }

        return new ArrayList<>();
    }

    private SuspiciousRegionDTO toRegion(JsonNode node, int imageWidth, int imageHeight) {
        double x = firstNumber(node, "/x", "/left", "/bbox/x", "/box/x");
        double y = firstNumber(node, "/y", "/top", "/bbox/y", "/box/y");
        double width = firstNumber(node, "/width", "/w", "/bbox/width", "/box/width");
        double height = firstNumber(node, "/height", "/h", "/bbox/height", "/box/height");

        if (width <= 0 || height <= 0) {
            return null;
        }

        if (x <= 1 && y <= 1 && width <= 1 && height <= 1) {
            // 0~1 비율 좌표로 온 경우 실제 이미지 픽셀 좌표로 변환한다.
            x *= imageWidth;
            y *= imageHeight;
            width *= imageWidth;
            height *= imageHeight;
        }

        SuspiciousRegionDTO region = new SuspiciousRegionDTO();
        region.setX(round(x));
        region.setY(round(y));
        region.setWidth(round(width));
        region.setHeight(round(height));
        region.setLabel(firstText(node, "/label", "/name", "/region", "/part", "/title"));

        double confidence = firstNumber(node, "/confidence", "/score", "/intensity", "/weight");
        region.setConfidence(confidence > 1 ? confidence : round(confidence * 100.0));
        if (!StringUtils.hasText(region.getLabel())) {
            region.setLabel("Suspicious signal");
        }
        return region;
    }

    private List<SuspiciousRegionDTO> buildMockRegions(int imageWidth, int imageHeight) {
        List<SuspiciousRegionDTO> regions = new ArrayList<>();
        regions.add(region(imageWidth * 0.27, imageHeight * 0.28, imageWidth * 0.15, imageHeight * 0.11, "Left eye area", 84));
        regions.add(region(imageWidth * 0.58, imageHeight * 0.29, imageWidth * 0.15, imageHeight * 0.11, "Right eye area", 81));
        regions.add(region(imageWidth * 0.43, imageHeight * 0.43, imageWidth * 0.16, imageHeight * 0.13, "Nose center", 72));
        regions.add(region(imageWidth * 0.34, imageHeight * 0.62, imageWidth * 0.32, imageHeight * 0.14, "Mouth area", 76));
        regions.add(region(imageWidth * 0.17, imageHeight * 0.16, imageWidth * 0.66, imageHeight * 0.69, "Face boundary", 64));
        return regions;
    }

    private SuspiciousRegionDTO region(double x, double y, double width, double height, String label, double confidence) {
        SuspiciousRegionDTO dto = new SuspiciousRegionDTO();
        dto.setX(round(x));
        dto.setY(round(y));
        dto.setWidth(round(width));
        dto.setHeight(round(height));
        dto.setLabel(label);
        dto.setConfidence(confidence);
        return dto;
    }

    private String buildExplanationText(String status, Integer finalScore, String notApplicableReason) {
        if ("NOT_APPLICABLE".equals(status)) {
            return StringUtils.hasText(notApplicableReason)
                    ? notApplicableReason
                    : "The image does not contain enough face information for a reliable decision.";
        }

        if (finalScore == null) {
            return "Signals that may affect the manipulation assessment were detected.";
        }

        if (finalScore <= 29) {
            return "The image appears mostly normal, and the abnormal signals in key regions remain low.";
        }
        if (finalScore <= 59) {
            return "Some abnormal signals were detected, so additional review is recommended.";
        }
        if (finalScore <= 79) {
            return "Manipulation-related signals are relatively clear, so detailed review is recommended.";
        }
        return "Multiple regions show strong signals associated with deepfake risk and require careful review.";
    }

    private String buildDetailedReason(String status,
                                       Integer finalScore,
                                       List<SuspiciousRegionDTO> regions,
                                       String fallbackReason) {
        if ("NOT_APPLICABLE".equals(status)) {
            String reason = StringUtils.hasText(fallbackReason)
                    ? fallbackReason
                    : "Face area is missing or too small for reliable analysis.";
            return reason + " This result is AI-assisted guidance and should be reviewed together with source verification.";
        }

        Set<String> labels = new LinkedHashSet<>();
        for (SuspiciousRegionDTO region : regions) {
            if (StringUtils.hasText(region.getLabel())) {
                labels.add(region.getLabel());
            }
            if (labels.size() >= 3) {
                break;
            }
        }

        String joinedLabels = labels.isEmpty() ? "major facial areas" : String.join(", ", labels);
        String intensityText;
        if (finalScore == null) {
            intensityText = "Suspicious signals were found in regions relevant to the decision.";
        } else if (finalScore <= 29) {
            intensityText = "No strong manipulation trace stands out, but faint signals remain in selected regions.";
        } else if (finalScore <= 59) {
            intensityText = "Signals around the eyes and the face boundary are noticeable enough to justify additional review.";
        } else if (finalScore <= 79) {
            intensityText = "Relatively strong abnormal signals appear around the eyes and the face boundary.";
        } else {
            intensityText = "Several critical regions show concentrated abnormal signals that increase deepfake risk.";
        }

        String base = "Regions affecting the manipulation assessment were detected. "
                + "The analysis focused on " + joinedLabels + ". "
                + intensityText;

        if (StringUtils.hasText(fallbackReason) && !fallbackReason.contains("API")) {
            base = base + " " + fallbackReason;
        }

        return base + " This result is AI-assisted guidance, and source verification is recommended before a final decision.";
    }

    private String normalizeStatus(String rawStatus, Integer score, String reason, SavedImage savedImage) {
        if (savedImage.width < 220 || savedImage.height < 220) {
            return "NOT_APPLICABLE";
        }

        if (StringUtils.hasText(rawStatus)) {
            String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
            if ("AUTHENTIC".equals(normalized) || "REAL".equals(normalized)) {
                return "REAL";
            }
            if ("SUSPICIOUS".equals(normalized)) {
                return "SUSPICIOUS";
            }
            if ("FAKE".equals(normalized) || "MANIPULATED".equals(normalized)) {
                return "FAKE";
            }
            if ("NOT_APPLICABLE".equals(normalized) || "UNABLE_TO_EVALUATE".equals(normalized)) {
                return "NOT_APPLICABLE";
            }
        }

        if (StringUtils.hasText(reason)) {
            String normalizedReason = reason.toLowerCase(Locale.ROOT);
            if (normalizedReason.contains("face") && (normalizedReason.contains("not found") || normalizedReason.contains("too small"))) {
                return "NOT_APPLICABLE";
            }
        }

        if (score == null) {
            return "NOT_APPLICABLE";
        }
        if (score <= 29) {
            return "REAL";
        }
        if (score <= 59) {
            return "SUSPICIOUS";
        }
        return "FAKE";
    }

    private String defaultNotApplicableReason(String reason) {
        if (StringUtils.hasText(reason)) {
            return reason;
        }
        return "Face area is missing or too small for reliable analysis.";
    }

    private Integer normalizeScore(double rawScore) {
        if (rawScore < 0) {
            return null;
        }
        if (rawScore <= 1.0) {
            rawScore = rawScore * 100.0;
        }
        int value = (int) Math.round(rawScore);
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }

    private String sanitizeFilename(String name) {
        if (!StringUtils.hasText(name)) {
            return "image.png";
        }
        return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private BufferedImage readImage(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return ImageIO.read(inputStream);
        }
    }

    private boolean isMockAllowed() {
        return mockMode || !StringUtils.hasText(apiKey);
    }

    private String toImageUrl(String savedPath) {
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = Paths.get(savedPath).toAbsolutePath().normalize();
        Path relativePath = basePath.relativize(filePath);
        return "/uploads/" + relativePath.toString().replace("\\", "/");
    }

    private String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    private String firstText(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            JsonNode found = node.at(pointer);
            if (!found.isMissingNode() && !found.isNull() && StringUtils.hasText(found.asText())) {
                return found.asText();
            }
        }
        return null;
    }

    private double firstNumber(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            JsonNode found = node.at(pointer);
            if (found.isMissingNode() || found.isNull()) {
                continue;
            }
            if (found.isNumber()) {
                return found.doubleValue();
            }
            try {
                return Double.parseDouble(found.asText());
            } catch (NumberFormatException ignore) {
            }
        }
        return -1;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class SavedImage {
        private Path absolutePath;
        private String relativeUrl;
        private int width;
        private int height;
    }
}

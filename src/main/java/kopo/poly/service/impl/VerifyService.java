package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.DeepfakeResultDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IVerifyMapper;
import kopo.poly.service.IDeepfakeClient;
import kopo.poly.service.IObjectStorageService;
import kopo.poly.service.IVerifyService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/**
 * 이미지 업로드 저장, 사전 품질 검사, 딥페이크 분석, 결과 저장을 처리한다.
 */
@Service("VerifyService")
public class VerifyService implements IVerifyService {

    // 업로드 분석 결과를 DB에 저장하고 다시 조회하는 MyBatis 매퍼다.
    private final IVerifyMapper verifyMapper;
    // 실제 파일 저장 위치를 숨기고 public URL과 objectKey만 서비스에 돌려준다.
    private final IObjectStorageService objectStorageService;
    // deepfake.client.mode 설정에 따라 더미 분석기 또는 실제 Reality Defender 클라이언트가 주입된다.
    private final IDeepfakeClient deepfakeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VerifyService(IVerifyMapper verifyMapper,
                         IObjectStorageService objectStorageService,
                         IDeepfakeClient deepfakeClient) {
        this.verifyMapper = verifyMapper;
        this.objectStorageService = objectStorageService;
        this.deepfakeClient = deepfakeClient;
    }

    @Override
    public VerifyDTO createVerification(MultipartFile file) throws Exception {
        return createVerification(file, null);
    }

    @Override
    public VerifyDTO createVerification(MultipartFile file, Long userId) throws Exception {
        // 업로드와 검증 API에서 공통으로 사용하는 핵심 분석 파이프라인이다.
        validateImageOnly(file);

        // 업로드 날짜별로 파일이 묶이도록 날짜 기반 objectKey를 만든다.
        String originalName = file.getOriginalFilename();
        String safeName = (originalName == null) ? "image" : originalName.replaceAll("[\\\\/]", "_");
        LocalDate now = LocalDate.now();
        String objectKey = String.format("%04d/%02d/%02d/%s_%s",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                safeName);

        // 원본 업로드 파일을 저장하고 /uploads/** 경로로 접근할 수 있게 한다.
        IObjectStorageService.UploadResult up = objectStorageService.uploadPublic(file, objectKey);

        // 분석 클라이언트가 Path를 받기 때문에 임시 로컬 파일을 만든다.
        Path temp = Files.createTempFile("df_", "_" + safeName);
        file.transferTo(temp.toFile());

        ImageSuitability suitability = inspectImageSuitability(temp);
        DeepfakeResultDTO df;
        if (suitability.notApplicable()) {
            df = buildNotApplicableResult(suitability);
        } else {
            // IDeepfakeClient 구현체는 설정에 따라 더미 분석기 또는 실제 API 분석기로 교체된다.
            df = deepfakeClient.analyze(temp);
        }

        // 임시 파일만 정리한다. 사용자가 볼 원본 파일은 uploads 폴더에 남긴다.
        try {
            Files.deleteIfExists(temp);
        } catch (Exception ignore) {
        }

        // 상세 화면과 이력 화면에서 다시 볼 수 있도록 파일 정보와 분석 원본 응답을 저장한다.
        VerifyDTO pDTO = new VerifyDTO();
        pDTO.setUserId(userId);
        pDTO.setOriginalName(originalName);
        pDTO.setMimeType(file.getContentType());
        pDTO.setFileSize(file.getSize());
        pDTO.setObjectKey(up.getObjectKey());
        pDTO.setPublicUrl(up.getPublicUrl());

        pDTO.setVerdict(df.getVerdict());
        pDTO.setScore(df.getScore());
        pDTO.setApiProvider("reality_defender");
        pDTO.setApiRaw(df.getRaw());

        verifyMapper.insertVerification(pDTO);
        return enrichVerification(verifyMapper.selectVerification(pDTO.getId()));
    }

    @Override
    public VerifyDTO getOne(Long id) {
        return enrichVerification(verifyMapper.selectVerification(id));
    }

    @Override
    public boolean deleteVerification(Long id, Long userId) {
        if (id == null || userId == null) {
            return false;
        }

        return verifyMapper.deleteVerificationByIdAndUserId(id, userId) > 0;
    }

    private void validateImageOnly(MultipartFile file) {
        // MIME 타입, 확장자, 용량을 모두 검사해 분석 대상이 아닌 파일을 초기에 차단한다.
        String ct = file.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";

        boolean okExt = ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("bmp")
                || ext.equals("webp") || ext.equals("heic") || ext.equals("heif");
        if (!okExt) {
            throw new IllegalArgumentException("지원하지 않는 이미지 확장자입니다.");
        }

        long max = 32L * 1024 * 1024;
        if (file.getSize() > max) {
            throw new IllegalArgumentException("이미지 파일은 32MB 미만만 업로드할 수 있습니다.");
        }
    }

    private ImageSuitability inspectImageSuitability(Path imagePath) {
        try {
            BufferedImage image = ImageIO.read(imagePath.toFile());
            if (image == null) {
                return ImageSuitability.notApplicable("이미지를 읽을 수 없어 분석할 수 없습니다.");
            }

            int width = image.getWidth();
            int height = image.getHeight();
            int minSide = Math.min(width, height);
            double aspectRatio = Math.max(width, height) / (double) Math.max(1, minSide);

            // 너무 작은 이미지나 흐릿한 이미지는 분석 결과 신뢰도가 낮기 때문에 NOT_APPLICABLE로 분리한다.
            if (width < 220 || height < 220) {
                return ImageSuitability.notApplicable("얼굴 영역이 너무 작거나 이미지 해상도가 낮아 신뢰할 수 있는 분석을 진행할 수 없습니다.");
            }

            double blurScore = estimateSharpness(image);
            if (blurScore < 28.0) {
                return ImageSuitability.notApplicable("이미지가 흐리게 찍혀 얼굴 특징을 안정적으로 확인할 수 없습니다.");
            }

            SkinRegionStats skinStats = analyzeSkinRegions(image);
            // 얼굴 검출 라이브러리 없이도 발표 시연이 가능하도록 피부색 기반의 간단한 사전 필터를 둔다.
            if (skinStats.skinRatio < 0.006) {
                return ImageSuitability.notApplicable("사람 얼굴로 판단할 수 있는 영역이 부족해 딥페이크 분석 대상이 아닙니다.");
            }

            if (skinStats.largestComponentRatio < 0.018) {
                return ImageSuitability.notApplicable("사람 얼굴로 판단할 수 있는 영역이 부족해 딥페이크 분석 대상이 아닙니다.");
            }

            if (skinStats.componentCount >= 7 && skinStats.largestComponentRatio < 0.035) {
                return ImageSuitability.notApplicable("사람 얼굴로 판단할 수 있는 단일 영역이 확인되지 않아 딥페이크 분석 대상이 아닙니다.");
            }

            if (skinStats.componentCount >= 7 || skinStats.componentCount >= 4) {
                return ImageSuitability.notApplicable("사람이 너무 많거나 얼굴 영역이 여러 개로 나뉘어 단일 얼굴 기준의 분석을 진행할 수 없습니다.");
            }

            if (minSide < 300 && aspectRatio > 1.65) {
                return ImageSuitability.notApplicable("얼굴이 너무 작게 찍혀 신뢰할 수 있는 분석을 진행할 수 없습니다.");
            }

            return ImageSuitability.applicable();
        } catch (Exception e) {
            return ImageSuitability.notApplicable("이미지 품질을 확인할 수 없어 신뢰할 수 있는 분석을 진행할 수 없습니다.");
        }
    }

    private DeepfakeResultDTO buildNotApplicableResult(ImageSuitability suitability) throws Exception {
        // 사전 검사에서 제외된 파일도 동일한 결과 화면을 사용할 수 있게 API 응답 형태의 JSON을 만든다.
        String raw = objectMapper.writeValueAsString(Map.of(
                "provider", "preflight",
                "status", "NOT_APPLICABLE",
                "reason", suitability.reason(),
                "explanation", suitability.reason()
        ));
        return new DeepfakeResultDTO("NOT_APPLICABLE", null, raw);
    }

    private double estimateSharpness(BufferedImage image) {
        // 라플라시안 분산 방식으로 이미지 선명도를 추정한다. 값이 낮을수록 흐린 이미지다.
        int width = image.getWidth();
        int height = image.getHeight();
        int step = Math.max(1, Math.max(width, height) / 450);
        double sum = 0.0;
        double sumSquares = 0.0;
        int count = 0;

        for (int y = step; y < height - step; y += step) {
            for (int x = step; x < width - step; x += step) {
                int center = luminance(image.getRGB(x, y));
                int laplacian = (4 * center)
                        - luminance(image.getRGB(x - step, y))
                        - luminance(image.getRGB(x + step, y))
                        - luminance(image.getRGB(x, y - step))
                        - luminance(image.getRGB(x, y + step));
                sum += laplacian;
                sumSquares += laplacian * laplacian;
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }

        double mean = sum / count;
        return (sumSquares / count) - (mean * mean);
    }

    private int luminance(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return (int) Math.round((0.299 * r) + (0.587 * g) + (0.114 * b));
    }

    private SkinRegionStats analyzeSkinRegions(BufferedImage image) {
        // 이미지를 축소 샘플링한 뒤 피부색으로 보이는 픽셀과 연결 영역 개수를 계산한다.
        int width = image.getWidth();
        int height = image.getHeight();
        int scale = Math.max(1, Math.max(width, height) / 180);
        int sampledWidth = Math.max(1, width / scale);
        int sampledHeight = Math.max(1, height / scale);
        boolean[][] skin = new boolean[sampledHeight][sampledWidth];
        boolean[][] visited = new boolean[sampledHeight][sampledWidth];
        int skinPixels = 0;

        for (int y = 0; y < sampledHeight; y++) {
            for (int x = 0; x < sampledWidth; x++) {
                int rgb = image.getRGB(Math.min(width - 1, x * scale), Math.min(height - 1, y * scale));
                skin[y][x] = isSkinLike(rgb);
                if (skin[y][x]) {
                    skinPixels++;
                }
            }
        }

        int totalPixels = sampledWidth * sampledHeight;
        int components = 0;
        int largest = 0;
        int minComponentSize = Math.max(4, totalPixels / 1200);

        for (int y = 0; y < sampledHeight; y++) {
            for (int x = 0; x < sampledWidth; x++) {
                if (!skin[y][x] || visited[y][x]) {
                    continue;
                }

                int size = floodFillSize(skin, visited, x, y, sampledWidth, sampledHeight);
                if (size >= minComponentSize) {
                    components++;
                    largest = Math.max(largest, size);
                }
            }
        }

        return new SkinRegionStats(
                skinPixels / (double) Math.max(1, totalPixels),
                largest / (double) Math.max(1, totalPixels),
                components
        );
    }

    private int floodFillSize(boolean[][] skin, boolean[][] visited, int startX, int startY, int width, int height) {
        // 연결된 피부색 영역의 크기를 BFS로 계산해 얼굴 후보 영역이 흩어져 있는지 판단한다.
        int size = 0;
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int[] point = queue.poll();
            size++;
            for (int i = 0; i < 4; i++) {
                int nx = point[0] + dx[i];
                int ny = point[1] + dy[i];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || visited[ny][nx] || !skin[ny][nx]) {
                    continue;
                }
                visited[ny][nx] = true;
                queue.add(new int[]{nx, ny});
            }
        }
        return size;
    }

    private boolean isSkinLike(int rgb) {
        // RGB 규칙과 YCbCr 규칙을 함께 사용해 단순 배경색이 피부색으로 오탐되는 것을 줄인다.
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));

        boolean rgbRule = r > 80 && g > 45 && b > 25 && r > g && r > b && max - min > 15 && Math.abs(r - g) > 8;
        double cb = 128 - (0.168736 * r) - (0.331264 * g) + (0.5 * b);
        double cr = 128 + (0.5 * r) - (0.418688 * g) - (0.081312 * b);
        boolean yCbCrRule = cb >= 77 && cb <= 135 && cr >= 133 && cr <= 180;
        return rgbRule && yCbCrRule;
    }

    private VerifyDTO enrichVerification(VerifyDTO dto) {
        if (dto == null) {
            return null;
        }

        dto.setAnalysisJson(buildAnalysisJson(dto));
        return dto;
    }

    private String buildAnalysisJson(VerifyDTO dto) {
        // detail.jsp가 분석 업체별 원본 JSON을 직접 파싱하지 않도록 화면용 JSON으로 정리한다.
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("source", "unknown");
        analysis.put("scorePercent", dto.getScore() == null ? null : (int) Math.round(clampScore(dto.getScore()) * 100.0));
        analysis.put("locationMode", "none");
        analysis.put("regions", List.of());

        String raw = dto.getApiRaw();
        if (raw == null || raw.isBlank()) {
            return writeJsonSafely(analysis);
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            analysis.put("source", resolveAnalysisSource(root));

            List<Map<String, Object>> regions = extractRegions(root);
            if (!regions.isEmpty()) {
                analysis.put("locationMode", "regions");
                analysis.put("regions", regions);
            }

            return writeJsonSafely(analysis);
        } catch (Exception e) {
            return writeJsonSafely(analysis);
        }
    }

    private String resolveAnalysisSource(JsonNode root) {
        // 화면에서 "실제 API/더미/사전검사" 중 어떤 경로의 결과인지 표시하기 위한 값이다.
        String provider = text(root, "/provider");
        String mode = text(root, "/mode");
        if ("preflight".equalsIgnoreCase(provider)) {
            return "preflight";
        }
        if ("dummy".equalsIgnoreCase(provider) || "offline".equalsIgnoreCase(mode)) {
            return "dummy";
        }
        if (root.has("resultsSummary")
                || hasText(root, "/requestId")
                || hasText(root, "/mediaId")
                || hasText(root, "/data/requestId")) {
            return "real";
        }
        return "unknown";
    }

    private List<Map<String, Object>> extractRegions(JsonNode root) {
        // 실제 API와 더미 데이터의 여러 응답 구조를 모두 처리하기 위해 후보 경로를 순서대로 확인한다.
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

            List<Map<String, Object>> regions = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                Map<String, Object> region = toRegion(node);
                if (region != null) {
                    regions.add(region);
                }
            }

            if (!regions.isEmpty()) {
                return regions;
            }
        }

        return List.of();
    }

    private Map<String, Object> toRegion(JsonNode node) {
        double x = firstNumber(node, "/x", "/left", "/bbox/x", "/box/x");
        double y = firstNumber(node, "/y", "/top", "/bbox/y", "/box/y");
        double width = firstNumber(node, "/width", "/w", "/bbox/width", "/box/width");
        double height = firstNumber(node, "/height", "/h", "/bbox/height", "/box/height");

        if (width <= 0 || height <= 0) {
            return null;
        }

        // detail.jsp는 normalized 여부를 보고 비율 좌표와 픽셀 좌표를 다르게 렌더링한다.
        boolean normalized = x <= 1 && y <= 1 && width <= 1 && height <= 1;
        Map<String, Object> region = new LinkedHashMap<>();
        region.put("x", round(x));
        region.put("y", round(y));
        region.put("width", round(width));
        region.put("height", round(height));
        region.put("normalized", normalized);

        String label = firstText(node, "/label", "/name", "/region", "/part", "/title");
        if (label != null && !label.isBlank()) {
            region.put("label", label);
        }

        double confidence = firstNumber(node, "/confidence", "/score", "/intensity", "/weight");
        if (confidence >= 0) {
            region.put("confidence", round(confidence <= 1 ? confidence * 100.0 : confidence));
        }

        return region;
    }

    private String writeJsonSafely(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"source\":\"unknown\",\"locationMode\":\"none\",\"regions\":[]}";
        }
    }

    private boolean hasText(JsonNode node, String pointer) {
        return text(node, pointer) != null;
    }

    private String firstText(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            String value = text(node, pointer);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String pointer) {
        JsonNode found = node.at(pointer);
        if (found.isMissingNode() || found.isNull()) {
            return null;
        }
        String value = found.asText();
        return value == null || value.isBlank() ? null : value;
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
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private double clampScore(Double value) {
        if (value == null) {
            return 0.0;
        }
        if (value < 0) {
            return 0.0;
        }
        if (value > 1) {
            return 1.0;
        }
        return value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ImageSuitability(boolean notApplicable, String reason) {
        private static ImageSuitability applicable() {
            return new ImageSuitability(false, null);
        }

        private static ImageSuitability notApplicable(String reason) {
            return new ImageSuitability(true, reason);
        }
    }

    private record SkinRegionStats(double skinRatio, double largestComponentRatio, int componentCount) {
    }
}

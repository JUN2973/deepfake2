package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(결과 상세): IMD 히트맵 생성 API 호출과 의심 영역 데이터를 처리한다.
 */

/**
 * 발표용 설명: 분석 결과의 시각화 데이터를 만드는 클라이언트입니다.
 * 히트맵, 오버레이 이미지, 의심 영역 좌표를 받아 결과 상세 화면에서 보여줄 수 있는 형태로 변환합니다.
 */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.service.IHeatmapClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

@Component
@ConditionalOnProperty(name = "imd.heatmap.enabled", havingValue = "true")
public class ImdHeatmapClient implements IHeatmapClient {

    private static final Logger log = LoggerFactory.getLogger(ImdHeatmapClient.class);

    private static final double HEATMAP_THRESHOLD = 0.55;
    private static final double MIN_REGION_AREA_RATIO = 0.003;
    private static final double OVERLAY_ALPHA = 0.58;
    private static final double[][] GAUSSIAN_5X5 = {
            {1, 4, 6, 4, 1},
            {4, 16, 24, 16, 4},
            {6, 24, 36, 24, 6},
            {4, 16, 24, 16, 4},
            {1, 4, 6, 4, 1}
    };
    private static final double GAUSSIAN_5X5_SUM = 256.0;

    // Reality Defender 같은 주 분석 결과에 IMD 히트맵만 추가할 때 사용하는 Python 서버 주소다.
    @Value("${imd.base-url:http://127.0.0.1:18080}")
    private String baseUrl;

    // 히트맵 생성은 모델 연산이 들어가므로 일반 HTTP 요청보다 넉넉한 timeout을 둔다.
    @Value("${imd.request-timeout-ms:45000}")
    private long requestTimeoutMs;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ImdLocalServiceManager imdLocalServiceManager;

    public ImdHeatmapClient(RestClient restClient,
                            ObjectMapper objectMapper,
                            ImdLocalServiceManager imdLocalServiceManager) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.imdLocalServiceManager = imdLocalServiceManager;
    }

    @Override
    public Optional<HeatmapResult> generateHeatmap(Path savedFilePath) throws Exception {
        // 이 메서드는 IMD를 보조 분석기로 사용한다.
        // 주 판정은 유지하고, 화면에서 위치를 보여줄 히트맵 데이터만 추가로 가져온다.
        if (!StringUtils.hasText(baseUrl)) {
            return Optional.empty();
        }

        String requestUrl = trimTrailingSlash(baseUrl) + "/analyze";
        // 로컬 서버가 내려가 있으면 자동 시작 후 health check를 기다린다.
        imdLocalServiceManager.ensureAvailable(baseUrl);
        byte[] bytes = Files.readAllBytes(savedFilePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));

        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return savedFilePath.getFileName().toString();
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        String responseBody;
        try {
            // Python FastAPI의 /analyze 엔드포인트에 이미지를 multipart로 전달한다.
            byte[] responseBytes = imdRequestClient().post()
                    .uri(requestUrl)
                    .headers(requestHeaders -> requestHeaders.addAll(headers))
                    .body(body)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            String errorBody = new String(StreamUtils.copyToByteArray(response.getBody()), StandardCharsets.UTF_8);
                            throw new IllegalStateException("IMD heatmap request failed: "
                                    + response.getStatusCode() + " " + errorBody);
                        }
                        return StreamUtils.copyToByteArray(response.getBody());
                    });
            responseBody = responseBodyAsString(responseBytes);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("IMD heatmap request failed: "
                    + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }

        if (!StringUtils.hasText(responseBody)) {
            return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(responseBody);
        // IMD 응답에서 화면에 필요한 raw/processed/overlay 히트맵을 찾아낸다.
        JsonNode rawHeatmap = firstNode(root, "/rawHeatmap", "/raw_heatmap");
        JsonNode processedHeatmap = firstNode(root, "/processedHeatmap", "/processed_heatmap", "/heatmap", "/mantranet/heatmap", "/data/heatmap");
        if (processedHeatmap == null || !processedHeatmap.isObject()) {
            return Optional.empty();
        }
        JsonNode overlay = firstNode(root, "/overlayHeatmap", "/overlay_heatmap", "/overlay", "/mantranet/overlay", "/data/overlay");

        String serverProcessedData = firstText(processedHeatmap, "/data", "/base64");
        if (!StringUtils.hasText(serverProcessedData)) {
            return Optional.empty();
        }

        String rawData = rawHeatmap == null ? null : firstText(rawHeatmap, "/data", "/base64");
        String rawType = rawHeatmap == null ? null : firstText(rawHeatmap, "/type", "/mimeType");
        String processedType = firstText(processedHeatmap, "/type", "/mimeType");
        String serverOverlayData = overlay == null ? null : firstText(overlay, "/data", "/base64");
        String overlayType = overlay == null ? null : firstText(overlay, "/type", "/mimeType");
        String modelName = firstText(root, "/modelName", "/model", "/data/modelName");

        String processedData = serverProcessedData;
        String overlayData = serverOverlayData;
        try {
            PostProcessedHeatmap userHeatmap = buildUserHeatmap(
                    savedFilePath,
                    StringUtils.hasText(rawData) ? rawData : serverProcessedData
            );
            processedData = userHeatmap.processedData();
            overlayData = userHeatmap.overlayData();
        } catch (Exception e) {
            log.warn("Failed to post-process IMD heatmap. Keeping server heatmap response.", e);
        }

        return Optional.of(new HeatmapResult(
                "imd",
                StringUtils.hasText(modelName) ? modelName : "IMD ManTraNet",
                StringUtils.hasText(rawType) ? rawType : "image/png",
                rawData,
                StringUtils.hasText(processedType) ? processedType : "image/png",
                processedData,
                StringUtils.hasText(overlayType) ? overlayType : "image/png",
                overlayData,
                extractRegions(root)
        ));
    }

    private PostProcessedHeatmap buildUserHeatmap(Path originalImagePath, String heatmapBase64) throws Exception {
        BufferedImage original = ImageIO.read(originalImagePath.toFile());
        BufferedImage rawHeatmap = ImageIO.read(new ByteArrayInputStream(decodeBase64Image(heatmapBase64)));
        if (original == null || rawHeatmap == null) {
            throw new IllegalArgumentException("Heatmap or original image could not be decoded.");
        }

        BufferedImage resizedHeatmap = resize(rawHeatmap, original.getWidth(), original.getHeight());
        double[][] normalized = normalizeHeatmap(resizedHeatmap);
        boolean[][] mask = threshold(normalized, HEATMAP_THRESHOLD);
        mask = removeSmallComponents(mask, MIN_REGION_AREA_RATIO);
        double[][] cleaned = applyMask(normalized, mask);
        double[][] smoothed = gaussianBlur(cleaned);
        smoothed = applyMask(smoothed, mask);
        smoothed = normalizeValues(smoothed);

        boolean[][] finalMask = threshold(smoothed, 0.01);
        boolean[][] largestRegion = largestComponent(finalMask);
        BufferedImage processed = renderProcessedHeatmap(smoothed, finalMask, largestRegion);
        BufferedImage overlay = renderOverlay(original, smoothed, finalMask, largestRegion);
        return new PostProcessedHeatmap(encodePng(processed), encodePng(overlay));
    }

    private byte[] decodeBase64Image(String value) {
        String data = value == null ? "" : value.trim();
        int comma = data.indexOf(',');
        if (data.startsWith("data:") && comma >= 0) {
            data = data.substring(comma + 1);
        }
        return Base64.getDecoder().decode(data);
    }

    private String responseBodyAsString(byte[] body) {
        return body == null || body.length == 0 ? "" : new String(body, StandardCharsets.UTF_8);
    }

    private BufferedImage resize(BufferedImage source, int width, int height) {
        if (source.getWidth() == width && source.getHeight() == height) {
            return source;
        }
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    private double[][] normalizeHeatmap(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] values = new double[height][width];
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                int r = (argb >>> 16) & 0xff;
                int g = (argb >>> 8) & 0xff;
                int b = argb & 0xff;
                double value = ((0.299 * r) + (0.587 * g) + (0.114 * b)) / 255.0;
                value *= alpha / 255.0;
                values[y][x] = value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }

        double range = max - min;
        if (range <= 0.000001) {
            return values;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                values[y][x] = clamp01((values[y][x] - min) / range);
            }
        }
        return values;
    }

    private boolean[][] threshold(double[][] values, double threshold) {
        int height = values.length;
        int width = values[0].length;
        boolean[][] mask = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mask[y][x] = values[y][x] >= threshold;
            }
        }
        return mask;
    }

    private boolean[][] removeSmallComponents(boolean[][] mask, double minAreaRatio) {
        int height = mask.length;
        int width = mask[0].length;
        int minArea = Math.max(16, (int) Math.round(width * height * minAreaRatio));
        boolean[][] keep = new boolean[height][width];
        boolean[][] visited = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x] || visited[y][x]) {
                    continue;
                }
                List<int[]> component = collectComponent(mask, visited, x, y);
                if (component.size() >= minArea) {
                    for (int[] point : component) {
                        keep[point[1]][point[0]] = true;
                    }
                }
            }
        }
        return keep;
    }

    private boolean[][] largestComponent(boolean[][] mask) {
        int height = mask.length;
        int width = mask[0].length;
        boolean[][] visited = new boolean[height][width];
        List<int[]> largest = List.of();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x] || visited[y][x]) {
                    continue;
                }
                List<int[]> component = collectComponent(mask, visited, x, y);
                if (component.size() > largest.size()) {
                    largest = component;
                }
            }
        }

        boolean[][] result = new boolean[height][width];
        for (int[] point : largest) {
            result[point[1]][point[0]] = true;
        }
        return result;
    }

    private List<int[]> collectComponent(boolean[][] mask, boolean[][] visited, int startX, int startY) {
        int height = mask.length;
        int width = mask[0].length;
        int[] dx = {1, -1, 0, 0, 1, 1, -1, -1};
        int[] dy = {0, 0, 1, -1, 1, -1, 1, -1};
        List<int[]> component = new ArrayList<>();
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            int[] point = queue.poll();
            component.add(point);
            for (int i = 0; i < dx.length; i++) {
                int nx = point[0] + dx[i];
                int ny = point[1] + dy[i];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || visited[ny][nx] || !mask[ny][nx]) {
                    continue;
                }
                visited[ny][nx] = true;
                queue.add(new int[]{nx, ny});
            }
        }
        return component;
    }

    private double[][] applyMask(double[][] values, boolean[][] mask) {
        int height = values.length;
        int width = values[0].length;
        double[][] result = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = mask[y][x] ? values[y][x] : 0.0;
            }
        }
        return result;
    }

    private double[][] gaussianBlur(double[][] values) {
        int height = values.length;
        int width = values[0].length;
        double[][] result = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sum = 0.0;
                for (int ky = -2; ky <= 2; ky++) {
                    for (int kx = -2; kx <= 2; kx++) {
                        int px = Math.min(width - 1, Math.max(0, x + kx));
                        int py = Math.min(height - 1, Math.max(0, y + ky));
                        sum += values[py][px] * GAUSSIAN_5X5[ky + 2][kx + 2];
                    }
                }
                result[y][x] = sum / GAUSSIAN_5X5_SUM;
            }
        }
        return result;
    }

    private double[][] normalizeValues(double[][] values) {
        int height = values.length;
        int width = values[0].length;
        double max = 0.0;
        for (double[] row : values) {
            for (double value : row) {
                max = Math.max(max, value);
            }
        }
        if (max <= 0.000001) {
            return values;
        }
        double[][] result = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y][x] = clamp01(values[y][x] / max);
            }
        }
        return result;
    }

    private BufferedImage renderProcessedHeatmap(double[][] values, boolean[][] mask, boolean[][] outlineRegion) {
        int height = values.length;
        int width = values[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x]) {
                    image.setRGB(x, y, 0x00000000);
                    continue;
                }
                int[] color = infernoColor(values[y][x]);
                int alpha = (int) Math.round(40 + (205 * values[y][x]));
                image.setRGB(x, y, argb(alpha, color[0], color[1], color[2]));
            }
        }
        drawOutline(image, outlineRegion);
        return image;
    }

    private BufferedImage renderOverlay(BufferedImage original, double[][] values, boolean[][] mask, boolean[][] outlineRegion) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x]) {
                    continue;
                }
                int base = image.getRGB(x, y);
                int[] color = infernoColor(values[y][x]);
                double alpha = OVERLAY_ALPHA * Math.max(0.25, values[y][x]);
                int r = blend((base >>> 16) & 0xff, color[0], alpha);
                int gr = blend((base >>> 8) & 0xff, color[1], alpha);
                int b = blend(base & 0xff, color[2], alpha);
                image.setRGB(x, y, argb(255, r, gr, b));
            }
        }
        drawOutline(image, outlineRegion);
        return image;
    }

    private void drawOutline(BufferedImage image, boolean[][] region) {
        int height = region.length;
        int width = region[0].length;
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(255, 235, 150, 230));
        g.setStroke(new BasicStroke(Math.max(1.5f, Math.min(width, height) / 350.0f)));
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (!region[y][x]) {
                    continue;
                }
                if (!region[y - 1][x] || !region[y + 1][x] || !region[y][x - 1] || !region[y][x + 1]) {
                    g.drawLine(x, y, x, y);
                }
            }
        }
        g.dispose();
    }

    private String encodePng(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private int[] infernoColor(double value) {
        double v = clamp01(value);
        int[][] stops = {
                {0, 0, 4},
                {31, 12, 72},
                {85, 15, 109},
                {136, 34, 106},
                {186, 54, 85},
                {227, 89, 51},
                {249, 140, 10},
                {249, 201, 50},
                {252, 255, 164}
        };
        double scaled = v * (stops.length - 1);
        int index = Math.min(stops.length - 2, (int) Math.floor(scaled));
        double t = scaled - index;
        return new int[]{
                lerp(stops[index][0], stops[index + 1][0], t),
                lerp(stops[index][1], stops[index + 1][1], t),
                lerp(stops[index][2], stops[index + 1][2], t)
        };
    }

    private int lerp(int a, int b, double t) {
        return (int) Math.round(a + ((b - a) * t));
    }

    private int blend(int base, int overlay, double alpha) {
        return (int) Math.round((base * (1.0 - alpha)) + (overlay * alpha));
    }

    private int argb(int a, int r, int g, int b) {
        return ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private List<Map<String, Object>> extractRegions(JsonNode root) {
        // Python 서버가 내려준 의심 영역 좌표를 화면용 List<Map> 형태로 정리한다.
        String[] pointers = {
                "/regions",
                "/suspiciousRegions",
                "/data/regions",
                "/data/suspiciousRegions",
                "/mantranet/regions",
                "/mantranet/suspiciousRegions"
        };

        for (String pointer : pointers) {
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
        // 좌표는 0~1 비율 좌표 또는 픽셀 좌표가 올 수 있다.
        // normalized 값을 함께 내려 JSP가 어떤 방식으로 그릴지 판단할 수 있게 한다.
        double x = firstNumber(node, "/x", "/left", "/bbox/x", "/box/x");
        double y = firstNumber(node, "/y", "/top", "/bbox/y", "/box/y");
        double width = firstNumber(node, "/width", "/w", "/bbox/width", "/box/width");
        double height = firstNumber(node, "/height", "/h", "/bbox/height", "/box/height");

        if (width <= 0 || height <= 0) {
            return null;
        }

        Map<String, Object> region = new LinkedHashMap<>();
        region.put("x", round(x));
        region.put("y", round(y));
        region.put("width", round(width));
        region.put("height", round(height));
        region.put("normalized", x <= 1 && y <= 1 && width <= 1 && height <= 1);

        String label = firstText(node, "/label", "/name", "/region", "/part", "/title", "/type");
        region.put("label", StringUtils.hasText(label) ? label : "픽셀 패턴 이상 영역");

        Double confidence = firstNumberOrNull(node, "/confidence", "/score", "/intensity", "/weight");
        if (confidence != null) {
            region.put("confidence", round(confidence > 1.0 ? confidence / 100.0 : confidence));
        }

        return region;
    }

    private JsonNode firstNode(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            JsonNode found = node.at(pointer);
            if (!found.isMissingNode() && !found.isNull()) {
                return found;
            }
        }
        return null;
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

    private Double firstNumberOrNull(JsonNode node, String... pointers) {
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
        return null;
    }

    private double firstNumber(JsonNode node, String... pointers) {
        Double value = firstNumberOrNull(node, pointers);
        return value == null ? -1 : value;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private String trimTrailingSlash(String value) {
        return value.replaceAll("/+$", "");
    }

    private RestClient imdRequestClient() {
        // 기본 RestClient 대신 IMD 요청 전용 timeout을 가진 RestClient를 매번 생성한다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(3000));
        factory.setReadTimeout(Duration.ofMillis(Math.max(5000, requestTimeoutMs)));
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    private record PostProcessedHeatmap(String processedData, String overlayData) {
    }
}

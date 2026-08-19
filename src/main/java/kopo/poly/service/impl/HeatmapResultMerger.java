package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.DeepfakeResultDTO;
import kopo.poly.service.IHeatmapClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges an optional IMD heatmap response into the primary deepfake analysis result.
 */
final class HeatmapResultMerger {

    private static final Logger log = LoggerFactory.getLogger(HeatmapResultMerger.class);

    private final ObjectMapper objectMapper;

    HeatmapResultMerger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    DeepfakeResultDTO merge(DeepfakeResultDTO primary,
                            String primaryProvider,
                            IHeatmapClient.HeatmapResult heatmap) {
        try {
            Map<String, Object> imdHeatmapResult = buildHeatmapResult(heatmap);

            Map<String, Object> merged = new LinkedHashMap<>();
            merged.put("provider", "unknown".equals(primaryProvider) ? "reality_defender" : primaryProvider);
            merged.put("realityResult", objectMapper.readTree(primary.getRaw()));
            merged.put("imdHeatmapResult", imdHeatmapResult);

            return new DeepfakeResultDTO(
                    primary.getVerdict(),
                    primary.getScore(),
                    objectMapper.writeValueAsString(merged)
            );
        } catch (Exception e) {
            log.warn("Failed to merge optional IMD heatmap with primary analysis.", e);
            return primary;
        }
    }

    private Map<String, Object> buildHeatmapResult(IHeatmapClient.HeatmapResult heatmap) {
        Map<String, Object> imdHeatmapResult = new LinkedHashMap<>();
        imdHeatmapResult.put("provider", heatmap.provider());
        imdHeatmapResult.put("modelName", heatmap.modelName());

        Map<String, Object> rawHeatmapData = buildImageData(heatmap.rawData(), heatmap.rawType());
        if (!rawHeatmapData.isEmpty()) {
            imdHeatmapResult.put("rawHeatmap", rawHeatmapData);
        }

        Map<String, Object> processedHeatmapData = buildImageData(heatmap.processedData(), heatmap.processedType());
        imdHeatmapResult.put("processedHeatmap", processedHeatmapData);
        imdHeatmapResult.put("heatmap", processedHeatmapData);

        Map<String, Object> overlayData = buildImageData(heatmap.overlayData(), heatmap.overlayType());
        if (!overlayData.isEmpty()) {
            imdHeatmapResult.put("overlayHeatmap", overlayData);
            imdHeatmapResult.put("overlay", overlayData);
        }

        imdHeatmapResult.put("regions", heatmap.regions() == null ? List.of() : heatmap.regions());
        return imdHeatmapResult;
    }

    private Map<String, Object> buildImageData(String data, String type) {
        Map<String, Object> imageData = new LinkedHashMap<>();
        if (data == null || data.isBlank()) {
            return imageData;
        }

        imageData.put("type", type == null || type.isBlank() ? "image/png" : type);
        imageData.put("data", data);
        return imageData;
    }
}

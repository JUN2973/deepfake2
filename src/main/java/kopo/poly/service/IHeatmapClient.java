package kopo.poly.service;


/**
 * 체크리스트 기준 주석: 설계/API 연동 설계: 컨트롤러와 구현체 사이의 서비스 계약을 정의한다.
 */
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optional localization heatmap provider used to enrich the primary analysis result.
 */
public interface IHeatmapClient {
    Optional<HeatmapResult> generateHeatmap(Path savedFilePath) throws Exception;

    record HeatmapResult(String provider,
                         String modelName,
                         String rawType,
                         String rawData,
                         String processedType,
                         String processedData,
                         String overlayType,
                         String overlayData,
                         List<Map<String, Object>> regions) {
    }
}

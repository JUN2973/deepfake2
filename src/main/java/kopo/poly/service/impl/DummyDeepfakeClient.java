package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별): 외부 API 없이 테스트 가능한 더미 분석 결과를 제공한다.
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.DeepfakeResultDTO;
import kopo.poly.service.IDeepfakeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

/**
 * 외부 API 없이 개발/시연할 수 있도록 가짜 분석 결과를 만드는 구현체다.
 */
@Component
@ConditionalOnProperty(name = "deepfake.client.mode", havingValue = "dummy", matchIfMissing = true)
public class DummyDeepfakeClient implements IDeepfakeClient {

    @Value("${rd.dummy.verdict:SUSPICIOUS}")
    private String verdict;

    @Value("${rd.dummy.score:0.82}")
    private Double score;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DeepfakeResultDTO analyze(Path savedFilePath) throws Exception {
        // 오프라인/시연 모드다. 외부 API 키가 없어도 앱을 시연할 수 있다.
        String raw = objectMapper.writeValueAsString(Map.of(
                "provider", "dummy",
                "mode", "offline",
                "fileName", savedFilePath.getFileName().toString(),
                "verdict", verdict,
                "score", score
        ));
        return new DeepfakeResultDTO(verdict, score, raw);
    }
}

package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.DeepfakeResultDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IVerifyMapper;
import kopo.poly.service.IDeepfakeClient;
import kopo.poly.service.IHeatmapClient;
import kopo.poly.service.IObjectStorageService;
import kopo.poly.service.IVerifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

/**
 * 업로드된 이미지의 저장, 사전 검사, 딥페이크 분석, 결과 저장을 조율하는 서비스다.
 */
@Service("VerifyService")
public class VerifyService implements IVerifyService {

    private static final Logger log = LoggerFactory.getLogger(VerifyService.class);

    // 분석 결과를 DB에 저장하고 다시 조회하는 MyBatis 매퍼다.
    private final IVerifyMapper verifyMapper;
    // 설정에 따라 더미 분석기 또는 실제 분석 클라이언트가 주입된다.
    private final IDeepfakeClient deepfakeClient;
    private final ObjectProvider<IHeatmapClient> heatmapClientProvider;
    private final VerificationFilePreparer verificationFilePreparer;
    private final ImageSuitabilityInspector imageSuitabilityInspector = new ImageSuitabilityInspector();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HeatmapResultMerger heatmapResultMerger = new HeatmapResultMerger(objectMapper);
    private final VerificationAnalysisBuilder verificationAnalysisBuilder = new VerificationAnalysisBuilder(objectMapper);

    public VerifyService(IVerifyMapper verifyMapper,
                         IObjectStorageService objectStorageService,
                         IDeepfakeClient deepfakeClient,
                         ObjectProvider<IHeatmapClient> heatmapClientProvider) {
        this.verifyMapper = verifyMapper;
        this.deepfakeClient = deepfakeClient;
        this.heatmapClientProvider = heatmapClientProvider;
        this.verificationFilePreparer = new VerificationFilePreparer(objectStorageService);
    }

    @Override
    public VerifyDTO createVerification(MultipartFile file) throws Exception {
        return createVerification(file, null);
    }

    @Override
    public VerifyDTO createVerification(MultipartFile file, Long userId) throws Exception {
        VerificationFilePreparer.PreparedUpload upload = verificationFilePreparer.prepare(file);
        DeepfakeResultDTO df;
        try (upload) {
            Path temp = upload.tempFilePath();
            ImageSuitabilityInspector.ImageSuitability suitability = imageSuitabilityInspector.inspect(temp);
            if (suitability.notApplicable()) {
                df = buildNotApplicableResult(suitability);
            } else {
                // 분석 클라이언트 구현체는 설정에 따라 교체된다.
                df = deepfakeClient.analyze(temp);
                df = enrichWithOptionalHeatmap(temp, df);
            }
        }

        // 상세 화면과 이력 화면에서 다시 볼 수 있도록 파일 정보와 분석 원본 응답을 저장한다.
        VerifyDTO pDTO = new VerifyDTO();
        pDTO.setUserId(userId);
        pDTO.setOriginalName(upload.originalName());
        pDTO.setMimeType(file.getContentType());
        pDTO.setFileSize(file.getSize());
        pDTO.setObjectKey(upload.uploadResult().getObjectKey());
        pDTO.setPublicUrl(upload.uploadResult().getPublicUrl());

        pDTO.setVerdict(df.getVerdict());
        pDTO.setScore(df.getScore());
        pDTO.setApiProvider(resolveApiProvider(df.getRaw()));
        pDTO.setApiRaw(df.getRaw());

        verifyMapper.insertVerification(pDTO);
        return enrichVerification(verifyMapper.selectVerification(pDTO.getId()));
    }

    private String resolveApiProvider(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            String provider = text(root, "/provider");
            if (provider != null) {
                return provider;
            }
            JsonNode realityResult = root.at("/realityResult");
            if (!realityResult.isMissingNode() && !realityResult.isNull()) {
                return resolveApiProvider(objectMapper.writeValueAsString(realityResult));
            }
            JsonNode primary = root.at("/primary");
            if (!primary.isMissingNode() && !primary.isNull()) {
                return resolveApiProvider(objectMapper.writeValueAsString(primary));
            }
            if (root.has("heatmap") || root.has("symptoms")) {
                return "imd";
            }
            if (root.has("resultsSummary") || hasText(root, "/requestId") || hasText(root, "/mediaId")) {
                return "reality_defender";
            }
        } catch (Exception ignored) {
        }

        return "unknown";
    }

    private DeepfakeResultDTO enrichWithOptionalHeatmap(Path temp, DeepfakeResultDTO primary) {
        if (primary == null || primary.getRaw() == null || primary.getRaw().isBlank()) {
            return primary;
        }

        String primaryProvider = resolveApiProvider(primary.getRaw());
        if ("imd".equalsIgnoreCase(primaryProvider) || isNotApplicableVerdict(primary.getVerdict())) {
            return primary;
        }

        IHeatmapClient heatmapClient = heatmapClientProvider.getIfAvailable();
        if (heatmapClient == null) {
            return primary;
        }

        try {
            return heatmapClient.generateHeatmap(temp)
                    .map(heatmap -> heatmapResultMerger.merge(primary, primaryProvider, heatmap))
                    .orElse(primary);
        } catch (Exception e) {
            log.warn("Optional IMD heatmap generation failed. Keeping primary analysis only.", e);
            return primary;
        }
    }

    private boolean isNotApplicableVerdict(String verdict) {
        return "NOT_APPLICABLE".equals(verdict) || "UNABLE_TO_EVALUATE".equals(verdict);
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

    private DeepfakeResultDTO buildNotApplicableResult(ImageSuitabilityInspector.ImageSuitability suitability) throws Exception {
        // 사전 검사에서 제외된 파일도 동일한 결과 화면을 사용할 수 있게 JSON으로 만든다.
        String raw = objectMapper.writeValueAsString(Map.of(
                "provider", "preflight",
                "status", "NOT_APPLICABLE",
                "reason", suitability.reason(),
                "explanation", suitability.reason()
        ));
        return new DeepfakeResultDTO("NOT_APPLICABLE", null, raw);
    }

    private VerifyDTO enrichVerification(VerifyDTO dto) {
        if (dto == null) {
            return null;
        }

        dto.setAnalysisJson(verificationAnalysisBuilder.build(dto));
        return dto;
    }

    private boolean hasText(JsonNode node, String pointer) {
        return text(node, pointer) != null;
    }

    private String text(JsonNode node, String pointer) {
        JsonNode found = node.at(pointer);
        if (found.isMissingNode() || found.isNull()) {
            return null;
        }
        String value = found.asText();
        return value == null || value.isBlank() ? null : value;
    }
}

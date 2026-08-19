package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.VerifyDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 결과/이력 화면에서 쓰기 좋은 분석 JSON을 만든다.
 */
final class VerificationAnalysisBuilder {

    private final ObjectMapper objectMapper;

    VerificationAnalysisBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String build(VerifyDTO dto) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("source", "unknown");
        analysis.put("scorePercent", dto.getScore() == null ? null : (int) Math.round(clampScore(dto.getScore()) * 100.0));
        analysis.put("locationMode", "none");
        analysis.put("regions", List.of());

        String raw = dto.getApiRaw();
        if (raw == null || raw.isBlank()) {
            addEnhancedExplanation(analysis, dto);
            return writeJsonSafely(analysis);
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            analysis.put("source", resolveAnalysisSource(root));
            analysis.put("realityResult", objectMapper.convertValue(firstNode(root, "/realityResult", "/primary", ""), Object.class));

            JsonNode imdHeatmapResult = firstNode(root, "/imdHeatmapResult");
            if (imdHeatmapResult != null && imdHeatmapResult.isObject()) {
                analysis.put("imdHeatmapResult", objectMapper.convertValue(imdHeatmapResult, Object.class));
            }

            List<Map<String, Object>> regions = extractRegions(imdHeatmapResult != null ? imdHeatmapResult : root);
            if (!regions.isEmpty()) {
                analysis.put("locationMode", "regions");
                analysis.put("regions", regions);
            }

            addImageDataIfPresent(analysis, root,
                    "rawHeatmap",
                    "/imdHeatmapResult/rawHeatmap",
                    "/rawHeatmap",
                    "/raw_heatmap");
            addImageDataIfPresent(analysis, root,
                    "heatmap",
                    "/imdHeatmapResult/processedHeatmap",
                    "/imdHeatmapResult/heatmap",
                    "/processedHeatmap",
                    "/heatmap",
                    "/mantranet/heatmap",
                    "/data/heatmap");
            addImageDataIfPresent(analysis, root,
                    "overlay",
                    "/imdHeatmapResult/overlayHeatmap",
                    "/imdHeatmapResult/overlay",
                    "/overlayHeatmap",
                    "/overlay",
                    "/mantranet/overlay",
                    "/data/overlay");

            List<Map<String, Object>> symptoms = extractSymptoms(root);
            if (!symptoms.isEmpty()) {
                analysis.put("symptoms", symptoms);
            }

            addEnhancedExplanation(analysis, dto);
            return writeJsonSafely(analysis);
        } catch (Exception e) {
            addEnhancedExplanation(analysis, dto);
            return writeJsonSafely(analysis);
        }
    }

    private void addImageDataIfPresent(Map<String, Object> analysis,
                                       JsonNode root,
                                       String key,
                                       String... pointers) {
        JsonNode imageNode = firstNode(root, pointers);
        if (imageNode == null || !imageNode.isObject()) {
            return;
        }

        String type = firstText(imageNode, "/type", "/mimeType");
        String data = firstText(imageNode, "/data", "/base64");
        if (data == null || data.isBlank()) {
            return;
        }

        Map<String, Object> imageData = new LinkedHashMap<>();
        imageData.put("type", type == null ? "image/png" : type);
        imageData.put("data", data);
        analysis.put("locationMode", "heatmap");
        analysis.put(key, imageData);

        if ("heatmap".equals(key)) {
            analysis.put("processedHeatmap", imageData);
        } else if ("overlay".equals(key)) {
            analysis.put("overlayHeatmap", imageData);
        }
    }

    private void addEnhancedExplanation(Map<String, Object> analysis, VerifyDTO dto) {
        Map<String, Object> explanation = buildEnhancedExplanation(analysis, dto);
        analysis.put("enhancedExplanation", explanation);
        analysis.put("explanationText", explanation.get("summary"));
        analysis.put("disclaimer",
                "AI 분석 결과는 원본 여부를 확정하는 값이 아닙니다. 이미지에서 주요 조작 의심 패턴과 다르게 감지된 영역을 보여주는 참고용 분석입니다.");
    }

    private Map<String, Object> buildEnhancedExplanation(Map<String, Object> analysis, VerifyDTO dto) {
        Integer scorePercent = dto.getScore() == null ? null : (int) Math.round(clampScore(dto.getScore()) * 100.0);
        String verdict = dto.getVerdict() == null ? "UNKNOWN" : dto.getVerdict().toUpperCase();
        int regionCount = countListValue(analysis.get("regions"));
        int symptomCount = countListValue(analysis.get("symptoms"));
        boolean hasHeatmap = analysis.containsKey("heatmap")
                || analysis.containsKey("processedHeatmap")
                || analysis.containsKey("overlay")
                || analysis.containsKey("overlayHeatmap");

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> evidence = new ArrayList<>();
        List<String> cautions = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (isNotApplicableVerdict(verdict)) {
            result.put("level", "분석 불가");
            result.put("summary", extractUnavailableSummary(dto));
            evidence.add("이미지에서 신뢰할 수 있는 얼굴 분석에 필요한 정보가 충분하지 않습니다.");
            evidence.add("얼굴이 너무 작거나 흐릿하거나 여러 명이 포함된 경우 분석 정확도가 낮아질 수 있습니다.");
            cautions.add("이 결과는 조작 가능성이 낮다는 뜻이 아니라 판단 가능한 정보가 부족하다는 의미입니다.");
            recommendations.add("얼굴이 크게 보이는 원본 이미지를 다시 업로드해 보세요.");
            recommendations.add("가능하면 캡처본보다 원본 파일이나 더 높은 해상도의 이미지를 사용해 주세요.");
            result.put("evidence", evidence);
            result.put("cautions", cautions);
            result.put("recommendations", recommendations);
            return result;
        }

        String level;
        String summary;
        if (scorePercent == null) {
            level = "점수 확인 필요";
            summary = "분석 점수가 제공되지 않아 정량적인 위험 판단은 제한됩니다.";
            evidence.add("모델 응답에 유효한 점수 값이 없거나 해석할 수 없는 값이 포함되어 있습니다.");
        } else if (scorePercent <= 29) {
            level = "낮음";
            summary = "조작 의심 신호가 낮게 감지되었습니다. 주요 얼굴 영역에서 강한 합성 흔적은 두드러지지 않습니다.";
            evidence.add("위험 점수가 낮은 구간에 있어 모델 기준의 조작 의심 신호가 약합니다.");
        } else if (scorePercent <= 59) {
            level = "주의";
            summary = "일부 조작 의심 신호가 감지되었습니다. 이미지 원본과 편집 이력을 함께 확인하는 것이 좋습니다.";
            evidence.add("위험 점수가 중간 구간에 있어 보정, 편집, 합성 가능성을 함께 검토해야 합니다.");
        } else if (scorePercent <= 79) {
            level = "의심";
            summary = "조작 가능성을 시사하는 신호가 비교적 강합니다. 원본 출처 확인과 추가 검증이 필요합니다.";
            evidence.add("위험 점수가 높은 구간에 있어 얼굴 변형 또는 부분 편집 가능성을 우선 검토해야 합니다.");
        } else {
            level = "강한 의심";
            summary = "여러 조작 의심 신호가 강하게 감지되었습니다. 단독 판단보다 원본 파일과 다른 검증 결과를 함께 확인해야 합니다.";
            evidence.add("위험 점수가 매우 높은 구간에 있어 모델 기준의 딥페이크 위험 신호가 강합니다.");
        }

        if (regionCount > 0) {
            evidence.add("의심 패턴 이상 영역 데이터가 " + regionCount + "개 감지되어 위치 기반 검토가 가능합니다.");
        } else if (hasHeatmap) {
            evidence.add("정확한 박스 좌표는 없지만 히트맵 기반 참고 시각화가 제공됩니다.");
        } else {
            evidence.add("위치 데이터가 없어 점수와 모델 응답 중심으로 해석해야 합니다.");
        }

        if (symptomCount > 0) {
            evidence.add("모델이 제공한 이상 징후 설명 " + symptomCount + "개가 함께 반영되었습니다.");
        }

        cautions.add("AI 분석 결과만으로 원본 여부를 확정하면 안 됩니다.");
        cautions.add("캡처, 리사이즈, 필터, 강한 보정, 재업로드 과정에서도 유사한 이상 패턴이 생길 수 있습니다.");
        recommendations.add("원본 이미지 파일, 게시 출처, 업로드 이력, 촬영 맥락을 함께 확인해 주세요.");
        recommendations.add("같은 이미지의 고해상도 버전이 있다면 다시 분석해 결과가 일관되는지 확인해 주세요.");

        result.put("level", level);
        result.put("summary", summary);
        result.put("evidence", evidence);
        result.put("cautions", cautions);
        result.put("recommendations", recommendations);
        return result;
    }

    private int countListValue(Object value) {
        return value instanceof List<?> list ? list.size() : 0;
    }

    private String extractUnavailableSummary(VerifyDTO dto) {
        String raw = dto.getApiRaw();
        if (raw != null && !raw.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(raw);
                String reason = firstText(root, "/reason", "/explanation", "/message", "/data/reason");
                if (reason != null && !reason.isBlank()) {
                    return reason;
                }
            } catch (Exception ignored) {
            }
        }
        return "이미지가 신뢰할 수 있는 딥페이크 분석 기준을 충족하지 못해 판정을 보류했습니다.";
    }

    private String resolveAnalysisSource(JsonNode root) {
        String provider = text(root, "/provider");
        String mode = text(root, "/mode");
        if ("preflight".equalsIgnoreCase(provider)) {
            return "preflight";
        }
        if ("dummy".equalsIgnoreCase(provider) || "offline".equalsIgnoreCase(mode)) {
            return "dummy";
        }
        if ("real".equalsIgnoreCase(provider)
                || "reality_defender".equalsIgnoreCase(provider)
                || root.has("resultsSummary")
                || hasText(root, "/requestId")
                || hasText(root, "/mediaId")
                || hasText(root, "/data/requestId")
                || hasText(root, "/realityResult/resultsSummary/status")
                || hasText(root, "/realityResult/requestId")
                || hasText(root, "/realityResult/mediaId")
                || hasText(root, "/primary/resultsSummary/status")
                || hasText(root, "/primary/requestId")
                || hasText(root, "/primary/mediaId")) {
            return "real";
        }
        if ("imd".equalsIgnoreCase(provider)
                || root.has("heatmap")
                || root.has("regions")
                || root.has("symptoms")) {
            return "imd";
        }
        return "unknown";
    }

    private List<Map<String, Object>> extractRegions(JsonNode root) {
        String[] candidatePointers = {
                "/suspiciousRegions",
                "/regions",
                "/resultsSummary/suspiciousRegions",
                "/resultsSummary/metadata/suspiciousRegions",
                "/results/suspiciousRegions",
                "/data/suspiciousRegions",
                "/data/regions",
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

    private List<Map<String, Object>> extractSymptoms(JsonNode root) {
        JsonNode arrayNode = firstNode(root, "/symptoms", "/anomalies", "/data/symptoms", "/data/anomalies");
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }

        List<Map<String, Object>> symptoms = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            String label = firstText(node, "/label", "/name", "/type", "/title");
            String severity = firstText(node, "/severity", "/level");
            String description = firstText(node, "/description", "/message", "/reason");

            if ((label == null || label.isBlank()) && (description == null || description.isBlank())) {
                continue;
            }

            Map<String, Object> symptom = new LinkedHashMap<>();
            symptom.put("label", label == null ? "이상 신호" : label);
            if (severity != null && !severity.isBlank()) {
                symptom.put("severity", severity);
            }
            if (description != null && !description.isBlank()) {
                symptom.put("description", description);
            }
            symptoms.add(symptom);
        }
        return symptoms;
    }

    private Map<String, Object> toRegion(JsonNode node) {
        double x = firstNumber(node, "/x", "/left", "/bbox/x", "/box/x");
        double y = firstNumber(node, "/y", "/top", "/bbox/y", "/box/y");
        double width = firstNumber(node, "/width", "/w", "/bbox/width", "/box/width");
        double height = firstNumber(node, "/height", "/h", "/bbox/height", "/box/height");

        if (width <= 0 || height <= 0) {
            return null;
        }

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

    private JsonNode firstNode(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            JsonNode found = node.at(pointer);
            if (!found.isMissingNode() && !found.isNull()) {
                return found;
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

    private boolean isNotApplicableVerdict(String verdict) {
        return "NOT_APPLICABLE".equals(verdict) || "UNABLE_TO_EVALUATE".equals(verdict);
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
}

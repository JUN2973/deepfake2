package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.AiAnalysisRequestDTO;
import kopo.poly.dto.AiAnalysisResponseDTO;
import kopo.poly.dto.AiImageVerificationResponseDTO;
import kopo.poly.dto.ReportPdfRequestDTO;
import kopo.poly.dto.ReportPdfResponseDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IReportPdfMapper;
import kopo.poly.mapper.IVerifyMapper;
import kopo.poly.service.IAiAnalysisService;
import kopo.poly.service.IAiImageVerificationService;
import kopo.poly.service.IObjectStorageService;
import kopo.poly.service.IReportPdfService;
import kopo.poly.service.ReportPdfServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 검증 결과를 신고용 스냅샷으로 저장하고 요청 시 PDF 문서로 생성한다.
 */
@Service
public class ReportPdfService implements IReportPdfService {

    private static final Logger log = LoggerFactory.getLogger(ReportPdfService.class);
    private static final int MAX_REASON_LENGTH = 2_000;
    private static final int MAX_SOURCE_URL_LENGTH = 1_000;
    private static final int MAX_INLINE_IMAGE_BYTES = 16 * 1024 * 1024;
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final IReportPdfMapper reportPdfMapper;
    private final IVerifyMapper verifyMapper;
    private final IAiAnalysisService aiAnalysisService;
    private final IAiImageVerificationService aiImageVerificationService;
    private final IObjectStorageService objectStorageService;
    private final ObjectMapper objectMapper;
    private final ReportPdfGenerator pdfGenerator;

    public ReportPdfService(IReportPdfMapper reportPdfMapper,
                            IVerifyMapper verifyMapper,
                            IAiAnalysisService aiAnalysisService,
                            IAiImageVerificationService aiImageVerificationService,
                            IObjectStorageService objectStorageService,
                            ObjectMapper objectMapper,
                            ReportPdfGenerator pdfGenerator) {
        this.reportPdfMapper = reportPdfMapper;
        this.verifyMapper = verifyMapper;
        this.aiAnalysisService = aiAnalysisService;
        this.aiImageVerificationService = aiImageVerificationService;
        this.objectStorageService = objectStorageService;
        this.objectMapper = objectMapper;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    @Transactional
    public ReportPdfResponseDTO createReport(Long verificationId,
                                             Long userId,
                                             ReportPdfRequestDTO request) {
        requireUserId(userId);
        if (verificationId == null) {
            throw new ReportPdfServiceException("REPORT-4000", "검증 기록 번호가 필요합니다.");
        }

        String reportReason = normalizeReason(request);
        String sourceUrl = normalizeSourceUrl(request);
        VerifyDTO verification = verifyMapper.selectVerificationByIdAndUserId(verificationId, userId);
        if (verification == null) {
            throw new ReportPdfServiceException("REPORT-4040", "신고자료를 만들 검증 기록을 찾을 수 없습니다.");
        }

        boolean includeOriginalImage = defaultTrue(request == null ? null : request.getIncludeOriginalImage());
        boolean includeHeatmap = defaultTrue(request == null ? null : request.getIncludeHeatmap());
        boolean includeAiReportDraft = Boolean.TRUE.equals(
                request == null ? null : request.getIncludeAiReportDraft()
        );

        String reportDraft = "";
        if (includeAiReportDraft) {
            reportDraft = createAiReportDraft(verification, reportReason);
        }

        ReportPdfResponseDTO saved = new ReportPdfResponseDTO();
        saved.setVerificationId(verificationId);
        saved.setUserId(userId);
        saved.setReportReason(reportReason);
        saved.setSourceUrl(sourceUrl);
        saved.setReportDraft(reportDraft);
        saved.setOriginalImageIncluded(includeOriginalImage);
        saved.setHeatmapIncluded(includeHeatmap);
        saved.setAiReportDraftIncluded(includeAiReportDraft);
        saved.setOriginalName(verification.getOriginalName());
        saved.setVerdict(verification.getVerdict());
        saved.setScore(verification.getScore());
        saved.setFileName(createFileName(verificationId));
        saved.setContentType(ReportPdfResponseDTO.CONTENT_TYPE_PDF);
        saved.setStatus(ReportPdfResponseDTO.STATUS_READY);

        int inserted = reportPdfMapper.insertReport(saved);
        if (inserted != 1 || saved.getId() == null) {
            throw new ReportPdfServiceException("REPORT-SAVE", "신고자료 생성 이력을 저장하지 못했습니다.");
        }

        ReportPdfResponseDTO result = reportPdfMapper.selectByIdAndUserId(saved.getId(), userId);
        if (result == null) {
            result = saved;
        }
        applyDownloadUrl(result);
        return result;
    }

    @Override
    public ReportPdfResponseDTO getReport(Long id, Long userId) {
        requireUserId(userId);
        if (id == null) {
            throw new ReportPdfServiceException("REPORT-4000", "신고자료 번호가 필요합니다.");
        }
        ReportPdfResponseDTO report = reportPdfMapper.selectByIdAndUserId(id, userId);
        if (report == null) {
            throw new ReportPdfServiceException("REPORT-4041", "신고자료를 찾을 수 없습니다.");
        }
        applyDownloadUrl(report);
        return report;
    }

    @Override
    public List<ReportPdfResponseDTO> getReports(Long userId) {
        requireUserId(userId);
        List<ReportPdfResponseDTO> reports = reportPdfMapper.selectListByUserId(userId);
        if (reports == null) {
            return List.of();
        }
        reports.forEach(this::applyDownloadUrl);
        return reports;
    }

    @Override
    public byte[] generatePdf(Long id, Long userId) {
        ReportPdfResponseDTO report = getReport(id, userId);
        VerifyDTO verification = verifyMapper.selectVerificationByIdAndUserId(report.getVerificationId(), userId);
        if (verification == null) {
            verification = snapshotVerification(report);
        }

        AiAnalysisResponseDTO explanation = aiAnalysisService.getLatest(report.getVerificationId());
        AiImageVerificationResponseDTO imageReview = aiImageVerificationService.getLatest(report.getVerificationId());
        byte[] originalImage = Boolean.TRUE.equals(report.getOriginalImageIncluded())
                ? readOriginalImage(verification)
                : null;
        byte[] heatmapImage = Boolean.TRUE.equals(report.getHeatmapIncluded())
                ? readHeatmapImage(verification)
                : null;

        try {
            byte[] pdf = pdfGenerator.generate(
                    report,
                    verification,
                    explanation,
                    imageReview,
                    originalImage,
                    heatmapImage
            );
            log.info("Generated report PDF. reportId={} verificationId={} bytes={}",
                    report.getId(), report.getVerificationId(), pdf.length);
            return pdf;
        } catch (ReportPdfServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ReportPdfServiceException("REPORT-PDF", "PDF 문서를 생성하지 못했습니다.", e);
        }
    }

    @Override
    @Transactional
    public void deleteReport(Long id, Long userId) {
        getReport(id, userId);
        if (reportPdfMapper.deleteByIdAndUserId(id, userId) != 1) {
            throw new ReportPdfServiceException("REPORT-DELETE", "신고자료를 삭제하지 못했습니다.");
        }
    }

    private String createAiReportDraft(VerifyDTO verification, String reportReason) {
        AiAnalysisRequestDTO aiRequest = new AiAnalysisRequestDTO();
        aiRequest.setVerificationId(verification.getId());
        aiRequest.setTaskType("CREATE_REPORT_DRAFT");
        aiRequest.setUserQuestion(limit(
                "다음 사용자 신고 사유를 바탕으로 사실 중심의 신고 제출 문구를 작성해 주세요: " + reportReason,
                300
        ));
        aiRequest.setTone("formal and factual");
        aiRequest.setIncludeReportDraft(true);

        try {
            AiAnalysisResponseDTO response = aiAnalysisService.analyze(aiRequest, verification);
            String reportDraft = response == null ? "" : normalizeText(response.getReportDraft());
            if (reportDraft.isBlank()) {
                throw new ReportPdfServiceException("REPORT-AI", "AI 신고 문구가 비어 있습니다.");
            }
            return reportDraft;
        } catch (ReportPdfServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ReportPdfServiceException("REPORT-AI", "AI 신고 문구를 생성하지 못했습니다.", e);
        }
    }

    private byte[] readOriginalImage(VerifyDTO verification) {
        if (verification == null || isBlank(verification.getObjectKey())) {
            return null;
        }
        try {
            byte[] bytes = objectStorageService.readObject(verification.getObjectKey());
            return isUsableImageSize(bytes) ? bytes : null;
        } catch (Exception e) {
            log.warn("Original image unavailable for report. verificationId={}", verification.getId());
            return null;
        }
    }

    private byte[] readHeatmapImage(VerifyDTO verification) {
        if (verification == null) {
            return null;
        }
        byte[] bytes = findInlineHeatmap(verification.getAnalysisJson());
        return bytes != null ? bytes : findInlineHeatmap(verification.getApiRaw());
    }

    private byte[] findInlineHeatmap(String json) {
        if (isBlank(json)) {
            return null;
        }
        try {
            return findInlineHeatmap(objectMapper.readTree(json), false);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] findInlineHeatmap(JsonNode node, boolean heatmapContext) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            var fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                String name = field.getKey().toLowerCase(Locale.ROOT);
                boolean childContext = heatmapContext || name.contains("heatmap") || name.contains("overlay");
                byte[] found = findInlineHeatmap(field.getValue(), childContext);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                byte[] found = findInlineHeatmap(child, heatmapContext);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isTextual() && heatmapContext) {
            return decodeInlineImage(node.asText());
        }
        return null;
    }

    private byte[] decodeInlineImage(String value) {
        if (isBlank(value)) {
            return null;
        }
        String text = value.trim();
        int comma = text.indexOf(',');
        if (text.startsWith("data:image/") && comma >= 0) {
            text = text.substring(comma + 1);
        } else if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("/")) {
            return null;
        }
        if (text.length() > MAX_INLINE_IMAGE_BYTES * 2) {
            return null;
        }
        try {
            byte[] decoded = Base64.getMimeDecoder().decode(text.getBytes(StandardCharsets.US_ASCII));
            return isUsableImageSize(decoded) ? decoded : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private VerifyDTO snapshotVerification(ReportPdfResponseDTO report) {
        VerifyDTO verification = new VerifyDTO();
        verification.setId(report.getVerificationId());
        verification.setUserId(report.getUserId());
        verification.setOriginalName(report.getOriginalName());
        verification.setVerdict(report.getVerdict());
        verification.setScore(report.getScore());
        return verification;
    }

    private String normalizeReason(ReportPdfRequestDTO request) {
        String reason = request == null ? "" : normalizeText(request.getReportReason());
        if (reason.isBlank()) {
            throw new ReportPdfServiceException("REPORT-REASON", "신고 사유를 입력해 주세요.");
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new ReportPdfServiceException("REPORT-REASON", "신고 사유는 2,000자 이내로 입력해 주세요.");
        }
        return reason;
    }

    private String normalizeSourceUrl(ReportPdfRequestDTO request) {
        String value = request == null ? "" : normalizeText(request.getSourceUrl());
        if (value.isBlank()) {
            return null;
        }
        if (value.length() > MAX_SOURCE_URL_LENGTH) {
            throw new ReportPdfServiceException("REPORT-URL", "출처 URL은 1,000자 이내로 입력해 주세요.");
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Unsupported URL");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException e) {
            throw new ReportPdfServiceException("REPORT-URL", "출처 URL은 올바른 HTTP 또는 HTTPS 주소여야 합니다.");
        }
    }

    private void applyDownloadUrl(ReportPdfResponseDTO report) {
        if (report != null && report.getId() != null) {
            report.setDownloadUrl("/api/v1/reports/" + report.getId() + "/download");
        }
    }

    private String createFileName(Long verificationId) {
        String timestamp = LocalDateTime.now().format(FILE_TIME_FORMAT);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "deepscan-report-v" + verificationId + "-" + timestamp + "-" + suffix + ".pdf";
    }

    private boolean defaultTrue(Boolean value) {
        return value == null || value;
    }

    private boolean isUsableImageSize(byte[] bytes) {
        return bytes != null && bytes.length > 0 && bytes.length <= MAX_INLINE_IMAGE_BYTES;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new ReportPdfServiceException("REPORT-AUTH", "로그인이 필요합니다.");
        }
    }
}

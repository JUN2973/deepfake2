package kopo.poly.dto;

/**
 * 생성된 신고 제출용 PDF의 분석 요약과 다운로드 정보를 전달하는 DTO다.
 */
public class ReportPdfResponseDTO {

    public static final String STATUS_READY = "READY";
    public static final String STATUS_FAILED = "FAILED";
    public static final String CONTENT_TYPE_PDF = "application/pdf";

    private Long id;
    private Long verificationId;
    private Long userId;
    private String reportReason;
    private String sourceUrl;
    private String reportDraft;
    private Boolean originalImageIncluded;
    private Boolean heatmapIncluded;
    private Boolean aiReportDraftIncluded;
    private String originalName;
    private String verdict;
    private Double score;
    private String fileName;
    private String contentType;
    private String downloadUrl;
    private String status;
    private String regDt;

    public ReportPdfResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(Long verificationId) {
        this.verificationId = verificationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReportReason() {
        return reportReason;
    }

    public void setReportReason(String reportReason) {
        this.reportReason = reportReason;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getReportDraft() {
        return reportDraft;
    }

    public void setReportDraft(String reportDraft) {
        this.reportDraft = reportDraft;
    }

    public Boolean getOriginalImageIncluded() {
        return originalImageIncluded;
    }

    public void setOriginalImageIncluded(Boolean originalImageIncluded) {
        this.originalImageIncluded = originalImageIncluded;
    }

    public Boolean getHeatmapIncluded() {
        return heatmapIncluded;
    }

    public void setHeatmapIncluded(Boolean heatmapIncluded) {
        this.heatmapIncluded = heatmapIncluded;
    }

    public Boolean getAiReportDraftIncluded() {
        return aiReportDraftIncluded;
    }

    public void setAiReportDraftIncluded(Boolean aiReportDraftIncluded) {
        this.aiReportDraftIncluded = aiReportDraftIncluded;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegDt() {
        return regDt;
    }

    public void setRegDt(String regDt) {
        this.regDt = regDt;
    }
}

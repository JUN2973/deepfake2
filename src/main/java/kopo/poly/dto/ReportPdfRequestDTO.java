package kopo.poly.dto;

/**
 * 사용자가 특정 이미지 검증 결과를 신고 제출용 PDF로 생성할 때 사용하는 DTO다.
 * 검증 번호와 사용자 번호는 URL 및 로그인 세션에서 결정한다.
 */
public class ReportPdfRequestDTO {

    private String reportReason;
    private String sourceUrl;
    private Boolean includeOriginalImage;
    private Boolean includeHeatmap;
    private Boolean includeAiReportDraft;

    public ReportPdfRequestDTO() {
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

    public Boolean getIncludeOriginalImage() {
        return includeOriginalImage;
    }

    public void setIncludeOriginalImage(Boolean includeOriginalImage) {
        this.includeOriginalImage = includeOriginalImage;
    }

    public Boolean getIncludeHeatmap() {
        return includeHeatmap;
    }

    public void setIncludeHeatmap(Boolean includeHeatmap) {
        this.includeHeatmap = includeHeatmap;
    }

    public Boolean getIncludeAiReportDraft() {
        return includeAiReportDraft;
    }

    public void setIncludeAiReportDraft(Boolean includeAiReportDraft) {
        this.includeAiReportDraft = includeAiReportDraft;
    }
}

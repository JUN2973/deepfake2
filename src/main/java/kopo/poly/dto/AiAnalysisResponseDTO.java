package kopo.poly.dto;

public class AiAnalysisResponseDTO {
    private Long verificationId;
    private String summary;
    private String explanation;
    private String reportDraft;
    private String actionGuide;
    private String riskLevel;
    private String disclaimer;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Boolean cached;

    public AiAnalysisResponseDTO() {
    }

    public AiAnalysisResponseDTO(String summary, String explanation, String reportDraft, String actionGuide, String model) {
        this.summary = summary;
        this.explanation = explanation;
        this.reportDraft = reportDraft;
        this.actionGuide = actionGuide;
        this.model = model;
    }

    public Long getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(Long verificationId) {
        this.verificationId = verificationId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getReportDraft() {
        return reportDraft;
    }

    public void setReportDraft(String reportDraft) {
        this.reportDraft = reportDraft;
    }

    public String getActionGuide() {
        return actionGuide;
    }

    public void setActionGuide(String actionGuide) {
        this.actionGuide = actionGuide;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Boolean getCached() {
        return cached;
    }

    public void setCached(Boolean cached) {
        this.cached = cached;
    }
}

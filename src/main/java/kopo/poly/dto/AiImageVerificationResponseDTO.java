package kopo.poly.dto;

import java.util.ArrayList;
import java.util.List;

public class AiImageVerificationResponseDTO {
    private Long verificationId;
    private String visualAssessment;
    private String confidenceLevel;
    private String visualSummary;
    private List<String> visualIndicators = new ArrayList<>();
    private String limitations;
    private String crossCheckStatus;
    private String combinedConclusion;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Boolean cached;

    public Long getVerificationId() { return verificationId; }
    public void setVerificationId(Long verificationId) { this.verificationId = verificationId; }
    public String getVisualAssessment() { return visualAssessment; }
    public void setVisualAssessment(String visualAssessment) { this.visualAssessment = visualAssessment; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public String getVisualSummary() { return visualSummary; }
    public void setVisualSummary(String visualSummary) { this.visualSummary = visualSummary; }
    public List<String> getVisualIndicators() { return visualIndicators; }
    public void setVisualIndicators(List<String> visualIndicators) {
        this.visualIndicators = visualIndicators == null ? new ArrayList<>() : visualIndicators;
    }
    public String getLimitations() { return limitations; }
    public void setLimitations(String limitations) { this.limitations = limitations; }
    public String getCrossCheckStatus() { return crossCheckStatus; }
    public void setCrossCheckStatus(String crossCheckStatus) { this.crossCheckStatus = crossCheckStatus; }
    public String getCombinedConclusion() { return combinedConclusion; }
    public void setCombinedConclusion(String combinedConclusion) { this.combinedConclusion = combinedConclusion; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public Boolean getCached() { return cached; }
    public void setCached(Boolean cached) { this.cached = cached; }
}

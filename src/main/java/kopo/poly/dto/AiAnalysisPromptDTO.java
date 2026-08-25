package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiAnalysisPromptDTO {
    private Long verificationId;
    private String originalName;
    private String verdict;
    private Double score;
    private Integer confidencePercent;
    private String apiProvider;
    private String apiRaw;
    private String analysisJson;
    private String regDt;

    public AiAnalysisPromptDTO() {
    }

    public Long getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(Long verificationId) {
        this.verificationId = verificationId;
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

    public Integer getConfidencePercent() {
        return confidencePercent;
    }

    public void setConfidencePercent(Integer confidencePercent) {
        this.confidencePercent = confidencePercent;
    }

    public String getApiProvider() {
        return apiProvider;
    }

    public void setApiProvider(String apiProvider) {
        this.apiProvider = apiProvider;
    }

    public String getApiRaw() {
        return apiRaw;
    }

    public void setApiRaw(String apiRaw) {
        this.apiRaw = apiRaw;
    }

    public String getAnalysisJson() {
        return analysisJson;
    }

    public void setAnalysisJson(String analysisJson) {
        this.analysisJson = analysisJson;
    }

    public String getRegDt() {
        return regDt;
    }

    public void setRegDt(String regDt) {
        this.regDt = regDt;
    }
}

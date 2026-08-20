package kopo.poly.dto;

public class AiAnalysisRequestDTO {
    private Long verificationId;
    private String taskType;
    private String userQuestion;
    private String tone;
    private Boolean includeReportDraft;

    public AiAnalysisRequestDTO() {
    }

    public Long getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(Long verificationId) {
        this.verificationId = verificationId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getUserQuestion() {
        return userQuestion;
    }

    public void setUserQuestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public Boolean getIncludeReportDraft() {
        return includeReportDraft;
    }

    public void setIncludeReportDraft(Boolean includeReportDraft) {
        this.includeReportDraft = includeReportDraft;
    }
}

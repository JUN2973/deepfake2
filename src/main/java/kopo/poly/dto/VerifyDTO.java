package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별/결과 상세/검증기록): 분석 결과와 검증기록 화면 데이터를 전달한다.
 */
/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class VerifyDTO {
    private Long id;
    private Long userId;

    private String originalName;
    private String mimeType;
    private Long fileSize;

    private String objectKey;
    private String publicUrl;

    private String verdict;
    private Double score;

    private String apiProvider;
    private String apiRaw;
    private String analysisJson;

    private String regDt;

    // JSP, MyBatis, JSON 변환에서 사용하는 기본 getter/setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getApiProvider() { return apiProvider; }
    public void setApiProvider(String apiProvider) { this.apiProvider = apiProvider; }

    public String getApiRaw() { return apiRaw; }
    public void setApiRaw(String apiRaw) { this.apiRaw = apiRaw; }

    public String getAnalysisJson() { return analysisJson; }
    public void setAnalysisJson(String analysisJson) { this.analysisJson = analysisJson; }

    public String getRegDt() { return regDt; }
    public void setRegDt(String regDt) { this.regDt = regDt; }
}

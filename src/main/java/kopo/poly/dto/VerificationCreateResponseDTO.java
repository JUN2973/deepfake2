package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별/결과 상세/검증기록): 분석 결과와 검증기록 화면 데이터를 전달한다.
 */
/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class VerificationCreateResponseDTO {
    private Long id;
    private String originalName;
    private String objectKey;
    private String publicUrl;

    private String verdict;
    private Double score;

    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
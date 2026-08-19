package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별/결과 상세/검증기록): 분석 결과와 검증기록 화면 데이터를 전달한다.
 */
/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class DetailDTO {
    private Long id;
    private String originalName;
    private String savedPath;
    private String verdict;
    private Double score;
    private String apiRaw;
    private String regDt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getSavedPath() { return savedPath; }
    public void setSavedPath(String savedPath) { this.savedPath = savedPath; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getApiRaw() { return apiRaw; }
    public void setApiRaw(String apiRaw) { this.apiRaw = apiRaw; }

    public String getRegDt() { return regDt; }
    public void setRegDt(String regDt) { this.regDt = regDt; }
}
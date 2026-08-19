package kopo.poly.dto;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별/결과 상세/검증기록): 분석 결과와 검증기록 화면 데이터를 전달한다.
 */
/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class DeepfakeResultDTO {
    private String verdict; // 분석 판정값: REAL/FAKE/UNKNOWN
    private Double score;
    private String raw;

    public DeepfakeResultDTO() {}
    public DeepfakeResultDTO(String verdict, Double score, String raw) {
        this.verdict = verdict;
        this.score = score;
        this.raw = raw;
    }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }
}

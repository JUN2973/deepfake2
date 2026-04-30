package kopo.poly.dto;

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

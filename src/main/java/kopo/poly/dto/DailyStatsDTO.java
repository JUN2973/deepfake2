package kopo.poly.dto;

/**
 * 검증 통계 대시보드의 날짜별 차트 데이터를 전달하는 DTO다.
 */
public class DailyStatsDTO {

    private String statisticsDate;
    private Long totalCount;
    private Long aiDetectionCount;
    private Long realCount;
    private Long unknownCount;
    private Double averageConfidence;
    private Long reviewRequestCount;

    public DailyStatsDTO() {
    }

    public String getStatisticsDate() {
        return statisticsDate;
    }

    public void setStatisticsDate(String statisticsDate) {
        this.statisticsDate = statisticsDate;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getAiDetectionCount() {
        return aiDetectionCount;
    }

    public void setAiDetectionCount(Long aiDetectionCount) {
        this.aiDetectionCount = aiDetectionCount;
    }

    public Long getRealCount() {
        return realCount;
    }

    public void setRealCount(Long realCount) {
        this.realCount = realCount;
    }

    public Long getUnknownCount() {
        return unknownCount;
    }

    public void setUnknownCount(Long unknownCount) {
        this.unknownCount = unknownCount;
    }

    public Double getAverageConfidence() {
        return averageConfidence;
    }

    public void setAverageConfidence(Double averageConfidence) {
        this.averageConfidence = averageConfidence;
    }

    public Long getReviewRequestCount() {
        return reviewRequestCount;
    }

    public void setReviewRequestCount(Long reviewRequestCount) {
        this.reviewRequestCount = reviewRequestCount;
    }
}

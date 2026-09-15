package kopo.poly.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 검증 통계 대시보드의 요약 지표와 날짜별 통계를 전달하는 DTO다.
 */
public class StatsResponseDTO {

    private String startDate;
    private String endDate;
    private Long totalVerificationCount;
    private Long aiDetectionCount;
    private Long realCount;
    private Long unknownCount;
    private Double aiDetectionRatio;
    private Double averageConfidence;
    private Long reviewRequestCount;
    private Double falsePositiveReportRatio;
    private List<DailyStatsDTO> dailyStatistics = new ArrayList<>();

    public StatsResponseDTO() {
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Long getTotalVerificationCount() {
        return totalVerificationCount;
    }

    public void setTotalVerificationCount(Long totalVerificationCount) {
        this.totalVerificationCount = totalVerificationCount;
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

    public Double getAiDetectionRatio() {
        return aiDetectionRatio;
    }

    public void setAiDetectionRatio(Double aiDetectionRatio) {
        this.aiDetectionRatio = aiDetectionRatio;
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

    public Double getFalsePositiveReportRatio() {
        return falsePositiveReportRatio;
    }

    public void setFalsePositiveReportRatio(Double falsePositiveReportRatio) {
        this.falsePositiveReportRatio = falsePositiveReportRatio;
    }

    public List<DailyStatsDTO> getDailyStatistics() {
        return dailyStatistics;
    }

    public void setDailyStatistics(List<DailyStatsDTO> dailyStatistics) {
        this.dailyStatistics = dailyStatistics == null ? new ArrayList<>() : dailyStatistics;
    }
}

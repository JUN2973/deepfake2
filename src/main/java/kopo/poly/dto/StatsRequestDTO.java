package kopo.poly.dto;

/**
 * 검증 통계 조회에 사용할 기간 조건을 전달하는 DTO다.
 */
public class StatsRequestDTO {

    private String startDate;
    private String endDate;

    public StatsRequestDTO() {
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
}

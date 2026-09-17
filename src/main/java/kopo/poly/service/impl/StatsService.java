package kopo.poly.service.impl;

import kopo.poly.dto.DailyStatsDTO;
import kopo.poly.dto.StatsRequestDTO;
import kopo.poly.dto.StatsResponseDTO;
import kopo.poly.mapper.IStatsMapper;
import kopo.poly.service.IStatsService;
import kopo.poly.service.StatsServiceException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 조회 기간을 검증하고 DB 집계 결과를 대시보드 응답 형태로 정리한다.
 */
@Service
public class StatsService implements IStatsService {

    private static final int DEFAULT_PERIOD_DAYS = 7;
    private static final int MAX_PERIOD_DAYS = 366;

    private final IStatsMapper statsMapper;

    public StatsService(IStatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    @Override
    public StatsResponseDTO getStats(StatsRequestDTO request) {
        StatsRequestDTO normalizedRequest = normalizePeriod(request);
        StatsResponseDTO summary = statsMapper.selectSummary(normalizedRequest);
        if (summary == null) {
            summary = new StatsResponseDTO();
        }

        summary.setStartDate(normalizedRequest.getStartDate());
        summary.setEndDate(normalizedRequest.getEndDate());
        normalizeSummary(summary);

        List<DailyStatsDTO> dailyStats = statsMapper.selectDailyStats(normalizedRequest);
        summary.setDailyStatistics(fillMissingDates(normalizedRequest, dailyStats));
        return summary;
    }

    private StatsRequestDTO normalizePeriod(StatsRequestDTO request) {
        String startValue = trimToNull(request == null ? null : request.getStartDate());
        String endValue = trimToNull(request == null ? null : request.getEndDate());

        LocalDate endDate = endValue == null ? LocalDate.now() : parseDate(endValue);
        LocalDate startDate = startValue == null
                ? endDate.minusDays(DEFAULT_PERIOD_DAYS - 1L)
                : parseDate(startValue);

        if (startDate.isAfter(endDate)) {
            throw new StatsServiceException("STATS-PERIOD", "시작일은 종료일보다 늦을 수 없습니다.");
        }

        long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (periodDays > MAX_PERIOD_DAYS) {
            throw new StatsServiceException("STATS-PERIOD", "통계 조회 기간은 최대 366일까지 선택할 수 있습니다.");
        }

        StatsRequestDTO normalized = new StatsRequestDTO();
        normalized.setStartDate(startDate.toString());
        normalized.setEndDate(endDate.toString());
        return normalized;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new StatsServiceException("STATS-DATE", "날짜는 yyyy-MM-dd 형식으로 입력해 주세요.");
        }
    }

    private void normalizeSummary(StatsResponseDTO summary) {
        summary.setTotalVerificationCount(zeroIfNull(summary.getTotalVerificationCount()));
        summary.setAiDetectionCount(zeroIfNull(summary.getAiDetectionCount()));
        summary.setRealCount(zeroIfNull(summary.getRealCount()));
        summary.setUnknownCount(zeroIfNull(summary.getUnknownCount()));
        summary.setReviewRequestCount(zeroIfNull(summary.getReviewRequestCount()));
        summary.setAiDetectionRatio(zeroIfNull(summary.getAiDetectionRatio()));
        summary.setFalsePositiveReportRatio(zeroIfNull(summary.getFalsePositiveReportRatio()));
        summary.setAverageConfidence(toPercent(summary.getAverageConfidence()));
    }

    private List<DailyStatsDTO> fillMissingDates(StatsRequestDTO request, List<DailyStatsDTO> rows) {
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        Map<String, DailyStatsDTO> indexedRows = new LinkedHashMap<>();

        if (rows != null) {
            for (DailyStatsDTO row : rows) {
                if (row != null && trimToNull(row.getStatisticsDate()) != null) {
                    normalizeDailyStats(row);
                    indexedRows.put(row.getStatisticsDate(), row);
                }
            }
        }

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            indexedRows.computeIfAbsent(date.toString(), this::emptyDailyStats);
        }

        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> indexedRows.get(date.toString()))
                .toList();
    }

    private DailyStatsDTO emptyDailyStats(String statisticsDate) {
        DailyStatsDTO result = new DailyStatsDTO();
        result.setStatisticsDate(statisticsDate);
        result.setTotalCount(0L);
        result.setAiDetectionCount(0L);
        result.setRealCount(0L);
        result.setUnknownCount(0L);
        result.setAverageConfidence(0.0);
        result.setReviewRequestCount(0L);
        return result;
    }

    private void normalizeDailyStats(DailyStatsDTO row) {
        row.setTotalCount(zeroIfNull(row.getTotalCount()));
        row.setAiDetectionCount(zeroIfNull(row.getAiDetectionCount()));
        row.setRealCount(zeroIfNull(row.getRealCount()));
        row.setUnknownCount(zeroIfNull(row.getUnknownCount()));
        row.setReviewRequestCount(zeroIfNull(row.getReviewRequestCount()));
        row.setAverageConfidence(toPercent(row.getAverageConfidence()));
    }

    private double toPercent(Double value) {
        if (value == null) {
            return 0.0;
        }
        double percent = value >= 0.0 && value <= 1.0 ? value * 100.0 : value;
        return Math.round(percent * 10.0) / 10.0;
    }

    private long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }

    private double zeroIfNull(Double value) {
        return value == null ? 0.0 : value;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

}

package kopo.poly.service.impl;

import kopo.poly.dto.DailyStatsDTO;
import kopo.poly.dto.StatsRequestDTO;
import kopo.poly.dto.StatsResponseDTO;
import kopo.poly.mapper.IStatsMapper;
import kopo.poly.service.StatsServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatsServiceTest {

    private IStatsMapper statsMapper;
    private StatsService service;

    @BeforeEach
    void setUp() {
        statsMapper = mock(IStatsMapper.class);
        service = new StatsService(statsMapper);
    }

    @Test
    void getStatsCombinesSummaryAndFillsMissingDates() {
        StatsRequestDTO request = period("2026-09-01", "2026-09-03");
        StatsResponseDTO summary = new StatsResponseDTO();
        summary.setTotalVerificationCount(2L);
        summary.setAiDetectionCount(1L);
        summary.setRealCount(1L);
        summary.setAverageConfidence(0.824);
        summary.setAiDetectionRatio(50.0);

        DailyStatsDTO firstDay = new DailyStatsDTO();
        firstDay.setStatisticsDate("2026-09-01");
        firstDay.setTotalCount(2L);
        firstDay.setAiDetectionCount(1L);
        firstDay.setRealCount(1L);
        firstDay.setAverageConfidence(0.824);

        when(statsMapper.selectSummary(eq(3L), any())).thenReturn(summary);
        when(statsMapper.selectDailyStats(eq(3L), any())).thenReturn(List.of(firstDay));

        StatsResponseDTO result = service.getStats(3L, request);

        assertThat(result.getStartDate()).isEqualTo("2026-09-01");
        assertThat(result.getEndDate()).isEqualTo("2026-09-03");
        assertThat(result.getAverageConfidence()).isEqualTo(82.4);
        assertThat(result.getDailyStatistics()).hasSize(3);
        assertThat(result.getDailyStatistics().get(1).getStatisticsDate()).isEqualTo("2026-09-02");
        assertThat(result.getDailyStatistics().get(1).getTotalCount()).isZero();
    }

    @Test
    void getStatsUsesSevenDayDefaultEndingAtRequestedEndDate() {
        StatsRequestDTO request = new StatsRequestDTO();
        request.setEndDate("2026-09-15");
        when(statsMapper.selectSummary(eq(3L), any())).thenReturn(new StatsResponseDTO());
        when(statsMapper.selectDailyStats(eq(3L), any())).thenReturn(List.of());

        service.getStats(3L, request);

        ArgumentCaptor<StatsRequestDTO> captor = ArgumentCaptor.forClass(StatsRequestDTO.class);
        verify(statsMapper).selectSummary(eq(3L), captor.capture());
        assertThat(captor.getValue().getStartDate()).isEqualTo("2026-09-09");
        assertThat(captor.getValue().getEndDate()).isEqualTo("2026-09-15");
    }

    @Test
    void getStatsRejectsInvalidPeriod() {
        StatsRequestDTO request = period("2026-09-15", "2026-09-01");

        assertThatThrownBy(() -> service.getStats(3L, request))
                .isInstanceOf(StatsServiceException.class)
                .extracting(error -> ((StatsServiceException) error).getCode())
                .isEqualTo("STATS-PERIOD");
    }

    @Test
    void getStatsRequiresLogin() {
        assertThatThrownBy(() -> service.getStats(null, period("2026-09-01", "2026-09-03")))
                .isInstanceOf(StatsServiceException.class)
                .extracting(error -> ((StatsServiceException) error).getCode())
                .isEqualTo("STATS-AUTH");
    }

    private StatsRequestDTO period(String startDate, String endDate) {
        StatsRequestDTO request = new StatsRequestDTO();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        return request;
    }
}

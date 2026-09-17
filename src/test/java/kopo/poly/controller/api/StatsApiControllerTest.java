package kopo.poly.controller.api;

import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.StatsRequestDTO;
import kopo.poly.dto.StatsResponseDTO;
import kopo.poly.service.IStatsService;
import kopo.poly.service.StatsServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatsApiControllerTest {

    private IStatsService statsService;
    private StatsApiController controller;

    @BeforeEach
    void setUp() {
        statsService = mock(IStatsService.class);
        controller = new StatsApiController(statsService);
    }

    @Test
    void getStatsUsesLoggedInUserAndPeriodParameters() {
        StatsResponseDTO response = new StatsResponseDTO();
        response.setTotalVerificationCount(25L);
        when(statsService.getStats(org.mockito.ArgumentMatchers.eq(3L), any())).thenReturn(response);

        ApiResponse<StatsResponseDTO> result = controller.getStats(
                "2026-09-01",
                "2026-09-15",
                loggedInSession(3L)
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(response);

        ArgumentCaptor<StatsRequestDTO> captor = ArgumentCaptor.forClass(StatsRequestDTO.class);
        verify(statsService).getStats(org.mockito.ArgumentMatchers.eq(3L), captor.capture());
        assertThat(captor.getValue().getStartDate()).isEqualTo("2026-09-01");
        assertThat(captor.getValue().getEndDate()).isEqualTo("2026-09-15");
    }

    @Test
    void getStatsAllowsServiceToApplyDefaultPeriod() {
        when(statsService.getStats(org.mockito.ArgumentMatchers.eq(3L), any()))
                .thenReturn(new StatsResponseDTO());

        ApiResponse<StatsResponseDTO> result = controller.getStats(null, null, loggedInSession(3L));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<StatsRequestDTO> captor = ArgumentCaptor.forClass(StatsRequestDTO.class);
        verify(statsService).getStats(org.mockito.ArgumentMatchers.eq(3L), captor.capture());
        assertThat(captor.getValue().getStartDate()).isNull();
        assertThat(captor.getValue().getEndDate()).isNull();
    }

    @Test
    void getStatsRejectsAnonymousUserBeforeCallingService() {
        ApiResponse<StatsResponseDTO> result = controller.getStats(
                "2026-09-01",
                "2026-09-15",
                new MockHttpSession()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("STATS-4010");
        verify(statsService, never()).getStats(any(), any());
    }

    @Test
    void getStatsMapsServiceValidationError() {
        when(statsService.getStats(org.mockito.ArgumentMatchers.eq(3L), any())).thenThrow(
                new StatsServiceException("STATS-PERIOD", "시작일은 종료일보다 늦을 수 없습니다.")
        );

        ApiResponse<StatsResponseDTO> result = controller.getStats(
                "2026-09-15",
                "2026-09-01",
                loggedInSession(3L)
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("STATS-PERIOD");
        assertThat(result.getError().getMessage()).contains("시작일");
    }

    private MockHttpSession loggedInSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", userId);
        return session;
    }
}

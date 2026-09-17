package kopo.poly.controller.api;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.StatsRequestDTO;
import kopo.poly.dto.StatsResponseDTO;
import kopo.poly.service.IStatsService;
import kopo.poly.service.StatsServiceException;
import kopo.poly.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 사용자의 기간별 검증 통계 조회 API를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/stats")
public class StatsApiController {

    private static final Logger log = LoggerFactory.getLogger(StatsApiController.class);

    private final IStatsService statsService;

    public StatsApiController(IStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ApiResponse<StatsResponseDTO> getStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("STATS-4010", "로그인 후 검증 통계를 확인할 수 있습니다.");
        }

        StatsRequestDTO request = new StatsRequestDTO();
        request.setStartDate(startDate);
        request.setEndDate(endDate);

        try {
            return ApiResponse.ok(statsService.getStats(userId, request));
        } catch (StatsServiceException e) {
            log.warn("Stats request failed. code={}, userId={}", e.getCode(), userId);
            return ApiResponse.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected stats request error. userId={}", userId, e);
            return ApiResponse.fail("STATS-5000", "검증 통계를 불러오는 중 오류가 발생했습니다.");
        }
    }
}

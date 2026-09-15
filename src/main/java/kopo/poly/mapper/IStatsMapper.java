package kopo.poly.mapper;

import kopo.poly.dto.DailyStatsDTO;
import kopo.poly.dto.StatsRequestDTO;
import kopo.poly.dto.StatsResponseDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 사용자별 검증 통계와 날짜별 차트 데이터를 조회하는 MyBatis 매퍼다.
 */
public interface IStatsMapper {

    StatsResponseDTO selectSummary(
            @Param("userId") Long userId,
            @Param("request") StatsRequestDTO request
    );

    List<DailyStatsDTO> selectDailyStats(
            @Param("userId") Long userId,
            @Param("request") StatsRequestDTO request
    );
}

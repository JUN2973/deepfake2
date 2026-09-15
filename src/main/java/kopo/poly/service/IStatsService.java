package kopo.poly.service;

import kopo.poly.dto.StatsRequestDTO;
import kopo.poly.dto.StatsResponseDTO;

/**
 * 사용자별 검증 통계 조회를 위한 서비스 계약이다.
 */
public interface IStatsService {

    StatsResponseDTO getStats(Long userId, StatsRequestDTO request);
}

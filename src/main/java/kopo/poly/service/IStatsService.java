package kopo.poly.service;

import kopo.poly.dto.StatsRequestDTO;
import kopo.poly.dto.StatsResponseDTO;

/**
 * 전체 검증 통계 조회를 위한 서비스 계약이다.
 */
public interface IStatsService {

    StatsResponseDTO getStats(StatsRequestDTO request);
}

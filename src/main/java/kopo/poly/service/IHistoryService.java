package kopo.poly.service;

import java.util.List;
import kopo.poly.dto.HistoryDTO;

/**
 * 사용자 분석 이력 조회 서비스 계약을 정의한다.
 */
public interface IHistoryService {
    List<HistoryDTO> getHistory(Long userId);
}

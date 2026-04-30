package kopo.poly.mapper;

import java.util.List;
import kopo.poly.dto.HistoryDTO;

/**
 * 사용자 분석 이력 조회 SQL을 호출하는 MyBatis 매퍼다.
 */
public interface IHistoryMapper {
    List<HistoryDTO> selectHistoryByUserId(Long userId);
}

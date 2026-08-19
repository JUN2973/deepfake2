package kopo.poly.mapper;


/**
 * 체크리스트 기준 주석: 테이블 명세서(RDBMS)/구현(검증기록): 검증기록 목록/상세 조회 SQL을 정의한다.
 */
import java.util.List;
import kopo.poly.dto.HistoryDTO;

/**
 * 사용자 분석 이력 조회 SQL을 호출하는 MyBatis 매퍼다.
 */
public interface IHistoryMapper {
    List<HistoryDTO> selectHistoryByUserId(Long userId);
}

package kopo.poly.service;


/**
 * 체크리스트 기준 주석: 설계/API 연동 설계: 컨트롤러와 구현체 사이의 서비스 계약을 정의한다.
 */
import java.util.List;
import kopo.poly.dto.HistoryDTO;

/**
 * 사용자 분석 이력 조회 서비스 계약을 정의한다.
 */
public interface IHistoryService {
    List<HistoryDTO> getHistory(Long userId);
}

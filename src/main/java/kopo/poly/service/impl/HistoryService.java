package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(검증기록): 검증기록 목록과 상세 데이터를 조회한다.
 */
import java.util.List;
import kopo.poly.dto.HistoryDTO;
import kopo.poly.mapper.IHistoryMapper;
import kopo.poly.service.IHistoryService;
import org.springframework.stereotype.Service;

/**
 * 사용자별 이미지 분석 이력을 조회하는 서비스 구현체다.
 */
@Service("HistoryService")
public class HistoryService implements IHistoryService {

    private final IHistoryMapper historyMapper;

    public HistoryService(IHistoryMapper historyMapper) {
        this.historyMapper = historyMapper;
    }

    @Override
    public List<HistoryDTO> getHistory(Long userId) {
        // 비로그인 사용자는 개인 분석 이력을 조회하지 않는다.
        if (userId == null) {
            return List.of();
        }

        return historyMapper.selectHistoryByUserId(userId);
    }
}

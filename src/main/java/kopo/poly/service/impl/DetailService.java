package kopo.poly.service.impl;


/**
 * 체크리스트 기준 주석: 구현(결과 상세): 분석 결과 상세 데이터와 의심 영역 표시 데이터를 구성한다.
 */
import kopo.poly.dto.DetailDTO;
import kopo.poly.mapper.IDetailMapper;
import kopo.poly.service.IDetailService;
import org.springframework.stereotype.Service;

/**
 * 분석 상세 화면에 필요한 검증 결과를 조회한다.
 */
@Service("DetailService")
public class DetailService implements IDetailService {

    private final IDetailMapper detailMapper;

    public DetailService(IDetailMapper detailMapper) {
        this.detailMapper = detailMapper;
    }

    @Override
    public DetailDTO getDetail(Long id) {
        // 상세 화면에서 사용할 검증 결과 한 건을 id 기준으로 조회한다.
        return detailMapper.selectDetail(id);
    }
}

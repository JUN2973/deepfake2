package kopo.poly.service.impl;

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
        return detailMapper.selectDetail(id);
    }
}
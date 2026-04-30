package kopo.poly.mapper;

import kopo.poly.dto.DetailDTO;

/**
 * 상세 화면에 필요한 검증 결과 SQL을 호출하는 MyBatis 매퍼다.
 */
public interface IDetailMapper {
    DetailDTO selectDetail(Long id);
}
package kopo.poly.mapper;


/**
 * 체크리스트 기준 주석: 테이블 명세서(RDBMS)/구현(결과 상세): 분석 상세 결과 조회 SQL을 정의한다.
 */
import kopo.poly.dto.DetailDTO;

/**
 * 상세 화면에 필요한 검증 결과 SQL을 호출하는 MyBatis 매퍼다.
 */
public interface IDetailMapper {
    DetailDTO selectDetail(Long id);
}
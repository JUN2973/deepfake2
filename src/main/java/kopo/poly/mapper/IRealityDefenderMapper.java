package kopo.poly.mapper;


/**
 * 체크리스트 기준 주석: 테이블 명세서(RDBMS)/API 연동 설계: 외부 분석 API 결과 저장/조회 SQL을 정의한다.
 */
import kopo.poly.dto.VerificationRecordDTO;

import java.util.List;

/**
 * Reality Defender 분석 결과 저장과 조회 SQL을 호출하는 매퍼다.
 */
public interface IRealityDefenderMapper {

    int insertVerificationRecord(VerificationRecordDTO pDTO);

    VerificationRecordDTO selectVerificationRecordById(Long id);

    List<VerificationRecordDTO> selectVerificationRecordList(Long userId);
}

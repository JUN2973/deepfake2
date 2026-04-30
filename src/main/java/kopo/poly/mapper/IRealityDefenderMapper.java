package kopo.poly.mapper;

import kopo.poly.dto.VerificationRecordDto;

import java.util.List;

/**
 * Reality Defender 분석 결과 저장과 조회 SQL을 호출하는 매퍼다.
 */
public interface IRealityDefenderMapper {

    int insertVerificationRecord(VerificationRecordDto pDTO);

    VerificationRecordDto selectVerificationRecordById(Long id);

    List<VerificationRecordDto> selectVerificationRecordList(Long userId);
}

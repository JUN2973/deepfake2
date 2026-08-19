package kopo.poly.mapper;


/**
 * 체크리스트 기준 주석: 테이블 명세서(RDBMS)/구현(딥페이크 판별): 검증 결과와 파일 메타데이터 접근 SQL을 정의한다.
 */
import kopo.poly.dto.VerifyDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 업로드 검증 결과 저장, 조회, 삭제 SQL을 호출하는 MyBatis 매퍼다.
 */
public interface IVerifyMapper {
    int insertVerification(VerifyDTO pDTO);
    VerifyDTO selectVerification(@Param("id") Long id);
    VerifyDTO selectVerificationByIdAndUserId(@Param("id") Long id,
                                              @Param("userId") Long userId);
    List<VerifyDTO> selectVerificationList(@Param("offset") int offset,
                                           @Param("size") int size,
                                           @Param("verdict") String verdict,
                                           @Param("userId") Long userId);
    int deleteVerificationByIdAndUserId(@Param("id") Long id,
                                        @Param("userId") Long userId);
}

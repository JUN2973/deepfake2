package kopo.poly.mapper;

import kopo.poly.entity.EmailAuthEntity;
import org.apache.ibatis.annotations.Param;

/**
 * 이메일 인증번호 저장과 검증 상태 변경 SQL을 호출하는 매퍼다.
 */
public interface IEmailAuthMapper {

    int insertEmailAuth(EmailAuthEntity pDTO);

    EmailAuthEntity selectLatestByEmail(@Param("email") String email);

    int expireAllByEmail(@Param("email") String email);

    int markVerified(@Param("id") Long id);
}

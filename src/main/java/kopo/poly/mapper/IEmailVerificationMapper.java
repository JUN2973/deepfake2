package kopo.poly.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 이메일 발송/인증 관련 SQL을 호출하는 MyBatis 매퍼다.
 */
public interface IEmailVerificationMapper {
    int upsertCode(@Param("email") String email,
                   @Param("codeHash") String codeHash,
                   @Param("expiresAt") String expiresAt);

    Map<String, Object> selectByEmail(@Param("email") String email);

    int increaseAttempt(@Param("email") String email);

    int markVerified(@Param("email") String email);

    int deleteByEmail(@Param("email") String email);
}

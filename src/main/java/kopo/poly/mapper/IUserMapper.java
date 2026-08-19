package kopo.poly.mapper;



/**
 * 체크리스트 기준 주석: 테이블 명세서(RDBMS)/구현(인증/회원): 회원, 로그인, 이메일 인증 데이터 접근 SQL을 정의한다.
 */
import kopo.poly.dto.UserCreateDTO;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

// AuthApiController와 OAuth 서비스가 사용하는 사용자 DB 접근 인터페이스다.
// 각 메서드명은 UserMapper.xml의 select/insert/update/delete id와 1:1로 연결된다.
/**
 * 회원 가입, 로그인, 계정 찾기, 마이페이지 SQL을 호출하는 MyBatis 매퍼다.
 */
public interface IUserMapper {
    // 회원가입 이메일 중복확인: 같은 이메일이 app_user에 존재하면 1 이상을 반환한다.
    Integer existsByEmail(@Param("email") String email);

    // 일반 회원가입 저장: 서비스에서 저장용 DTO로 변환한 값만 전달한다.
    int insertUser(UserCreateDTO dto);

    // Google OAuth 사용자 저장: OAuth 제공자와 제공자별 ID를 같이 저장한다.
    int insertSocialUser(@Param("name") String name,
                         @Param("email") String email,
                         @Param("passwordHash") String passwordHash,
                         @Param("phoneNumber") String phoneNumber,
                         @Param("address") String address,
                         @Param("oauthProvider") String oauthProvider,
                         @Param("oauthProviderId") String oauthProviderId);

    // 로그인에서 이메일로 사용자와 비밀번호 해시를 조회한다.
    Map<String, Object> selectUserByEmail(@Param("email") String email);

    // 세션 USER_ID 기준으로 현재 사용자를 조회한다.
    Map<String, Object> selectUserById(@Param("userId") Long userId);

    // OAuth 제공자 정보로 소셜 로그인 사용자를 조회한다.
    Map<String, Object> selectUserByOauth(@Param("oauthProvider") String oauthProvider,
                                          @Param("oauthProviderId") String oauthProviderId);

    // 아이디 찾기에서 이름과 전화번호가 일치하는 사용자를 찾는다.
    Map<String, Object> selectUserByNameAndPhone(@Param("name") String name,
                                                 @Param("phoneNumber") String phoneNumber);

    // 비밀번호 재설정 시작 전에 이메일과 이름이 일치하는 사용자를 찾는다.
    Map<String, Object> selectUserByEmailAndName(@Param("email") String email,
                                                 @Param("name") String name);

    // 기존 이메일 계정에 OAuth 정보를 연결하거나 갱신한다.
    int updateOauthByEmail(@Param("email") String email,
                           @Param("name") String name,
                           @Param("oauthProvider") String oauthProvider,
                           @Param("oauthProviderId") String oauthProviderId);

    // 비밀번호 찾기/재설정에서 새 해시 비밀번호를 저장한다.
    int updatePasswordByEmail(@Param("email") String email,
                              @Param("passwordHash") String passwordHash);

    // 마이페이지 프로필 수정에서 이름, 전화번호, 주소를 갱신한다.
    int updateProfileById(@Param("userId") Long userId,
                          @Param("name") String name,
                          @Param("phoneNumber") String phoneNumber,
                          @Param("address") String address);

    // 로그인 상태의 비밀번호 변경에서 새 해시 비밀번호를 저장한다.
    int updatePasswordById(@Param("userId") Long userId,
                           @Param("passwordHash") String passwordHash);

    // 회원탈퇴 시 사용자가 누른 댓글 좋아요를 삭제한다.
    int deleteCommentLikesByUserId(@Param("userId") Long userId);

    // 회원탈퇴 시 사용자가 누른 게시글 좋아요를 삭제한다.
    int deletePostLikesByUserId(@Param("userId") Long userId);

    // 회원탈퇴 시 사용자가 작성한 댓글을 삭제한다.
    int deleteCommentsByUserId(@Param("userId") Long userId);

    // 회원탈퇴 시 탈퇴 사용자의 게시글에 달린 좋아요를 삭제한다.
    int deletePostLikesByPostOwnerId(@Param("userId") Long userId);

    // 회원탈퇴 시 사용자가 작성한 게시글을 삭제한다.
    int deletePostsByUserId(@Param("userId") Long userId);

    // 회원탈퇴 시 user_id로 연결된 검증 데이터를 삭제한다.
    int deleteVerificationsByUserId(@Param("userId") Long userId);

    // 회원탈퇴 시 user_id로 연결된 검증 이력 데이터를 삭제한다.
    int deleteVerificationRecordsByUserId(@Param("userId") Long userId);

    // 회원탈퇴 마지막 단계에서 app_user 본문을 삭제한다.
    int deleteUserById(@Param("userId") Long userId);
}

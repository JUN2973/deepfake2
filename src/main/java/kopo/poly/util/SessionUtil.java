package kopo.poly.util;


/**
 * 체크리스트 기준 주석: 구현(인증/회원): 로그인 세션과 최근 검증 ID 관리를 제공한다.
 */

/**
 * 발표용 설명: JSP와 API에서 공통으로 사용하는 로그인 세션 유틸입니다.
 * 사용자 ID 조회, 최근 검증 ID 저장, 검증기록 접근 권한 확인을 한 곳에서 처리합니다.
 */
import jakarta.servlet.http.HttpSession;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 컨트롤러에서 반복되는 로그인 세션 값 변환을 담당한다.
 */
public final class SessionUtil {

    private static final String OWNED_VERIFICATION_IDS = "OWNED_VERIFICATION_IDS";

    private SessionUtil() {
    }

    public static Long getUserId(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (userId == null) {
            return null;
        }
//long - 세션에서 USER_ID를 가져와서 Long 타입으로 돌려주고, 로그인 정보가 없으면 null을 돌려주는 메서드

        if (userId instanceof Number number) {
            return number.longValue();
        }
//
        try {
            return Long.parseLong(String.valueOf(userId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void rememberVerificationId(HttpSession session, Long verificationId) {
        if (session == null || verificationId == null) {
            return;
        }

        Set<Long> ids = getOwnedVerificationIds(session);
        ids.add(verificationId);
        session.setAttribute(OWNED_VERIFICATION_IDS, ids);
    }
    // Set<Long>을 쓰는이유 - 중복을 허용하지 않는다.

    public static boolean canAccessVerification(HttpSession session, Long ownerUserId, Long verificationId) {
        Long currentUserId = getUserId(session);
        if (currentUserId != null && ownerUserId != null) {
            return currentUserId.equals(ownerUserId);
        }

        return verificationId != null && getOwnedVerificationIds(session).contains(verificationId);
    }

    private static Set<Long> getOwnedVerificationIds(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(OWNED_VERIFICATION_IDS);
        if (value instanceof Set<?> set) {
            Set<Long> ids = new LinkedHashSet<>();
            for (Object item : set) {
                try {
                    ids.add(Long.parseLong(String.valueOf(item)));
                } catch (NumberFormatException ignored) {
                }
            }
            return ids;
        }
        return new LinkedHashSet<>();
    }
}

package kopo.poly.util;


/**
 * 체크리스트 기준 주석: 구현(인증/회원): 비밀번호 등 민감 값 해시 처리를 제공한다.
 */
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 비밀번호와 인증 코드처럼 같은 규칙으로 해시해야 하는 값을 처리한다.
 */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256(String value) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();

        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }
}

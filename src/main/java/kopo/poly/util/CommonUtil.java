package kopo.poly.util;


/**
 * 체크리스트 기준 주석: 설계: 문자열 null 처리 등 공통 유틸리티 기능을 제공한다.
 */
import java.util.Objects;

/**
 * 문자열 null 처리처럼 여러 곳에서 쓰는 공통 유틸리티 클래스다.
 */
public class CommonUtil {

    public static String nvl(String str, String chg_str) {
        return (str == null || str.isEmpty()) ? chg_str : str;
    }

    public static String nvl(String str) {
        return nvl(str, "");
    }

    public static String checked(String str, String com_str) {
        return Objects.equals(str, com_str) ? " checked" : "";
    }

    public static String checked(String[] str, String com_str) {
        if (str == null) return ""; // null 방어

        for (String s : str) {
            if (Objects.equals(s, com_str)) {
                return " checked";
            }
        }
        return "";
    }

    public static String select(String str, String com_str) {
        return Objects.equals(str, com_str) ? " selected" : "";
    }
}
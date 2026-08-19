package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(인증/회원): 로그인, 회원가입, 아이디/비밀번호 찾기, 마이페이지 화면 진입을 담당한다.
 */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 로그인, 회원가입, 계정 찾기 화면을 반환하는 페이지 컨트롤러다.
 */
@Controller
public class AuthPageController {

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/find-account")
    public String findAccount() {
        return "find-account";
    }

    @GetMapping("/mypage")
    public String myPage() {
        return "mypage";
    }
}

package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별): 서비스 메인/이미지 업로드 진입 화면을 담당한다.
 */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 메인 홈 화면을 반환하는 페이지 컨트롤러다.
 */
@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "home";  // 홈 화면 JSP를 반환한다.
    }
}

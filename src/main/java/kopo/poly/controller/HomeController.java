package kopo.poly.controller;

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

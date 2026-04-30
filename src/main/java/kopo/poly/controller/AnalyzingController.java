package kopo.poly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 분석 진행 화면으로 이동시키는 페이지 컨트롤러다.
 */
@Controller
public class AnalyzingController {

    @GetMapping("/analyzing")
    public String analyzing() {
        return "analyzing";
    }
}
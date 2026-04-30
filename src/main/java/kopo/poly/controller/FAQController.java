package kopo.poly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * FAQ 화면을 반환하는 단순 페이지 컨트롤러다.
 */
@Controller
public class FAQController {

    @GetMapping("/faq")
    public String faqPage() {
        return "faq";
    }
}

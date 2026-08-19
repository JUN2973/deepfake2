package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(자주하는질문): FAQ 목록 조회와 질문/답변 펼치기 화면을 담당한다.
 */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * FAQ 화면을 반환하는 단순 페이지 컨트롤러다.
 */
@Controller
public class FaqController {

    @GetMapping("/faq")
    public String faqPage() {
        // FAQ JSP로 이동해 자주 묻는 질문과 답변을 보여준다.
        return "faq";
    }
}

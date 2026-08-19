package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(신고): 신고 페이지 진입과 경찰서/지도 안내 화면을 담당한다.
 */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 신고 안내 및 관련 화면을 반환하는 페이지 컨트롤러다.
 */
@Controller
public class ReportController {

    @GetMapping("/report")
    public String reportPage() {
        return "report";
    }

    @GetMapping("/report/success")
    public String reportSuccessPage() {
        // 별도 success 화면이 없으므로 기존 신고 페이지로 돌려보낸다.
        return "redirect:/report";
    }
}

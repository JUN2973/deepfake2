package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별): 분석 진행 화면 진입과 진행 상태 표시 흐름을 담당한다.
 */
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

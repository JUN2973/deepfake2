package kopo.poly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 분석 결과 상세 화면에 필요한 검증 결과를 조회해 전달한다.
 */
@Controller
public class DetailController {

    @GetMapping("/legacy/detail/{id}")
    public String detail(@PathVariable("id") Long id) {
        // 현재 상세 화면은 /detail/{id} 기준으로 result 모델을 사용하므로
        // 예전 경로도 동일한 상세 라우트로 연결한다.
        return "redirect:/detail/" + id;
    }
}

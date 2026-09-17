package kopo.poly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 검증 통계 대시보드 화면을 반환하는 페이지 컨트롤러다.
 */
@Controller
public class StatsPageController {

    @GetMapping("/stats")
    public String stats() {
        return "stats";
    }
}

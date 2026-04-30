package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import kopo.poly.dto.HistoryDTO;
import kopo.poly.service.IHistoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 사용자의 분석 이력 화면과 이력 데이터를 연결하는 컨트롤러다.
 */
@Controller
public class HistoryController {

    private final IHistoryService historyService;

    public HistoryController(IHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history")
    public String history(Model model, HttpSession session) {
        try {
            // 세션에 저장된 로그인 사용자 기준으로 분석 이력을 조회한다.
            List<HistoryDTO> list = historyService.getHistory(extractUserId(session));
            model.addAttribute("list", list);
            model.addAttribute("historyList", list);
        } catch (Exception e) {
            model.addAttribute("list", new ArrayList<>());
            model.addAttribute("historyList", new ArrayList<>());
            model.addAttribute("error", "DB connection failed. Check whether MariaDB is running.");
        }
        return "history";
    }

    private Long extractUserId(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        if (userId == null) {
            return null;
        }

        if (userId instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(userId));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

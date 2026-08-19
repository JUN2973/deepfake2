package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(검증기록): 검증기록 목록과 상세 조회 화면을 담당한다.
 */
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import kopo.poly.dto.HistoryDTO;
import kopo.poly.service.IHistoryService;
import kopo.poly.util.SessionUtil;
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
            List<HistoryDTO> list = historyService.getHistory(SessionUtil.getUserId(session));
            model.addAttribute("list", list);
            model.addAttribute("historyList", list);
        } catch (Exception e) {
            model.addAttribute("list", new ArrayList<>());
            model.addAttribute("historyList", new ArrayList<>());
            model.addAttribute("error", "DB 연결에 실패했습니다. MariaDB가 실행 중인지 확인해 주세요.");
        }
        return "deepfake-history";
    }

}

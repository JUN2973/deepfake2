package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(결과 상세): 새벽 분석 결과, 상세 점수, 판별 항목 표시 화면을 담당한다.
 */
import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.IVerifyService;
import kopo.poly.util.SessionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 분석 결과 화면에 표시할 업로드 검증 정보를 조회한다.
 */
@Controller
@RequiredArgsConstructor
public class ResultController {

    private final IVerifyService verifyService;

    @Value("${app.detail.show-analysis-source-badge:true}")
    private boolean showAnalysisSourceBadge;

    @GetMapping({"/detail/{id}", "/result/{id}"})
    public String detail(@PathVariable("id") Long id, ModelMap model, HttpSession session) {
        // 업로드 완료 후 redirect된 상세 화면에서 저장된 분석 결과를 다시 조회한다.
        VerifyDTO rDTO;
        try {
            rDTO = verifyService.getOne(id);
        } catch (DataAccessException e) {
            return "redirect:/history";
        }

        if (rDTO == null || !SessionUtil.canAccessVerification(session, rDTO.getUserId(), rDTO.getId())) {
            return "redirect:/history";
        }

        model.addAttribute("result", rDTO);
        model.addAttribute("safeApiRaw", safeJsonForScript(rDTO.getApiRaw()));
        model.addAttribute("safeAnalysisJson", safeJsonForScript(rDTO.getAnalysisJson()));
        model.addAttribute("showAnalysisSourceBadge", showAnalysisSourceBadge);
        return "deepfake-result";
    }

    private String safeJsonForScript(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("<", "\\u003C")
                .replace(">", "\\u003E")
                .replace("&", "\\u0026")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
    }
}

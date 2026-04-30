package kopo.poly.controller;

import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.IVerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, ModelMap model) throws Exception {
        // 업로드 완료 후 redirect된 상세 화면에서 저장된 분석 결과를 다시 조회한다.
        VerifyDTO rDTO = verifyService.getOne(id);

        if (rDTO == null) {
            return "redirect:/history";
        }

        model.addAttribute("result", rDTO);
        model.addAttribute("showAnalysisSourceBadge", showAnalysisSourceBadge);
        return "detail";
    }
}

package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.IVerifyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * 검증 상세 화면과 분석 결과 데이터를 연결하는 페이지 컨트롤러다.
 */
@Controller
public class VerifyController {

    private final IVerifyService verifyService;

    public VerifyController(IVerifyService verifyService) {
        this.verifyService = verifyService;
    }

    @PostMapping("/verify")
    public String verify(MultipartHttpServletRequest request, HttpSession session) throws Exception {
        MultipartFile file = request.getFile("file");
        VerifyDTO rDTO = verifyService.createVerification(file, extractUserId(session));
        return "redirect:/detail/" + rDTO.getId();
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

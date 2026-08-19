package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.IVerifyService;
import kopo.poly.util.SessionUtil;
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
        // 기존 /verify 업로드 경로도 VerifyService의 공통 분석 파이프라인을 사용한다.
        MultipartFile file = request.getFile("file");
        VerifyDTO rDTO = verifyService.createVerification(file, SessionUtil.getUserId(session));
        SessionUtil.rememberVerificationId(session, rDTO.getId());
        // 분석 결과 id를 상세 화면 URL에 붙여 결과 페이지로 이동한다.
        return "redirect:/detail/" + rDTO.getId();
    }
}

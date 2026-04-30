package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.service.IVerifyService;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * 홈 화면에서 업로드한 이미지 파일을 분석 서비스로 넘기는 컨트롤러다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UploadController {

    private final IVerifyService verifyService;

    @PostMapping("/upload")
    public ModelAndView upload(MultipartHttpServletRequest request, HttpSession session) {

        ModelAndView mav = new ModelAndView();

        try {
            // 메인 시연 흐름의 시작점으로, home.jsp에서 선택한 이미지 파일을 받는다.
            MultipartFile file = request.getFile("file");
            if (file == null || file.isEmpty()) {
                mav.addObject("error", "Please select an image file.");
                mav.setViewName("home");
                return mav;
            }

            // 분석 파이프라인에는 이미지 파일만 들어갈 수 있도록 제한한다.
            String ct = CmmUtil.nvl(file.getContentType());
            if (!ct.startsWith("image/")) {
                mav.addObject("error", "Only image files are allowed.");
                mav.setViewName("home");
                return mav;
            }

            // multipart 설정과 맞춰 컨트롤러에서도 32MB 제한을 한 번 더 확인한다.
            if (file.getSize() > 32L * 1024 * 1024) {
                mav.addObject("error", "File size must be 32MB or smaller.");
                mav.setViewName("home");
                return mav;
            }

            // 서비스에서 파일 저장, 분석 실행, DB 저장을 처리하고 생성된 id를 반환한다.
            Long id = verifyService.createVerification(file, extractUserId(session)).getId();
            mav.setViewName("redirect:/detail/" + id);
            return mav;

        } catch (Exception e) {
            log.error("upload error", e);
            mav.addObject("error", "Upload/analysis failed: " + e.getMessage());
            mav.setViewName("home");
            return mav;
        }
    }

    private Long extractUserId(HttpSession session) {
        // USER_ID는 일반 로그인 또는 구글 OAuth 로그인 성공 시 세션에 저장된다.
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

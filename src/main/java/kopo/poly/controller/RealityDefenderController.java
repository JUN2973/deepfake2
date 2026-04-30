package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.service.IRealityDefenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * Reality Defender 기반 탐지 요청을 기존 화면 흐름으로 연결하는 컨트롤러다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/detect")
public class RealityDefenderController {

    private final IRealityDefenderService realityDefenderService;

    @GetMapping
    public String detectPage() {
        // 별도 detect 화면은 사용하지 않고 홈 화면의 업로드 흐름으로 통합한다.
        return "redirect:/";
    }

    @PostMapping("/image")
    public String detectImage(MultipartHttpServletRequest request,
                              HttpSession session) {
        try {
            MultipartFile file = request.getFile("file");
            Long userId = extractUserId(session);
            realityDefenderService.analyzeImage(file, userId);
            return "redirect:/history";
        } catch (IllegalArgumentException e) {
            log.warn("detect validation error: {}", e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            log.error("detect image error", e);
            return "redirect:/";
        }
    }

    @GetMapping("/result/{id}")
    public String detectResult(@PathVariable("id") Long id) {
        return "redirect:/history";
    }

    @GetMapping("/history")
    public String detectHistory() {
        return "redirect:/history";
    }

    @GetMapping("/history/{id}")
    public String detectHistoryDetail(@PathVariable("id") Long id) {
        return "redirect:/history";
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

package kopo.poly.controller;


/**
 * 체크리스트 기준 주석: 구현(딥페이크 판별): 이미지 업로드, 형식/용량 검사, 분석 요청 시작을 담당한다.
 */

/**
 * 발표용 설명: 사용자가 업로드한 이미지를 분석 서비스로 넘기는 첫 서버 진입점입니다.
 * 브라우저 검증을 신뢰하지 않고 서버에서 파일 존재 여부, 이미지 MIME 타입, 32MB 용량 제한을 다시 검사합니다.
 * 검사를 통과한 파일은 VerifyService로 전달되고, 분석 완료 후 상세 결과 화면으로 이동합니다.
 */
import jakarta.servlet.http.HttpSession;
import kopo.poly.service.IVerifyService;
import kopo.poly.util.CommonUtil;
import kopo.poly.util.SessionUtil;
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
                mav.addObject("error", "이미지 파일을 선택해 주세요.");
                mav.setViewName("home");
                return mav;
            }

            // 분석 파이프라인에는 이미지 파일만 들어갈 수 있도록 제한한다.
            String ct = CommonUtil.nvl(file.getContentType());
            if (!ct.startsWith("image/")) {
                mav.addObject("error", "이미지 파일만 업로드할 수 있습니다.");
                mav.setViewName("home");
                return mav;
            }

            // multipart 설정과 맞춰 컨트롤러에서도 32MB 제한을 한 번 더 확인한다.
            if (file.getSize() > 32L * 1024 * 1024) {
                mav.addObject("error", "파일 크기는 32MB 이하여야 합니다.");
                mav.setViewName("home");
                return mav;
            }

            // 서비스에서 파일 저장, 분석 실행, DB 저장을 처리하고 생성된 id를 반환한다.
            Long id = verifyService.createVerification(file, SessionUtil.getUserId(session)).getId();
            SessionUtil.rememberVerificationId(session, id);
            mav.setViewName("redirect:/detail/" + id);
            return mav;

        } catch (Exception e) {
            log.error("upload error", e);
            mav.addObject("error", "업로드 또는 분석에 실패했습니다: " + e.getMessage());
            mav.setViewName("home");
            return mav;
        }
    }

}

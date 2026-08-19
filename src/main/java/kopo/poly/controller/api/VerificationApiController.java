package kopo.poly.controller.api;


/**
 * 체크리스트 기준 주석: 구현(검증기록): 사용자의 검증기록 목록/상세/삭제 API를 처리한다.
 */

/**
 * 발표용 설명: 검증기록을 API 형태로 생성, 조회, 삭제하는 컨트롤러입니다.
 * 세션의 사용자 ID를 기준으로 본인 기록만 접근하게 하여 다른 사용자의 분석 결과 조회를 막습니다.
 */
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.VerificationCreateResponseDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IVerifyMapper;
import kopo.poly.service.IVerifyService;
import kopo.poly.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * 이미지 업로드 검증 이력을 API 방식으로 생성, 조회, 삭제하는 컨트롤러다.
 */
@RestController
@RequestMapping("/api/v1/verifications")
public class VerificationApiController {

    private static final Logger log = LoggerFactory.getLogger(VerificationApiController.class);

    private final IVerifyService verifyService;
    private final IVerifyMapper verifyMapper;

    public VerificationApiController(IVerifyService verifyService, IVerifyMapper verifyMapper) {
        this.verifyService = verifyService;
        this.verifyMapper = verifyMapper;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<VerificationCreateResponseDTO> create(MultipartHttpServletRequest request,
                                                             HttpSession session) {
        try {
            // API 업로드도 JSP 업로드와 같은 서비스 파이프라인을 재사용한다.
            MultipartFile file = request.getFile("file");
            VerifyDTO saved = verifyService.createVerification(file, SessionUtil.getUserId(session));
            SessionUtil.rememberVerificationId(session, saved.getId());

            VerificationCreateResponseDTO out = new VerificationCreateResponseDTO();
            out.setId(saved.getId());
            out.setOriginalName(saved.getOriginalName());
            out.setObjectKey(saved.getObjectKey());
            out.setPublicUrl(saved.getPublicUrl());
            out.setVerdict(saved.getVerdict());
            out.setScore(saved.getScore());
            out.setCreatedAt(saved.getRegDt());

            return ApiResponse.ok(out);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("DF-4001", e.getMessage());
        } catch (Exception e) {
            log.error("Verification create error", e);
            return ApiResponse.fail("DF-5000", "분석 요청 처리 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<VerifyDTO> detail(@PathVariable Long id, HttpSession session) {
        VerifyDTO r = verifyService.getOne(id);
        if (r == null) {
            return ApiResponse.fail("DF-4040", "데이터를 찾을 수 없습니다.");
        }
        if (!SessionUtil.canAccessVerification(session, r.getUserId(), r.getId())) {
            return ApiResponse.fail("DF-4030", "접근 권한이 없습니다.");
        }
        return ApiResponse.ok(r);
    }

    @GetMapping
    public ApiResponse<List<VerifyDTO>> list(HttpServletRequest request, HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("DF-4010", "로그인이 필요합니다.");
        }

        // 이 프로젝트에서는 파라미터 어노테이션 대신 요청 객체에서 직접 값을 읽는다.
        int page = parseInt(request.getParameter("page"), 1);
        int size = clamp(parseInt(request.getParameter("size"), 20), 1, 100);
        String verdict = request.getParameter("verdict");
        int offset = Math.max(0, (page - 1) * size);
        List<VerifyDTO> list = verifyMapper.selectVerificationList(offset, size, verdict, userId);
        return ApiResponse.ok(list);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id, HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("DF-4010", "로그인이 필요합니다.");
        }

        if (!verifyService.deleteVerification(id, userId)) {
            return ApiResponse.fail("DF-4041", "검증 기록을 찾을 수 없습니다.");
        }

        return ApiResponse.ok(null);
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

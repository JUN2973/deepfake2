package kopo.poly.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.VerificationCreateResponseDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IVerifyMapper;
import kopo.poly.service.IVerifyService;
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
            VerifyDTO saved = verifyService.createVerification(file, extractUserId(session));

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
            return ApiResponse.fail("DF-5000", "Server error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<VerifyDTO> detail(@PathVariable Long id) {
        VerifyDTO r = verifyService.getOne(id);
        if (r == null) {
            return ApiResponse.fail("DF-4040", "Data not found.");
        }
        return ApiResponse.ok(r);
    }

    @GetMapping
    public ApiResponse<List<VerifyDTO>> list(HttpServletRequest request) {
        // 이 프로젝트에서는 파라미터 어노테이션 대신 요청 객체에서 직접 값을 읽는다.
        int page = parseInt(request.getParameter("page"), 1);
        int size = parseInt(request.getParameter("size"), 20);
        String verdict = request.getParameter("verdict");
        int offset = Math.max(0, (page - 1) * size);
        List<VerifyDTO> list = verifyMapper.selectVerificationList(offset, size, verdict);
        return ApiResponse.ok(list);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id, HttpSession session) {
        Long userId = extractUserId(session);
        if (userId == null) {
            return ApiResponse.fail("DF-4010", "Login required.");
        }

        if (!verifyService.deleteVerification(id, userId)) {
            return ApiResponse.fail("DF-4041", "Verification record not found.");
        }

        return ApiResponse.ok(null);
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
}

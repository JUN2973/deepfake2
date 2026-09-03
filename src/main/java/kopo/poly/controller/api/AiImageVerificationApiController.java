package kopo.poly.controller.api;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.AiImageVerificationResponseDTO;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.AiAnalysisServiceException;
import kopo.poly.service.IAiImageVerificationService;
import kopo.poly.service.IVerifyService;
import kopo.poly.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/verifications")
public class AiImageVerificationApiController {

    private static final Logger log = LoggerFactory.getLogger(AiImageVerificationApiController.class);

    private final IVerifyService verifyService;
    private final IAiImageVerificationService imageVerificationService;

    public AiImageVerificationApiController(IVerifyService verifyService,
                                            IAiImageVerificationService imageVerificationService) {
        this.verifyService = verifyService;
        this.imageVerificationService = imageVerificationService;
    }

    @GetMapping("/{id}/image-review")
    public ApiResponse<AiImageVerificationResponseDTO> getSavedReview(
            @PathVariable Long id,
            HttpSession session) {
        try {
            VerifyDTO verification = verifyService.getOne(id);
            ApiResponse<AiImageVerificationResponseDTO> accessError = validateAccess(verification, session);
            if (accessError != null) {
                return accessError;
            }
            return ApiResponse.ok(imageVerificationService.getLatest(id));
        } catch (Exception e) {
            log.error("Failed to load saved AI image verification. verificationId={}", id, e);
            return ApiResponse.fail("AI-5003", "저장된 AI 이미지 검증 결과를 불러오지 못했습니다.");
        }
    }

    @PostMapping("/{id}/image-review")
    public ApiResponse<AiImageVerificationResponseDTO> reviewImage(
            @PathVariable Long id,
        HttpSession session) {
        try {
            VerifyDTO verification = verifyService.getOne(id);
            ApiResponse<AiImageVerificationResponseDTO> accessError = validateAccess(verification, session);
            if (accessError != null) {
                return accessError;
            }
            return ApiResponse.ok(imageVerificationService.verify(verification));
        } catch (AiAnalysisServiceException e) {
            log.warn("AI image verification failed. code={}, verificationId={}", e.getCode(), id);
            return ApiResponse.fail(e.getCode(), userMessage(e.getCode()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("AI-4000", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected AI image verification error. verificationId={}", id, e);
            return ApiResponse.fail("AI-5002", "AI 이미지 2차 검증 중 오류가 발생했습니다.");
        }
    }

    private ApiResponse<AiImageVerificationResponseDTO> validateAccess(
            VerifyDTO verification,
            HttpSession session) {
        if (verification == null) {
            return ApiResponse.fail("AI-4040", "분석 결과를 찾을 수 없습니다.");
        }
        if (!SessionUtil.canAccessVerification(session, verification.getUserId(), verification.getId())) {
            return ApiResponse.fail("AI-4030", "이 분석 결과에 접근할 권한이 없습니다.");
        }
        return null;
    }

    private String userMessage(String code) {
        return switch (code) {
            case "AI-CONFIG" -> "Gemini API 키가 아직 설정되지 않았습니다.";
            case "AI-AUTH" -> "Gemini 인증에 실패했습니다.";
            case "AI-RATE-LIMIT" -> "Gemini 무료 사용량 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.";
            case "AI-IMAGE-MISSING", "AI-IMAGE-READ" -> "검증할 원본 이미지를 불러오지 못했습니다.";
            case "AI-IMAGE-TYPE" -> "AI 2차 검증이 지원하지 않는 이미지 형식입니다.";
            case "AI-IMAGE-SIZE" -> "이미지가 너무 커서 AI 2차 검증을 진행할 수 없습니다.";
            case "AI-REQUEST" -> "AI 이미지 검증 요청을 처리할 수 없습니다.";
            case "AI-RESPONSE" -> "AI 이미지 검증 응답을 해석할 수 없습니다. 다시 시도해 주세요.";
            default -> "AI 이미지 2차 검증 중 오류가 발생했습니다.";
        };
    }
}

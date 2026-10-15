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

/**
 * 체크리스트 기준 주석: 추가 기능 2(Gemini 이미지 2차 검증): 2차 검증 실행·저장 결과 조회 REST API를 처리한다.
 */

/**
 * 발표용 설명: 결과 화면의 [이미지 다시 검토] 버튼이 호출하는 컨트롤러입니다.
 * 흐름: deepfake-result.jsp generateImageReview() → POST /api/v1/ai/verifications/{id}/image-review (본문 없음)
 *       → 이 클래스 → GeminiImageVerificationService.verify() → 저장소에서 원본 읽기 → Gemini Vision → ai_analysis_result
 * 이미지는 업로드 때 서버에 이미 저장되어 있으므로 브라우저는 검증 번호만 보내고, 서버가 직접 원본을 읽습니다.
 */
@RestController
@RequestMapping("/api/v1/ai/verifications")
public class AiImageVerificationApiController {

    private static final Logger log = LoggerFactory.getLogger(AiImageVerificationApiController.class);

    private final IVerifyService verifyService;                          // 검증 기록 조회
    private final IAiImageVerificationService imageVerificationService;  // 실제 구현: GeminiImageVerificationService

    // 생성자 주입: 스프링이 구현체를 자동으로 넣어 준다.
    public AiImageVerificationApiController(IVerifyService verifyService,
                                            IAiImageVerificationService imageVerificationService) {
        this.verifyService = verifyService;
        this.imageVerificationService = imageVerificationService;
    }

    /**
     * [GET] 저장된 최신 2차 검증 결과 조회.
     * 결과 화면이 열릴 때 loadSavedImageReview()가 호출한다. Gemini는 부르지 않고 DB만 읽는다.
     */
    @GetMapping("/{id}/image-review")
    public ApiResponse<AiImageVerificationResponseDTO> getSavedReview(
            @PathVariable Long id,
            HttpSession session) {
        try {
            VerifyDTO verification = verifyService.getOne(id);
            ApiResponse<AiImageVerificationResponseDTO> accessError = validateAccess(verification, session);
            if (accessError != null) {
                return accessError; // 없거나 남의 기록이면 오류 응답 그대로 반환
            }
            return ApiResponse.ok(imageVerificationService.getLatest(id));
        } catch (Exception e) {
            log.error("Failed to load saved AI image verification. verificationId={}", id, e);
            return ApiResponse.fail("AI-5003", "저장된 AI 이미지 검증 결과를 불러오지 못했습니다.");
        }
    }

    /**
     * [POST] 이미지 2차 검증 실행. [이미지 다시 검토] 버튼을 누르면 호출된다.
     * 요청 본문이 없다 → 필요한 것은 URL의 검증 번호와 세션(로그인 정보)뿐.
     */
    @PostMapping("/{id}/image-review")
    public ApiResponse<AiImageVerificationResponseDTO> reviewImage(
            @PathVariable Long id,
        HttpSession session) {
        try {
            // 1) 검증 기록 조회: 이미지 위치(object_key), MIME, 크기, 1차 판정이 들어 있다.
            VerifyDTO verification = verifyService.getOne(id);
            // 2) 없으면 AI-4040, 내 기록이 아니면 AI-4030 (통과하면 null)
            ApiResponse<AiImageVerificationResponseDTO> accessError = validateAccess(verification, session);
            if (accessError != null) {
                return accessError;
            }
            // 3) 서비스 호출: 검사 → 저장 결과 재사용 → (없으면) 이미지 읽기 → Gemini → 교차 검증 → 저장
            return ApiResponse.ok(imageVerificationService.verify(verification));
        } catch (AiAnalysisServiceException e) {
            // 서비스가 정한 오류 코드(AI-IMAGE-SIZE 등)를 한국어 안내문으로 바꿔 응답
            log.warn("AI image verification failed. code={}, verificationId={}", e.getCode(), id);
            return ApiResponse.fail(e.getCode(), userMessage(e.getCode()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("AI-4000", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected AI image verification error. verificationId={}", id, e);
            return ApiResponse.fail("AI-5002", "AI 이미지 2차 검증 중 오류가 발생했습니다.");
        }
    }

    /**
     * GET·POST가 함께 쓰는 접근 확인.
     * 기록이 없으면 AI-4040, 세션 사용자가 기록 주인이 아니면 AI-4030 응답을 돌려주고, 통과하면 null.
     */
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

    // 서비스 오류 코드 → 화면에 보여 줄 한국어 문장(내부 메시지는 노출하지 않음)
    private String userMessage(String code) {
        return switch (code) {
            case "AI-CONFIG" -> "Gemini API 키가 아직 설정되지 않았습니다.";
            case "AI-AUTH" -> "Gemini 인증에 실패했습니다.";
            case "AI-RATE-LIMIT" -> "Gemini 무료 사용량 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.";
            case "AI-OVERLOADED" -> "Gemini 서버가 일시적으로 혼잡합니다. 잠시 후 다시 시도해 주세요."; // 503(재시도 후에도 실패)
            case "AI-IMAGE-MISSING", "AI-IMAGE-READ" -> "검증할 원본 이미지를 불러오지 못했습니다."; // 저장 이미지 없음/읽기 실패
            case "AI-IMAGE-TYPE" -> "AI 2차 검증이 지원하지 않는 이미지 형식입니다.";             // jpeg·png·webp·gif 아님
            case "AI-IMAGE-SIZE" -> "이미지가 너무 커서 AI 2차 검증을 진행할 수 없습니다.";          // 8MB 초과
            case "AI-REQUEST" -> "AI 이미지 검증 요청을 처리할 수 없습니다.";
            case "AI-RESPONSE" -> "AI 이미지 검증 응답을 해석할 수 없습니다. 다시 시도해 주세요.";
            default -> "AI 이미지 2차 검증 중 오류가 발생했습니다.";
        };
    }
}

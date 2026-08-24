package kopo.poly.controller.api;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.AiAnalysisRequestDTO;
import kopo.poly.dto.AiAnalysisResponseDTO;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.AiAnalysisServiceException;
import kopo.poly.service.IAiAnalysisService;
import kopo.poly.service.IVerifyService;
import kopo.poly.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAnalysisApiController {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisApiController.class);

    private final IVerifyService verifyService;
    private final IAiAnalysisService aiAnalysisService;

    public AiAnalysisApiController(IVerifyService verifyService,
                                   IAiAnalysisService aiAnalysisService) {
        this.verifyService = verifyService;
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/verifications/{id}/explanation")
    public ApiResponse<AiAnalysisResponseDTO> generateExplanation(
            @PathVariable Long id,
            @RequestBody(required = false) AiAnalysisRequestDTO request,
            HttpSession session) {
        try {
            VerifyDTO verification = verifyService.getOne(id);
            if (verification == null) {
                return ApiResponse.fail("AI-4040", "분석 결과를 찾을 수 없습니다.");
            }

            if (!SessionUtil.canAccessVerification(
                    session,
                    verification.getUserId(),
                    verification.getId()
            )) {
                return ApiResponse.fail("AI-4030", "이 분석 결과에 접근할 권한이 없습니다.");
            }

            AiAnalysisRequestDTO normalizedRequest = normalizeRequest(request, id);
            AiAnalysisResponseDTO response = aiAnalysisService.analyze(
                    normalizedRequest,
                    verification
            );
            return ApiResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("AI-4000", e.getMessage());
        } catch (AiAnalysisServiceException e) {
            log.warn("AI analysis request failed. code={}, verificationId={}", e.getCode(), id);
            return ApiResponse.fail(e.getCode(), userMessage(e.getCode()));
        } catch (Exception e) {
            log.error("Unexpected AI analysis error. verificationId={}", id, e);
            return ApiResponse.fail("AI-5000", "AI 해설을 생성하는 중 오류가 발생했습니다.");
        }
    }

    private AiAnalysisRequestDTO normalizeRequest(AiAnalysisRequestDTO request, Long id) {
        AiAnalysisRequestDTO normalized = request == null
                ? new AiAnalysisRequestDTO()
                : request;

        if (normalized.getVerificationId() != null
                && !normalized.getVerificationId().equals(id)) {
            throw new IllegalArgumentException("요청한 분석 결과 번호가 경로의 번호와 일치하지 않습니다.");
        }

        normalized.setVerificationId(id);
        return normalized;
    }

    private String userMessage(String code) {
        if (code == null) {
            return "AI 해설을 생성하는 중 오류가 발생했습니다.";
        }

        return switch (code) {
            case "AI-CONFIG" -> "AI 서비스 설정이 완료되지 않았습니다.";
            case "AI-AUTH" -> "AI 서비스 인증에 실패했습니다.";
            case "AI-RATE-LIMIT" -> "AI 사용량 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.";
            case "AI-CONNECTION" -> "AI 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.";
            case "AI-REFUSAL" -> "AI가 이 분석에 대한 해설 생성을 거절했습니다.";
            case "AI-REQUEST" -> "AI 해설 요청 데이터를 처리할 수 없습니다.";
            case "AI-RESPONSE" -> "AI 응답을 해석할 수 없습니다. 다시 시도해 주세요.";
            default -> "AI 해설을 생성하는 중 오류가 발생했습니다.";
        };
    }
}

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 체크리스트 기준 주석: 추가 기능 1(Gemini AI 상세 해설): 해설 생성·저장된 해설 조회 REST API를 처리한다.
 */

/**
 * 발표용 설명: 결과 화면의 [상세 해설 생성] 버튼이 호출하는 컨트롤러입니다.
 * 흐름: deepfake-result.jsp generateAiExplanation() → POST /api/v1/ai/verifications/{id}/explanation
 *       → 이 클래스 → GeminiAnalysisService.analyze() → IAiAnalysisResultMapper → ai_analysis_result 테이블
 * 컨트롤러는 "기록이 있는지, 내 기록인지"만 확인하고 실제 Gemini 호출·캐시·DB 저장은 서비스에 맡깁니다(얇은 컨트롤러).
 * 결과는 항상 ApiResponse(success/data/error)로 감싸서 JSON으로 돌려줍니다.
 */
@RestController // 반환값을 JSP 이름이 아니라 JSON 응답 본문으로 보낸다.
@RequestMapping("/api/v1/ai") // 이 클래스의 모든 주소 앞에 붙는 공통 경로
public class AiAnalysisApiController {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisApiController.class);
    // 현재 클래스에서 log.info(), log.warn(), log.error()를 사용하기 위한 로그 객체 생성 = @Slf4j를 활용하면 안써도 된다.

    // 구현 클래스가 아니라 인터페이스 타입으로 받는다 → 실제 객체(VerifyService, GeminiAnalysisService)는 스프링이 넣어 준다.
    private final IVerifyService verifyService;
    private final IAiAnalysisService aiAnalysisService;

    // 생성자 주입: 스프링이 @Service로 등록된 구현체를 찾아 자동으로 넣어 준다.
    public AiAnalysisApiController(IVerifyService verifyService,
                                   IAiAnalysisService aiAnalysisService) {
        this.verifyService = verifyService;
        this.aiAnalysisService = aiAnalysisService;
    }

    /**
     * [GET] 저장된 최신 해설 조회.
     * 결과 화면이 열릴 때 JSP의 loadSavedAiExplanation()이 자동으로 호출한다.
     * Gemini를 새로 부르지 않고 DB(ai_analysis_result)에 저장된 최신 EXPLANATION 1건만 꺼내 보여 준다.
     */
    @GetMapping("/verifications/{id}/explanation")
    public ApiResponse<AiAnalysisResponseDTO> getSavedExplanation(
            @PathVariable Long id, // @PathVariable - URL 경로에 들어있는 값을 꺼내서 Controller 메서드의 변수로 받기 위해 사용
            HttpSession session) {
        try {
            // 1) 검증 기록 조회 (SELECT … FROM verification WHERE id = ?)
            VerifyDTO verification = verifyService.getOne(id);
            if (verification == null) {
                return ApiResponse.fail("AI-4040", "분석 결과를 찾을 수 없습니다.");
            }
            // 2) 세션 사용자와 기록 주인이 같은지 확인 → 남의 해설은 볼 수 없다.
            if (!SessionUtil.canAccessVerification(session, verification.getUserId(), verification.getId())) {
                return ApiResponse.fail("AI-4030", "이 분석 결과에 접근할 권한이 없습니다.");
            } //canAccessVerification(...) - true면 접근가능 false면 접근 불가
            // 3) 저장된 최신 해설(없으면 data=null)을 성공 응답으로 반환
            return ApiResponse.ok(aiAnalysisService.getLatest(id));
        } catch (Exception e) {
            log.error("Failed to load saved AI analysis. verificationId={}", id, e);
            return ApiResponse.fail("AI-5001", "저장된 AI 해설을 불러오지 못했습니다.");
        }
    }

    /**
     * [POST] 상세 해설 생성. [상세 해설 생성] 버튼을 누르면 호출된다.
     * 순서: 기록 조회 → 권한 확인 → 요청 번호 정리 → 서비스 analyze() 호출 → ApiResponse로 반환.
     * 같은 요청은 서비스가 캐시/DB에서 재사용하므로 Gemini가 매번 호출되지는 않는다.
     */
    @PostMapping("/verifications/{id}/explanation")
    public ApiResponse<AiAnalysisResponseDTO> generateExplanation(
            @PathVariable Long id, // URL의 {id} (검증 번호)
            @RequestBody(required = false) AiAnalysisRequestDTO request, // JSON 본문 → DTO (본문이 없어도 null로 받음)
            HttpSession session) { // 로그인 정보가 들어 있는 세션
        try {
            // 1) 해설의 재료(판정·점수·원문 JSON)가 들어 있는 검증 기록 조회
            VerifyDTO verification = verifyService.getOne(id);
            if (verification == null) {
                return ApiResponse.fail("AI-4040", "분석 결과를 찾을 수 없습니다.");
            }

            // 2) 내 기록인지 확인 → 남의 결과로 AI 비용을 쓰는 것을 막는다.
            if (!SessionUtil.canAccessVerification(
                    session,
                    verification.getUserId(),
                    verification.getId()
            )) {
                return ApiResponse.fail("AI-4030", "이 분석 결과에 접근할 권한이 없습니다.");
            }

            // 3) 본문의 verificationId를 URL의 id로 고정(다르면 IllegalArgumentException → AI-4000)
            AiAnalysisRequestDTO normalizedRequest = normalizeRequest(request, id);
            // 4) 서비스 호출: 캐시 확인 → (없으면) Gemini 호출 → DB 저장까지 서비스가 처리
            AiAnalysisResponseDTO response = aiAnalysisService.analyze(
                    normalizedRequest,
                    verification
            );
            // 5) {success:true, data:{summary, explanation, ...}} 형태로 응답
            return ApiResponse.ok(response);
        } catch (IllegalArgumentException e) {
            // 요청 값이 잘못된 경우(예: 본문 번호와 URL 번호 불일치)
            return ApiResponse.fail("AI-4000", e.getMessage());
        } catch (AiAnalysisServiceException e) {
            // 서비스가 정한 오류 코드(AI-CONFIG, AI-RATE-LIMIT 등). 로그에는 코드만, 화면에는 한국어 안내문만 보낸다.
            log.warn("AI analysis request failed. code={}, verificationId={}", e.getCode(), id);
            return ApiResponse.fail(e.getCode(), userMessage(e.getCode()));
        } catch (Exception e) {
            // 예상하지 못한 오류는 스택까지 ERROR 로그로 남기고 공통 메시지로 응답
            log.error("Unexpected AI analysis error. verificationId={}", id, e);
            return ApiResponse.fail("AI-5000", "AI 해설을 생성하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 요청 DTO를 정리한다.
     * - 본문이 없으면 빈 DTO를 만든다(서비스가 기본 질문·어조를 채움).
     * - 본문에 다른 검증 번호가 들어 있으면 거절한다 → 다른 기록 ID를 끼워 넣는 요청 차단.
     * - 최종적으로 URL의 id를 verificationId로 고정한다.
     */
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

    /**
     * 서비스 오류 코드를 사용자에게 보여 줄 한국어 문장으로 바꾼다.
     * 내부 예외 메시지(영문, API 키 관련 정보 등)를 화면에 그대로 노출하지 않기 위해서다.
     */
    private String userMessage(String code) {
        if (code == null) {
            return "AI 해설을 생성하는 중 오류가 발생했습니다.";
        }

        return switch (code) {
            case "AI-CONFIG" -> "AI 서비스 설정이 완료되지 않았습니다.";          // API 키 없음
            case "AI-AUTH" -> "AI 서비스 인증에 실패했습니다.";                  // Gemini 401/403
            case "AI-RATE-LIMIT" -> "AI 사용량 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."; // Gemini 429
            case "AI-OVERLOADED" -> "AI 서버가 일시적으로 혼잡합니다. 잠시 후 다시 시도해 주세요."; // Gemini 503(재시도 후에도 실패)
            case "AI-CONNECTION" -> "AI 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."; // 연결 실패·타임아웃
            case "AI-REFUSAL" -> "AI가 이 분석에 대한 해설 생성을 거절했습니다.";
            case "AI-REQUEST" -> "AI 해설 요청 데이터를 처리할 수 없습니다.";      // 요청 JSON 생성 실패
            case "AI-RESPONSE" -> "AI 응답을 해석할 수 없습니다. 다시 시도해 주세요."; // 응답 형식 이상
            default -> "AI 해설을 생성하는 중 오류가 발생했습니다.";
        };
    }
}

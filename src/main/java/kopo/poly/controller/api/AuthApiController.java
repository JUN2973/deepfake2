package kopo.poly.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.AuthenticatedUserDTO;
import kopo.poly.dto.FindIdRequestDTO;
import kopo.poly.dto.FindPasswordRequestDTO;
import kopo.poly.dto.LoginRequestDTO;
import kopo.poly.dto.PasswordChangeRequestDTO;
import kopo.poly.dto.ProfileResponseDTO;
import kopo.poly.dto.ProfileUpdateRequestDTO;
import kopo.poly.dto.ResetPasswordRequestDTO;
import kopo.poly.dto.SendCodeRequestDTO;
import kopo.poly.dto.SignupCompleteRequestDTO;
import kopo.poly.dto.VerifyCodeRequestDTO;
import kopo.poly.service.AuthServiceException;
import kopo.poly.service.IAuthService;
import kopo.poly.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);
    private static final String DATASOURCE_ERROR =
            "데이터베이스 연결에 실패했습니다. MariaDB와 데이터소스 설정을 확인해 주세요.";

    private final IAuthService authService;

    public AuthApiController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Object> login(@RequestBody LoginRequestDTO req,
                                     HttpServletRequest request,
                                     HttpSession session) {
        return handleAuthRequest("Login", "AU-5000", DATASOURCE_ERROR, "서버 오류가 발생했습니다.", () -> {
            AuthenticatedUserDTO user = authService.login(req);

            request.changeSessionId();
            session.setAttribute("USER_ID", user.getId());
            session.setAttribute("USER_NAME", user.getName());
            session.setAttribute("USER_EMAIL", user.getEmail());

            return ApiResponse.ok(true);
        });
    }

    @PostMapping("/find-id")
    public ApiResponse<Object> findId(@RequestBody FindIdRequestDTO req) {
        return handleAuthRequest("Find-id", "AU-5004", DATASOURCE_ERROR,
                "아이디 찾기 중 오류가 발생했습니다.",
                () -> ApiResponse.ok(authService.findId(req)));
    }

    @PostMapping("/password/send-code")
    public ApiResponse<Object> sendPasswordResetCode(@RequestBody FindPasswordRequestDTO req) {
        return handleAuthRequest("Password send-code", "AU-5005", DATASOURCE_ERROR,
                "인증번호 발송 중 오류가 발생했습니다.", () -> {
                    authService.sendPasswordResetCode(req);
                    return ApiResponse.ok(true);
                });
    }

    @PostMapping("/password/verify-code")
    public ApiResponse<Object> verifyPasswordResetCode(@RequestBody VerifyCodeRequestDTO req) {
        return handleAuthRequest("Password verify-code", "AU-5006", DATASOURCE_ERROR,
                "인증번호 확인 중 오류가 발생했습니다.", () -> {
                    authService.verifyPasswordResetCode(req);
                    return ApiResponse.ok(true);
                });
    }

    @PostMapping("/password/reset")
    public ApiResponse<Object> resetPassword(@RequestBody ResetPasswordRequestDTO req) {
        return handleAuthRequest("Password reset", "AU-5007", DATASOURCE_ERROR,
                "비밀번호 재설정 중 오류가 발생했습니다.", () -> {
                    authService.resetPassword(req);
                    return ApiResponse.ok(true);
                });
    }

    @PostMapping("/signup/check-email")
    public ApiResponse<Object> checkEmail(@RequestBody SendCodeRequestDTO req) {
        return handleAuthRequest("Signup check-email", "AU-5008", DATASOURCE_ERROR,
                "이메일 확인 중 오류가 발생했습니다.",
                () -> ApiResponse.ok(authService.checkEmail(req)));
    }

    @PostMapping("/signup/send-code")
    public ApiResponse<Object> sendCode(@RequestBody SendCodeRequestDTO req) {
        return handleAuthRequest("Signup send-code", "AU-5001", DATASOURCE_ERROR,
                "인증 메일 발송에 실패했습니다.", () -> {
                    authService.sendSignupCode(req);
                    return ApiResponse.ok(true);
                });
    }

    @PostMapping("/signup/verify-code")
    public ApiResponse<Object> verifyCode(@RequestBody VerifyCodeRequestDTO req) {
        return handleAuthRequest("Signup verify-code", "AU-5002", DATASOURCE_ERROR,
                "인증번호 확인에 실패했습니다.", () -> {
                    authService.verifySignupCode(req);
                    return ApiResponse.ok(true);
                });
    }

    @PostMapping("/signup/complete")
    public ApiResponse<Object> complete(@RequestBody SignupCompleteRequestDTO req) {
        return handleAuthRequest("Signup complete", "AU-5003", DATASOURCE_ERROR,
                "회원가입 처리에 실패했습니다.", () -> {
                    authService.completeSignup(req);
                    return ApiResponse.ok(true);
                });
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.ok(true);
    }

    @GetMapping("/me")
    public ApiResponse<Object> me(HttpSession session) {
        return handleAuthRequest("Current user", "AU-5000", DATASOURCE_ERROR,
                "서버 오류가 발생했습니다.",
                () -> ApiResponse.ok(authService.getCurrentUser(SessionUtil.getUserId(session))));
    }

    @PutMapping("/profile")
    public ApiResponse<Object> updateProfile(@RequestBody ProfileUpdateRequestDTO req,
                                             HttpSession session) {
        return handleAuthRequest("Profile update", "AU-5010",
                "프로필 수정 중 데이터베이스 오류가 발생했습니다.",
                "프로필 수정 중 오류가 발생했습니다.", () -> {
                    ProfileResponseDTO profile = authService.updateProfile(SessionUtil.getUserId(session), req);
                    session.setAttribute("USER_NAME", profile.getName());
                    return ApiResponse.ok(profile);
                });
    }

    @PutMapping("/password")
    public ApiResponse<Object> changePassword(@RequestBody PasswordChangeRequestDTO req,
                                              HttpSession session) {
        return handleAuthRequest("Password change", "AU-5011",
                "비밀번호 변경 중 데이터베이스 오류가 발생했습니다.",
                "비밀번호 변경 중 오류가 발생했습니다.", () -> {
                    authService.changePassword(SessionUtil.getUserId(session), req);
                    return ApiResponse.ok(true);
                });
    }

    @DeleteMapping("/account")
    public ApiResponse<Object> deleteAccount(HttpSession session) {
        return handleAuthRequest("Delete account", "AU-5009",
                "회원탈퇴 처리 중 데이터베이스 오류가 발생했습니다.",
                "회원탈퇴 처리 중 오류가 발생했습니다.", () -> {
                    authService.deleteAccount(SessionUtil.getUserId(session));
                    session.invalidate();
                    return ApiResponse.ok(true);
                });
    }

    private ApiResponse<Object> handleAuthRequest(String action,
                                                  String fallbackCode,
                                                  String dbMessage,
                                                  String errorMessage,
                                                  AuthRequestHandler handler) {
        try {
            return handler.handle();
        } catch (AuthServiceException e) {
            return ApiResponse.fail(e.getCode(), e.getMessage());
        } catch (DataAccessException e) {
            log.error("{} DB error", action, e);
            return ApiResponse.fail(fallbackCode, dbMessage);
        } catch (Exception e) {
            log.error("{} error", action, e);
            return ApiResponse.fail(fallbackCode, errorMessage);
        }
    }

    @FunctionalInterface
    private interface AuthRequestHandler {
        ApiResponse<Object> handle();
    }
}

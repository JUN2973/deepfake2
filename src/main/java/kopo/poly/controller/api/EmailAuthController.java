package kopo.poly.controller.api;

import kopo.poly.dto.EmailAuthResponseDTO;
import kopo.poly.dto.EmailAuthSendRequestDTO;
import kopo.poly.dto.EmailAuthVerifyRequestDTO;
import kopo.poly.service.impl.EmailAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이메일 인증번호 발송과 검증 요청을 처리하는 API 컨트롤러다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/email")
public class EmailAuthController {

    private final EmailAuthService emailAuthService;

    @PostMapping("/send")
    public EmailAuthResponseDTO send(@RequestBody EmailAuthSendRequestDTO requestDTO) {
        try {
            return emailAuthService.sendAuthCode(requestDTO.getEmail());
        } catch (IllegalArgumentException e) {
            return new EmailAuthResponseDTO(false, e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Email auth send configuration error", e);
            return new EmailAuthResponseDTO(false, e.getMessage());
        } catch (MailAuthenticationException e) {
            log.error("Email auth SMTP authentication error", e);
            return new EmailAuthResponseDTO(false, "메일 서버 인증에 실패했습니다. 메일 계정 설정을 확인해 주세요.");
        } catch (Exception e) {
            log.error("Email auth send error", e);
            return new EmailAuthResponseDTO(false, "인증번호 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public EmailAuthResponseDTO verify(@RequestBody EmailAuthVerifyRequestDTO requestDTO) {
        return emailAuthService.verifyAuthCode(requestDTO.getEmail(), requestDTO.getAuthCode());
    }
}

package kopo.poly.service.impl;

import kopo.poly.dto.EmailAuthResponseDTO;
import kopo.poly.dto.MailDTO;
import kopo.poly.entity.EmailAuthEntity;
import kopo.poly.mapper.IEmailAuthMapper;
import kopo.poly.service.IMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이메일 인증번호 생성, 발송, 검증, 만료 처리를 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_SENDS_PER_WINDOW = 3;
    private static final int MAX_VERIFY_FAILURES_PER_WINDOW = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(10);

    private final IEmailAuthMapper emailAuthRepository;
    private final IMailService mailService;
    private final ConcurrentHashMap<String, RateWindow> sendWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateWindow> verifyFailureWindows = new ConcurrentHashMap<>();

    public EmailAuthResponseDTO sendAuthCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        validateEmail(normalizedEmail);
        enforceRateLimit(sendWindows, normalizedEmail, MAX_SENDS_PER_WINDOW,
                "인증번호 요청 횟수가 너무 많습니다. 잠시 후 다시 시도해 주세요.");

        String authCode = generateAuthCode();
        String expiresAt = LocalDateTime.now().plusMinutes(5).format(DATE_TIME_FORMATTER);

        emailAuthRepository.expireAllByEmail(normalizedEmail);

        EmailAuthEntity entity = new EmailAuthEntity();
        entity.setEmail(normalizedEmail);
        entity.setAuthCode(authCode);
        entity.setVerified("N");
        entity.setExpiresAt(expiresAt);
        emailAuthRepository.insertEmailAuth(entity);

        sendAuthCodeMail(normalizedEmail, authCode);

        EmailAuthResponseDTO response = new EmailAuthResponseDTO(true, "인증번호가 이메일로 발송되었습니다.");
        response.setEmail(normalizedEmail);
        response.setVerified("N");
        return response;
    }

    public EmailAuthResponseDTO verifyAuthCode(String email, String authCode) {
        String normalizedEmail = normalizeEmail(email);

        if (!StringUtils.hasText(normalizedEmail)) {
            return new EmailAuthResponseDTO(false, "이메일을 입력해 주세요.");
        }
        if (!StringUtils.hasText(authCode)) {
            return new EmailAuthResponseDTO(false, "인증번호를 입력해 주세요.");
        }

        EmailAuthEntity latest = emailAuthRepository.selectLatestByEmail(normalizedEmail);
        if (latest == null) {
            return new EmailAuthResponseDTO(false, "이메일 인증 요청 정보가 없습니다.");
        }

        LocalDateTime expiresAt = parseDateTime(latest.getExpiresAt());
        if (LocalDateTime.now().isAfter(expiresAt)) {
            return new EmailAuthResponseDTO(false, "인증 시간이 만료되었습니다. 인증번호를 다시 요청해 주세요.");
        }

        if (!authCode.equals(latest.getAuthCode())) {
            if (isRateLimited(verifyFailureWindows, normalizedEmail, MAX_VERIFY_FAILURES_PER_WINDOW)) {
                return new EmailAuthResponseDTO(false, "인증 실패 횟수가 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            }
            return new EmailAuthResponseDTO(false, "인증번호가 일치하지 않습니다.");
        }

        emailAuthRepository.markVerified(latest.getId());
        verifyFailureWindows.remove(normalizedEmail);
        EmailAuthResponseDTO response = new EmailAuthResponseDTO(true, "이메일 인증이 완료되었습니다.");
        response.setEmail(normalizedEmail);
        response.setVerified("Y");
        return response;
    }

    public boolean isEmailVerified(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail)) {
            return false;
        }

        EmailAuthEntity latest = emailAuthRepository.selectLatestByEmail(normalizedEmail);
        if (latest == null) {
            return false;
        }

        if (!"Y".equalsIgnoreCase(latest.getVerified())) {
            return false;
        }

        LocalDateTime expiresAt = parseDateTime(latest.getExpiresAt());
        return !LocalDateTime.now().isAfter(expiresAt);
    }

    public void expireEmailVerification(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (StringUtils.hasText(normalizedEmail)) {
            emailAuthRepository.expireAllByEmail(normalizedEmail);
            verifyFailureWindows.remove(normalizedEmail);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    private void sendAuthCodeMail(String email, String authCode) {
        try {
            MailDTO mailDTO = new MailDTO();
            mailDTO.setToMail(email);
            mailDTO.setTitle("[DeepScan] 이메일 인증번호");
            mailDTO.setContents(buildMailContents(authCode));

            mailService.doSendMail(mailDTO);
        } catch (MailAuthenticationException e) {
            emailAuthRepository.expireAllByEmail(email);
            log.error("SMTP authentication failed while sending email auth code. email={}", email, e);
            throw e;
        } catch (IllegalStateException e) {
            emailAuthRepository.expireAllByEmail(email);
            log.error("Mail configuration is invalid for email auth send. email={}", email, e);
            throw e;
        } catch (Exception e) {
            emailAuthRepository.expireAllByEmail(email);
            log.error("Failed to send email auth code mail. email={}", email, e);
            throw new IllegalStateException("인증번호 메일 발송에 실패했습니다.", e);
        }
    }

    private String buildMailContents(String authCode) {
        return """
                <div style="max-width:560px;margin:0 auto;padding:32px;background:#f8fafc;font-family:'Malgun Gothic',sans-serif;">
                    <div style="background:#0f172a;border-radius:24px;padding:32px;color:#ffffff;">
                        <div style="display:inline-block;padding:8px 14px;border-radius:999px;background:#1d4ed8;font-size:13px;font-weight:700;">
                            DeepScan 이메일 인증
                        </div>
                        <h1 style="margin:20px 0 12px;font-size:28px;line-height:1.3;">인증번호를 확인해 주세요</h1>
                        <p style="margin:0 0 24px;font-size:15px;line-height:1.7;color:#cbd5e1;">
                            아래 인증번호를 화면에 입력하면 이메일 인증이 완료됩니다.
                        </p>
                        <div style="padding:24px;border-radius:20px;background:#ffffff;color:#0f172a;text-align:center;">
                            <div style="font-size:14px;color:#475569;margin-bottom:10px;">인증번호</div>
                            <div style="font-size:36px;font-weight:800;letter-spacing:8px;">%s</div>
                        </div>
                        <p style="margin:24px 0 0;font-size:14px;line-height:1.7;color:#cbd5e1;">
                            인증번호는 5분 동안 유효합니다. 직접 요청하지 않았다면 이 메일을 무시해 주세요.
                        </p>
                    </div>
                </div>
                """.formatted(authCode);
    }

    private String generateAuthCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private void enforceRateLimit(ConcurrentHashMap<String, RateWindow> windows,
                                  String key,
                                  int maxAttempts,
                                  String message) {
        if (isRateLimited(windows, key, maxAttempts)) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean isRateLimited(ConcurrentHashMap<String, RateWindow> windows, String key, int maxAttempts) {
        LocalDateTime now = LocalDateTime.now();
        RateWindow window = windows.compute(key, (ignored, current) -> {
            if (current == null || now.isAfter(current.resetAt())) {
                return new RateWindow(1, now.plus(RATE_LIMIT_WINDOW));
            }
            return new RateWindow(current.count() + 1, current.resetAt());
        });
        return window.count() > maxAttempts;
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(value.replace("T", " ").substring(0, 19), DATE_TIME_FORMATTER);
        }
    }

    private record RateWindow(int count, LocalDateTime resetAt) {
    }
}

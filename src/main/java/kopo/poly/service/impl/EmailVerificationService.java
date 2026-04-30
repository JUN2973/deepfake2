package kopo.poly.service.impl;

import kopo.poly.dto.MailDTO;
import kopo.poly.mapper.IEmailVerificationMapper;
import kopo.poly.service.IEmailVerificationService;
import kopo.poly.service.IMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Random;

/**
 * 회원가입 완료 등 안내 메일 발송을 담당하는 서비스다.
 */
@Service
public class EmailVerificationService implements IEmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final IEmailVerificationMapper mapper;
    private final IMailService mailService;

    @Value("${app.auth.devVerificationCode:}")
    private String devVerificationCode;

    private final Random random = new Random();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EmailVerificationService(IEmailVerificationMapper mapper, IMailService mailService) {
        this.mapper = mapper;
        this.mailService = mailService;
    }

    @Override
    public void sendCode(String email) throws Exception {
        String code = String.format("%06d", random.nextInt(1_000_000));
        String codeHash = sha256(code);
        String expiresAt = LocalDateTime.now().plusMinutes(10).format(fmt);

        mapper.upsertCode(email, codeHash, expiresAt);

        MailDTO pDTO = new MailDTO();
        pDTO.setToMail(email);
        pDTO.setTitle("[DeepScan] 이메일 인증 코드");
        pDTO.setContents("""
                <div style="font-family:Arial,sans-serif; line-height:1.6;">
                  <h2>DeepScan 이메일 인증</h2>
                  <p>아래 인증 코드를 입력하면 회원가입을 계속 진행할 수 있습니다.</p>
                  <div style="font-size:28px; font-weight:700; letter-spacing:6px; padding:12px 16px; background:#f5f3ff; border:1px solid #e9d5ff; display:inline-block; border-radius:10px;">
                    %s
                  </div>
                  <p style="margin-top:16px;color:#64748b;">인증 코드는 10분 동안 유효합니다.</p>
                </div>
                """.formatted(code));

        try {
            mailService.doSendMail(pDTO);
        } catch (Exception e) {
            log.warn("Email delivery failed for {}. Use verification code: {}", email, code, e);
        }
    }

    @Override
    public boolean verifyCode(String email, String code) throws Exception {
        if (devVerificationCode != null && !devVerificationCode.isBlank() && devVerificationCode.equals(code)) {
            mapper.markVerified(email);
            log.info("Email verification bypassed with development code for {}", email);
            return true;
        }

        Map<String, Object> row = mapper.selectByEmail(email);
        if (row == null) {
            log.warn("No email verification row found for {}", email);
            return false;
        }

        int attempt = Integer.parseInt(String.valueOf(row.get("attemptCount")));
        if (attempt >= 5) {
            log.warn("Email verification blocked by attempt limit. email={}, attempts={}", email, attempt);
            return false;
        }

        String expiresAtStr = String.valueOf(row.get("expiresAt"));
        LocalDateTime expiresAt = parseDateTime(expiresAtStr);
        if (LocalDateTime.now().isAfter(expiresAt)) {
            log.warn("Email verification code expired. email={}, expiresAt={}", email, expiresAtStr);
            return false;
        }

        String codeHash = String.valueOf(row.get("codeHash"));
        String inputHash = sha256(code);

        if (!inputHash.equalsIgnoreCase(codeHash)) {
            mapper.increaseAttempt(email);
            log.warn("Email verification code mismatch. email={}, inputHash={}, storedHash={}, expiresAt={}",
                    email, inputHash, codeHash, expiresAtStr);
            return false;
        }

        mapper.markVerified(email);
        log.info("Email verification succeeded for {}", email);
        return true;
    }

    @Override
    public boolean isVerified(String email) {
        Map<String, Object> row = mapper.selectByEmail(email);
        if (row == null) {
            return false;
        }
        return "1".equals(String.valueOf(row.get("verified")));
    }

    @Override
    public void sendSignupCompletedMail(String email, String name) throws Exception {
        MailDTO pDTO = new MailDTO();
        pDTO.setToMail(email);
        pDTO.setTitle("[DeepScan] 회원가입이 완료되었습니다.");
        pDTO.setContents("""
                <div style="font-family:Arial,sans-serif; line-height:1.6;">
                  <h2>DeepScan 회원가입 완료</h2>
                  <p><strong>%s</strong>님, 회원가입이 정상적으로 완료되었습니다.</p>
                  <p>이제 DeepScan 서비스를 이용하실 수 있습니다.</p>
                </div>
                """.formatted(name));
        mailService.doSendMail(pDTO);
    }

    private String sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, fmt);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
        }
    }
}

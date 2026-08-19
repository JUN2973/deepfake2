package kopo.poly.service.impl;

import jakarta.mail.internet.MimeMessage;
import kopo.poly.dto.MailDTO;
import kopo.poly.service.IMailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Spring JavaMailSender로 이메일을 발송한다.
 */
@Service
public class MailService implements IMailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromMail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void doSendMail(MailDTO pDTO) throws Exception {
        validateMailConfiguration();

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(StringUtils.hasText(fromMail) ? fromMail : mailUsername);
        helper.setTo(pDTO.getToMail());
        helper.setSubject(pDTO.getTitle());
        helper.setText(pDTO.getContents(), true);

        mailSender.send(message);
    }

    private void validateMailConfiguration() {
        if (!StringUtils.hasText(mailUsername) || !StringUtils.hasText(mailPassword)) {
            throw new IllegalStateException("메일 발송 계정이 설정되지 않았습니다. MAIL_USERNAME과 MAIL_PASSWORD를 설정해 주세요.");
        }
    }
}

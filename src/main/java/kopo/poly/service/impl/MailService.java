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
 * Spring JavaMailSender로 실제 이메일을 발송하는 서비스 구현체다.
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
            throw new IllegalStateException("Mail is not configured. Set MAIL_USERNAME and MAIL_PASSWORD.");
        }
    }
}

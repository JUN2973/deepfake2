package kopo.poly.service;

import kopo.poly.dto.MailDTO;

/**
 * 메일 발송 기능의 서비스 계약을 정의한다.
 */
public interface IMailService {
    void doSendMail(MailDTO pDTO) throws Exception;
}

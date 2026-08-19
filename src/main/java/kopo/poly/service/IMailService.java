package kopo.poly.service;


/**
 * 체크리스트 기준 주석: 설계/API 연동 설계: 컨트롤러와 구현체 사이의 서비스 계약을 정의한다.
 */
import kopo.poly.dto.MailDTO;

/**
 * 메일 발송 기능의 서비스 계약을 정의한다.
 */
public interface IMailService {
    void doSendMail(MailDTO pDTO) throws Exception;
}

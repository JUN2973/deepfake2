package kopo.poly.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
@Getter
@Setter
public class MailDTO {
    private String toMail;
    private String title;
    private String contents;
}

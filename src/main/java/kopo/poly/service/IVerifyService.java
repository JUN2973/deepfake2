package kopo.poly.service;

import kopo.poly.dto.VerifyDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 검증 생성, 조회, 삭제 기능의 서비스 계약을 정의한다.
 */
public interface IVerifyService {
    VerifyDTO createVerification(MultipartFile file) throws Exception;
    VerifyDTO createVerification(MultipartFile file, Long userId) throws Exception;
    VerifyDTO getOne(Long id);
    boolean deleteVerification(Long id, Long userId);
}

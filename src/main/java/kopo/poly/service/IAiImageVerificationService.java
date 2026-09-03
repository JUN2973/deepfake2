package kopo.poly.service;

import kopo.poly.dto.AiImageVerificationResponseDTO;
import kopo.poly.dto.VerifyDTO;

public interface IAiImageVerificationService {
    AiImageVerificationResponseDTO verify(VerifyDTO verification);
    AiImageVerificationResponseDTO getLatest(Long verificationId);
}

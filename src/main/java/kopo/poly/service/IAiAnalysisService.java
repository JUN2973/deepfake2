package kopo.poly.service;

import kopo.poly.dto.AiAnalysisRequestDTO;
import kopo.poly.dto.AiAnalysisResponseDTO;
import kopo.poly.dto.VerifyDTO;

public interface IAiAnalysisService {

    AiAnalysisResponseDTO analyze(AiAnalysisRequestDTO request, VerifyDTO verification);
    AiAnalysisResponseDTO getLatest(Long verificationId);
}

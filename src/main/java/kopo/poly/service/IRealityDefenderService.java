package kopo.poly.service;

import kopo.poly.dto.DetectionResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Reality Defender 분석과 이력 조회 기능의 서비스 계약을 정의한다.
 */
public interface IRealityDefenderService {

    DetectionResultDto analyzeImage(MultipartFile file, Long userId) throws Exception;

    DetectionResultDto getDetectionResult(Long id) throws Exception;

    List<DetectionResultDto> getDetectionHistory(Long userId) throws Exception;
}

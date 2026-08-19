package kopo.poly.service;


/**
 * 체크리스트 기준 주석: 설계/API 연동 설계: 컨트롤러와 구현체 사이의 서비스 계약을 정의한다.
 */
import kopo.poly.dto.DetectionResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Reality Defender 분석과 이력 조회 기능의 서비스 계약을 정의한다.
 */
public interface IRealityDefenderService {

    DetectionResultDTO analyzeImage(MultipartFile file, Long userId) throws Exception;

    DetectionResultDTO getDetectionResult(Long id) throws Exception;

    List<DetectionResultDTO> getDetectionHistory(Long userId) throws Exception;
}

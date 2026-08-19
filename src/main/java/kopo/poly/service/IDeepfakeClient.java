package kopo.poly.service;


/**
 * 체크리스트 기준 주석: 설계/API 연동 설계: 컨트롤러와 구현체 사이의 서비스 계약을 정의한다.
 */
import java.nio.file.Path;
import kopo.poly.dto.DeepfakeResultDTO;

/**
 * 딥페이크 분석기 구현체가 따라야 하는 공통 분석 인터페이스다.
 */
public interface IDeepfakeClient {
    DeepfakeResultDTO analyze(Path savedFilePath) throws Exception;
}
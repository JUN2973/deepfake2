package kopo.poly.service;

import java.nio.file.Path;
import kopo.poly.dto.DeepfakeResultDTO;

/**
 * 딥페이크 분석기 구현체가 따라야 하는 공통 분석 인터페이스다.
 */
public interface IDeepfakeClient {
    DeepfakeResultDTO analyze(Path savedFilePath) throws Exception;
}
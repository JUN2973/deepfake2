package kopo.poly.service;


/**
 * 체크리스트 기준 주석: 설계/API 연동 설계: 컨트롤러와 구현체 사이의 서비스 계약을 정의한다.
 */
import kopo.poly.dto.DetailDTO;

/**
 * 분석 상세 조회 기능의 서비스 계약을 정의한다.
 */
public interface IDetailService {
    DetailDTO getDetail(Long id);
}
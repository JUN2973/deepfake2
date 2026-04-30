package kopo.poly.service;

import kopo.poly.dto.DetailDTO;

/**
 * 분석 상세 조회 기능의 서비스 계약을 정의한다.
 */
public interface IDetailService {
    DetailDTO getDetail(Long id);
}
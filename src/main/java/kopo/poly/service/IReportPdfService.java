package kopo.poly.service;

import kopo.poly.dto.ReportPdfRequestDTO;
import kopo.poly.dto.ReportPdfResponseDTO;

import java.util.List;

/**
 * 신고 제출용 PDF 생성 이력과 다운로드 문서를 제공하는 서비스 계약이다.
 */
public interface IReportPdfService {

    ReportPdfResponseDTO createReport(Long verificationId,
                                      Long userId,
                                      ReportPdfRequestDTO request);

    ReportPdfResponseDTO getReport(Long id, Long userId);

    List<ReportPdfResponseDTO> getReports(Long userId);

    byte[] generatePdf(Long id, Long userId);

    void deleteReport(Long id, Long userId);
}

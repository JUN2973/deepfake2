package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.AiAnalysisResponseDTO;
import kopo.poly.dto.ReportPdfRequestDTO;
import kopo.poly.dto.ReportPdfResponseDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IReportPdfMapper;
import kopo.poly.mapper.IVerifyMapper;
import kopo.poly.service.IAiAnalysisService;
import kopo.poly.service.IAiImageVerificationService;
import kopo.poly.service.IObjectStorageService;
import kopo.poly.service.ReportPdfServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportPdfServiceTest {

    private IReportPdfMapper reportPdfMapper;
    private IVerifyMapper verifyMapper;
    private IAiAnalysisService aiAnalysisService;
    private IAiImageVerificationService aiImageVerificationService;
    private IObjectStorageService objectStorageService;
    private ReportPdfGenerator pdfGenerator;
    private ReportPdfService service;

    @BeforeEach
    void setUp() {
        reportPdfMapper = mock(IReportPdfMapper.class);
        verifyMapper = mock(IVerifyMapper.class);
        aiAnalysisService = mock(IAiAnalysisService.class);
        aiImageVerificationService = mock(IAiImageVerificationService.class);
        objectStorageService = mock(IObjectStorageService.class);
        pdfGenerator = mock(ReportPdfGenerator.class);
        service = new ReportPdfService(
                reportPdfMapper,
                verifyMapper,
                aiAnalysisService,
                aiImageVerificationService,
                objectStorageService,
                new ObjectMapper(),
                pdfGenerator
        );
    }

    @Test
    void createReportStoresSnapshotWithoutCallingGeminiByDefault() {
        when(verifyMapper.selectVerificationByIdAndUserId(7L, 3L)).thenReturn(verification());
        when(reportPdfMapper.insertReport(any())).thenAnswer(setGeneratedId(21L));

        ReportPdfRequestDTO request = validRequest();
        request.setSourceUrl("https://example.com/posts/1");
        ReportPdfResponseDTO result = service.createReport(7L, 3L, request);

        assertThat(result.getId()).isEqualTo(21L);
        assertThat(result.getStatus()).isEqualTo(ReportPdfResponseDTO.STATUS_READY);
        assertThat(result.getDownloadUrl()).isEqualTo("/api/v1/reports/21/download");
        assertThat(result.getOriginalImageIncluded()).isTrue();
        assertThat(result.getHeatmapIncluded()).isTrue();
        verify(aiAnalysisService, never()).analyze(any(), any());
    }

    @Test
    void createReportCallsGeminiOnlyWhenAiDraftIsRequested() {
        when(verifyMapper.selectVerificationByIdAndUserId(7L, 3L)).thenReturn(verification());
        when(reportPdfMapper.insertReport(any())).thenAnswer(setGeneratedId(22L));
        AiAnalysisResponseDTO aiResponse = new AiAnalysisResponseDTO();
        aiResponse.setReportDraft("해당 이미지는 자동 분석에서 추가 확인이 필요한 것으로 나타났습니다.");
        when(aiAnalysisService.analyze(any(), any())).thenReturn(aiResponse);

        ReportPdfRequestDTO request = validRequest();
        request.setIncludeAiReportDraft(true);
        ReportPdfResponseDTO result = service.createReport(7L, 3L, request);

        assertThat(result.getAiReportDraftIncluded()).isTrue();
        assertThat(result.getReportDraft()).contains("추가 확인");
        ArgumentCaptor<kopo.poly.dto.AiAnalysisRequestDTO> captor =
                ArgumentCaptor.forClass(kopo.poly.dto.AiAnalysisRequestDTO.class);
        verify(aiAnalysisService).analyze(captor.capture(), any(VerifyDTO.class));
        assertThat(captor.getValue().getIncludeReportDraft()).isTrue();
        assertThat(captor.getValue().getTaskType()).isEqualTo("CREATE_REPORT_DRAFT");
    }

    @Test
    void createReportRejectsVerificationOwnedByAnotherUser() {
        when(verifyMapper.selectVerificationByIdAndUserId(7L, 3L)).thenReturn(null);

        assertThatThrownBy(() -> service.createReport(7L, 3L, validRequest()))
                .isInstanceOf(ReportPdfServiceException.class)
                .extracting(error -> ((ReportPdfServiceException) error).getCode())
                .isEqualTo("REPORT-4040");
        verify(reportPdfMapper, never()).insertReport(any());
    }

    @Test
    void createReportRejectsUnsupportedSourceUrl() {
        ReportPdfRequestDTO request = validRequest();
        request.setSourceUrl("file:///C:/secret.txt");

        assertThatThrownBy(() -> service.createReport(7L, 3L, request))
                .isInstanceOf(ReportPdfServiceException.class)
                .extracting(error -> ((ReportPdfServiceException) error).getCode())
                .isEqualTo("REPORT-URL");
        verify(verifyMapper, never()).selectVerificationByIdAndUserId(any(), any());
    }

    @Test
    void generatePdfUsesSavedDataWithoutNewAiCall() throws Exception {
        ReportPdfResponseDTO report = savedReport();
        VerifyDTO verification = verification();
        when(reportPdfMapper.selectByIdAndUserId(21L, 3L)).thenReturn(report);
        when(verifyMapper.selectVerificationByIdAndUserId(7L, 3L)).thenReturn(verification);
        when(objectStorageService.readObject("2026/sample.png")).thenReturn(new byte[]{1, 2, 3});
        when(pdfGenerator.generate(any(), any(), any(), any(), any(), any()))
                .thenReturn("%PDF-test".getBytes());

        byte[] result = service.generatePdf(21L, 3L);

        assertThat(new String(result)).startsWith("%PDF");
        verify(aiAnalysisService).getLatest(7L);
        verify(aiAnalysisService, never()).analyze(any(), any());
        verify(pdfGenerator).generate(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getReportsAddsDownloadUrls() {
        ReportPdfResponseDTO report = savedReport();
        when(reportPdfMapper.selectListByUserId(3L)).thenReturn(List.of(report));

        List<ReportPdfResponseDTO> result = service.getReports(3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDownloadUrl()).isEqualTo("/api/v1/reports/21/download");
    }

    private Answer<Integer> setGeneratedId(Long id) {
        return invocation -> {
            ReportPdfResponseDTO report = invocation.getArgument(0);
            report.setId(id);
            return 1;
        };
    }

    private ReportPdfRequestDTO validRequest() {
        ReportPdfRequestDTO request = new ReportPdfRequestDTO();
        request.setReportReason("온라인 게시물에 사용된 이미지의 조작 여부를 확인하고 싶습니다.");
        return request;
    }

    private VerifyDTO verification() {
        VerifyDTO verification = new VerifyDTO();
        verification.setId(7L);
        verification.setUserId(3L);
        verification.setOriginalName("sample.png");
        verification.setObjectKey("2026/sample.png");
        verification.setApiProvider("Reality Defender");
        verification.setVerdict("SUSPICIOUS");
        verification.setScore(0.82);
        verification.setRegDt("2026-09-11 10:00:00");
        return verification;
    }

    private ReportPdfResponseDTO savedReport() {
        ReportPdfResponseDTO report = new ReportPdfResponseDTO();
        report.setId(21L);
        report.setVerificationId(7L);
        report.setUserId(3L);
        report.setReportReason("신고 사유");
        report.setOriginalImageIncluded(true);
        report.setHeatmapIncluded(false);
        report.setFileName("report.pdf");
        report.setContentType(ReportPdfResponseDTO.CONTENT_TYPE_PDF);
        report.setStatus(ReportPdfResponseDTO.STATUS_READY);
        return report;
    }
}

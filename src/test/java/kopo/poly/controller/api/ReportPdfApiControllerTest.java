package kopo.poly.controller.api;

import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.ReportPdfRequestDTO;
import kopo.poly.dto.ReportPdfResponseDTO;
import kopo.poly.service.IReportPdfService;
import kopo.poly.service.ReportPdfServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportPdfApiControllerTest {

    private IReportPdfService reportPdfService;
    private ReportPdfApiController controller;

    @BeforeEach
    void setUp() {
        reportPdfService = mock(IReportPdfService.class);
        controller = new ReportPdfApiController(reportPdfService);
    }

    @Test
    void createUsesLoggedInUserId() {
        ReportPdfRequestDTO request = new ReportPdfRequestDTO();
        request.setReportReason("신고자료를 생성해 주세요.");
        ReportPdfResponseDTO response = response(21L);
        when(reportPdfService.createReport(7L, 3L, request)).thenReturn(response);

        ApiResponse<ReportPdfResponseDTO> result = controller.create(
                7L, request, loggedInSession(3L)
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(response);
        verify(reportPdfService).createReport(7L, 3L, request);
    }

    @Test
    void createRejectsAnonymousUserBeforeCallingService() {
        ReportPdfRequestDTO request = new ReportPdfRequestDTO();

        ApiResponse<ReportPdfResponseDTO> result = controller.create(
                7L, request, new MockHttpSession()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("REPORT-4010");
        verify(reportPdfService, never()).createReport(7L, null, request);
    }

    @Test
    void createMapsServiceValidationError() {
        ReportPdfRequestDTO request = new ReportPdfRequestDTO();
        when(reportPdfService.createReport(7L, 3L, request)).thenThrow(
                new ReportPdfServiceException("REPORT-REASON", "신고 사유를 입력해 주세요.")
        );

        ApiResponse<ReportPdfResponseDTO> result = controller.create(
                7L, request, loggedInSession(3L)
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("REPORT-REASON");
    }

    @Test
    void listReturnsCurrentUsersReports() {
        when(reportPdfService.getReports(3L)).thenReturn(List.of(response(21L), response(22L)));

        ApiResponse<List<ReportPdfResponseDTO>> result = controller.list(loggedInSession(3L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(2);
        verify(reportPdfService).getReports(3L);
    }

    @Test
    void detailReturnsOnlyCurrentUsersReport() {
        ReportPdfResponseDTO response = response(21L);
        when(reportPdfService.getReport(21L, 3L)).thenReturn(response);

        ApiResponse<ReportPdfResponseDTO> result = controller.detail(21L, loggedInSession(3L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(response);
        verify(reportPdfService).getReport(21L, 3L);
    }

    @Test
    void downloadReturnsPdfAttachment() {
        ReportPdfResponseDTO report = response(21L);
        report.setFileName("딥페이크-신고자료.pdf");
        byte[] pdf = "%PDF-test".getBytes(StandardCharsets.US_ASCII);
        when(reportPdfService.getReport(21L, 3L)).thenReturn(report);
        when(reportPdfService.generatePdf(21L, 3L)).thenReturn(pdf);

        ResponseEntity<?> result = controller.download(21L, loggedInSession(3L));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(result.getHeaders().getContentLength()).isEqualTo(pdf.length);
        assertThat(result.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment").contains(".pdf");
        assertThat(result.getHeaders().getCacheControl()).contains("no-store");
        assertThat(result.getBody()).isEqualTo(pdf);
    }

    @Test
    void downloadRejectsAnonymousUserWithHttp401() {
        ResponseEntity<?> result = controller.download(21L, new MockHttpSession());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(result.getBody()).isInstanceOf(ApiResponse.class);
        verify(reportPdfService, never()).generatePdf(21L, null);
    }

    @Test
    void downloadMapsMissingReportToHttp404() {
        when(reportPdfService.getReport(99L, 3L)).thenThrow(
                new ReportPdfServiceException("REPORT-4041", "신고자료를 찾을 수 없습니다.")
        );

        ResponseEntity<?> result = controller.download(99L, loggedInSession(3L));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void deleteCallsServiceForCurrentUser() {
        ApiResponse<Boolean> result = controller.delete(21L, loggedInSession(3L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isTrue();
        verify(reportPdfService).deleteReport(21L, 3L);
    }

    private MockHttpSession loggedInSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", userId);
        return session;
    }

    private ReportPdfResponseDTO response(Long id) {
        ReportPdfResponseDTO response = new ReportPdfResponseDTO();
        response.setId(id);
        response.setVerificationId(7L);
        response.setUserId(3L);
        response.setReportReason("신고 사유");
        response.setFileName("deepscan-report.pdf");
        response.setContentType(ReportPdfResponseDTO.CONTENT_TYPE_PDF);
        response.setStatus(ReportPdfResponseDTO.STATUS_READY);
        return response;
    }
}

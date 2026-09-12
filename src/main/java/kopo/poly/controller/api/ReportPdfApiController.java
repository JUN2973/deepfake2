package kopo.poly.controller.api;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.ApiResponse;
import kopo.poly.dto.ReportPdfRequestDTO;
import kopo.poly.dto.ReportPdfResponseDTO;
import kopo.poly.service.IReportPdfService;
import kopo.poly.service.ReportPdfServiceException;
import kopo.poly.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 로그인 사용자의 신고 제출용 PDF 생성 이력과 다운로드 API를 제공한다.
 */
@RestController
@RequestMapping("/api/v1")
public class ReportPdfApiController {

    private static final Logger log = LoggerFactory.getLogger(ReportPdfApiController.class);

    private final IReportPdfService reportPdfService;

    public ReportPdfApiController(IReportPdfService reportPdfService) {
        this.reportPdfService = reportPdfService;
    }

    @PostMapping("/verifications/{verificationId}/reports")
    public ApiResponse<ReportPdfResponseDTO> create(
            @PathVariable Long verificationId,
            @RequestBody(required = false) ReportPdfRequestDTO request,
            HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("REPORT-4010", "로그인 후 신고자료를 생성할 수 있습니다.");
        }

        try {
            return ApiResponse.ok(reportPdfService.createReport(verificationId, userId, request));
        } catch (ReportPdfServiceException e) {
            log.warn("Report PDF create failed. code={}, verificationId={}, userId={}",
                    e.getCode(), verificationId, userId);
            return ApiResponse.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected report PDF create error. verificationId={}, userId={}",
                    verificationId, userId, e);
            return ApiResponse.fail("REPORT-5000", "신고자료를 생성하는 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/reports")
    public ApiResponse<List<ReportPdfResponseDTO>> list(HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("REPORT-4010", "로그인 후 신고자료를 확인할 수 있습니다.");
        }

        try {
            return ApiResponse.ok(reportPdfService.getReports(userId));
        } catch (ReportPdfServiceException e) {
            return ApiResponse.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected report PDF list error. userId={}", userId, e);
            return ApiResponse.fail("REPORT-5001", "신고자료 목록을 불러오지 못했습니다.");
        }
    }

    @GetMapping("/reports/{id}")
    public ApiResponse<ReportPdfResponseDTO> detail(
            @PathVariable Long id,
            HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("REPORT-4010", "로그인 후 신고자료를 확인할 수 있습니다.");
        }

        try {
            return ApiResponse.ok(reportPdfService.getReport(id, userId));
        } catch (ReportPdfServiceException e) {
            return ApiResponse.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected report PDF detail error. reportId={}, userId={}", id, userId, e);
            return ApiResponse.fail("REPORT-5002", "신고자료를 불러오지 못했습니다.");
        }
    }

    @GetMapping(value = "/reports/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> download(
            @PathVariable Long id,
            HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return downloadError(HttpStatus.UNAUTHORIZED, "REPORT-4010", "로그인 후 PDF를 다운로드할 수 있습니다.");
        }

        try {
            ReportPdfResponseDTO report = reportPdfService.getReport(id, userId);
            byte[] pdf = reportPdfService.generatePdf(id, userId);
            String fileName = safeFileName(report.getFileName(), id);
            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdf.length)
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(pdf);
        } catch (ReportPdfServiceException e) {
            log.warn("Report PDF download failed. code={}, reportId={}, userId={}",
                    e.getCode(), id, userId);
            return downloadError(downloadStatus(e.getCode()), e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected report PDF download error. reportId={}, userId={}", id, userId, e);
            return downloadError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "REPORT-5003", "PDF 다운로드를 준비하지 못했습니다.");
        }
    }

    @DeleteMapping("/reports/{id}")
    public ApiResponse<Boolean> delete(
            @PathVariable Long id,
            HttpSession session) {
        Long userId = SessionUtil.getUserId(session);
        if (userId == null) {
            return ApiResponse.fail("REPORT-4010", "로그인 후 신고자료를 삭제할 수 있습니다.");
        }

        try {
            reportPdfService.deleteReport(id, userId);
            return ApiResponse.ok(true);
        } catch (ReportPdfServiceException e) {
            return ApiResponse.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected report PDF delete error. reportId={}, userId={}", id, userId, e);
            return ApiResponse.fail("REPORT-5004", "신고자료를 삭제하지 못했습니다.");
        }
    }

    private ResponseEntity<ApiResponse<Object>> downloadError(HttpStatus status,
                                                               String code,
                                                               String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.fail(code, message));
    }

    private HttpStatus downloadStatus(String code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (code.contains("401") || code.endsWith("AUTH")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code.contains("404")) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.contains("400") || code.endsWith("REASON") || code.endsWith("URL")) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String safeFileName(String fileName, Long reportId) {
        String fallback = "deepscan-report-" + reportId + ".pdf";
        if (fileName == null || fileName.isBlank()) {
            return fallback;
        }
        String safe = fileName.replace("\r", "").replace("\n", "").trim();
        return safe.toLowerCase().endsWith(".pdf") ? safe : safe + ".pdf";
    }
}

package kopo.poly.service.impl;

import kopo.poly.dto.AiAnalysisResponseDTO;
import kopo.poly.dto.AiImageVerificationResponseDTO;
import kopo.poly.dto.ReportPdfResponseDTO;
import kopo.poly.dto.VerifyDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReportPdfGeneratorTest {

    @Test
    void generatesReadableKoreanPdf() throws Exception {
        Path fontPath = Path.of("C:/Windows/Fonts/malgun.ttf");
        assumeTrue(Files.isRegularFile(fontPath), "Korean test font is unavailable");

        ReportPdfGenerator generator = new ReportPdfGenerator(fontPath.toString());
        byte[] pdf = generator.generate(
                report(),
                verification(),
                explanation(),
                imageReview(),
                null,
                null
        );

        Path previewPath = Path.of("build/reports/pdf/report-pdf-preview.pdf");
        Files.createDirectories(previewPath.getParent());
        Files.write(previewPath, pdf);

        assertThat(pdf).startsWith("%PDF-".getBytes());
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("딥페이크 분석 신고 참고자료");
            assertThat(text).contains("신고 제출 문구");
            assertThat(text).contains("판정 재검토를 요청합니다");
        }
    }

    private ReportPdfResponseDTO report() {
        ReportPdfResponseDTO report = new ReportPdfResponseDTO();
        report.setId(21L);
        report.setVerificationId(7L);
        report.setUserId(3L);
        report.setReportReason("공개된 이미지의 판정 재검토를 요청합니다.");
        report.setReportDraft("해당 이미지에서 조작 의심 신호가 확인되어 사실관계 확인을 요청합니다.");
        report.setOriginalName("sample.png");
        report.setVerdict("SUSPICIOUS");
        report.setScore(0.82);
        report.setRegDt("2026-09-11 10:00:00");
        return report;
    }

    private VerifyDTO verification() {
        VerifyDTO verification = new VerifyDTO();
        verification.setApiProvider("Reality Defender");
        verification.setRegDt("2026-09-11 09:30:00");
        return verification;
    }

    private AiAnalysisResponseDTO explanation() {
        AiAnalysisResponseDTO result = new AiAnalysisResponseDTO();
        result.setSummary("자동 분석에서 추가 확인이 필요한 신호가 감지되었습니다.");
        result.setExplanation("이 결과만으로 이미지 조작 여부를 확정할 수 없습니다.");
        result.setActionGuide("원본 출처와 게시 맥락을 함께 확인하세요.");
        return result;
    }

    private AiImageVerificationResponseDTO imageReview() {
        AiImageVerificationResponseDTO result = new AiImageVerificationResponseDTO();
        result.setCrossCheckStatus("AGREES");
        result.setCombinedConclusion("1차 판독과 2차 검토가 대체로 일치합니다.");
        result.setVisualSummary("얼굴 경계에서 일부 불규칙한 패턴이 관찰됩니다.");
        result.setVisualIndicators(List.of("얼굴 경계의 질감 차이", "배경 압축 흔적"));
        return result;
    }
}

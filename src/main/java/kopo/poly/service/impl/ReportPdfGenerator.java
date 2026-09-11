package kopo.poly.service.impl;

import kopo.poly.dto.AiAnalysisResponseDTO;
import kopo.poly.dto.AiImageVerificationResponseDTO;
import kopo.poly.dto.ReportPdfResponseDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.service.ReportPdfServiceException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 신고자료 데이터를 A4 PDF 문서로 렌더링한다.
 */
@Component
public class ReportPdfGenerator {

    private static final float PAGE_MARGIN = 52f;
    private static final float CONTENT_WIDTH = PDRectangle.A4.getWidth() - PAGE_MARGIN * 2;
    private static final int MAX_IMAGE_PIXELS = 25_000_000;
    private static final List<String> DEFAULT_FONT_PATHS = List.of(
            "C:/Windows/Fonts/malgun.ttf",
            "C:/Windows/Fonts/NanumGothic.ttf",
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/noto/NotoSansKR-Regular.ttf",
            "/Library/Fonts/AppleGothic.ttf"
    );

    private final String configuredFontPath;

    public ReportPdfGenerator(@Value("${app.report.pdf.font-path:}") String configuredFontPath) {
        this.configuredFontPath = configuredFontPath == null ? "" : configuredFontPath.trim();
    }

    public byte[] generate(ReportPdfResponseDTO report,
                           VerifyDTO verification,
                           AiAnalysisResponseDTO explanation,
                           AiImageVerificationResponseDTO imageReview,
                           byte[] originalImage,
                           byte[] heatmapImage) {
        try (PDDocument document = new PDDocument()) {
            PDFont font = loadFont(document);
            PdfCanvas canvas = new PdfCanvas(document, font);

            canvas.documentTitle("딥페이크 분석 신고 참고자료");
            canvas.paragraph("본 문서는 DeepScan 자동 분석 결과를 신고 기관에 설명하기 위한 참고자료입니다. "
                    + "조작 여부를 확정하는 공식 감정서가 아닙니다.", 10f, Color.AMBER);

            canvas.section("1. 문서 및 신고 정보");
            canvas.keyValue("보고서 번호", value(report.getId()));
            canvas.keyValue("검증 번호", value(report.getVerificationId()));
            canvas.keyValue("생성 일시", value(report.getRegDt()));
            canvas.keyValue("원본 파일명", value(report.getOriginalName()));
            canvas.keyValue("콘텐츠 출처", blankFallback(report.getSourceUrl(), "입력되지 않음"));
            canvas.label("신고 사유");
            canvas.paragraph(blankFallback(report.getReportReason(), "입력된 신고 사유가 없습니다."), 10.5f, Color.BODY);

            canvas.section("2. 자동 분석 결과");
            canvas.keyValue("분석 제공자", blankFallback(verification.getApiProvider(), "Reality Defender"));
            canvas.keyValue("판정", verdictLabel(report.getVerdict()));
            canvas.keyValue("분석 점수", scoreLabel(report.getScore()));
            canvas.keyValue("분석 일시", value(verification.getRegDt()));

            if (originalImage != null) {
                canvas.section("3. 원본 이미지");
                canvas.image(originalImage, "원본 이미지");
            }
            if (heatmapImage != null) {
                canvas.section(originalImage == null ? "3. 히트맵 참고 이미지" : "4. 히트맵 참고 이미지");
                canvas.image(heatmapImage, "히트맵 참고 이미지");
                canvas.paragraph("히트맵은 픽셀 패턴 차이를 보여주는 참고 시각화이며 최종 판정 기준이 아닙니다.",
                        9f, Color.MUTED);
            }

            int nextSection = 3 + (originalImage == null ? 0 : 1) + (heatmapImage == null ? 0 : 1);
            canvas.section(nextSection++ + ". Gemini 상세 해설");
            if (explanation == null) {
                canvas.paragraph("저장된 Gemini 상세 해설이 없습니다.", 10f, Color.MUTED);
            } else {
                canvas.label("종합 해석");
                canvas.paragraph(blankFallback(explanation.getSummary(), "요약 정보가 없습니다."), 10.5f, Color.BODY);
                canvas.label("상세 설명");
                canvas.paragraph(blankFallback(explanation.getExplanation(), "상세 설명이 없습니다."), 10f, Color.BODY);
                canvas.label("추가 확인 방법");
                canvas.paragraph(blankFallback(explanation.getActionGuide(), "추가 확인 방법이 없습니다."), 10f, Color.BODY);
            }

            canvas.section(nextSection++ + ". AI 이미지 2차 검증");
            if (imageReview == null) {
                canvas.paragraph("저장된 AI 이미지 2차 검증 결과가 없습니다.", 10f, Color.MUTED);
            } else {
                canvas.keyValue("교차 검증", crossCheckLabel(imageReview.getCrossCheckStatus()));
                canvas.label("결론");
                canvas.paragraph(blankFallback(imageReview.getCombinedConclusion(), "결론 정보가 없습니다."),
                        10.5f, Color.BODY);
                canvas.label("시각적 관찰");
                canvas.paragraph(blankFallback(imageReview.getVisualSummary(), "시각적 관찰 정보가 없습니다."),
                        10f, Color.BODY);
                if (imageReview.getVisualIndicators() != null) {
                    for (String indicator : imageReview.getVisualIndicators()) {
                        canvas.bullet(indicator);
                    }
                }
            }

            canvas.section(nextSection + ". 신고 제출 문구");
            String reportDraft = blankFallback(report.getReportDraft(), report.getReportReason());
            canvas.paragraph(blankFallback(reportDraft, "신고 제출 문구가 없습니다."), 10.5f, Color.BODY);

            canvas.section("주의사항");
            canvas.paragraph("자동 분석 결과는 확률적 참고정보입니다. 실제 신고 시 원본 파일, 게시물 주소, "
                    + "게시 일시, 작성자 정보와 피해 내용을 함께 제출하고 수사기관의 안내를 따르세요.",
                    9.5f, Color.AMBER);
            canvas.finish();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (ReportPdfServiceException e) {
            throw e;
        } catch (IOException | IllegalArgumentException e) {
            throw new ReportPdfServiceException("REPORT-PDF", "PDF 문서를 생성하지 못했습니다.", e);
        }
    }

    private PDFont loadFont(PDDocument document) throws IOException {
        List<String> paths = new ArrayList<>();
        if (!configuredFontPath.isBlank()) {
            paths.add(configuredFontPath);
        }
        paths.addAll(DEFAULT_FONT_PATHS);

        for (String pathValue : paths) {
            Path path = Path.of(pathValue);
            if (Files.isRegularFile(path)) {
                try (InputStream input = Files.newInputStream(path)) {
                    return PDType0Font.load(document, input, true);
                }
            }
        }

        try (InputStream input = ReportPdfGenerator.class.getResourceAsStream("/fonts/NotoSansKR-Regular.ttf")) {
            if (input != null) {
                return PDType0Font.load(document, input, true);
            }
        }
        throw new ReportPdfServiceException(
                "REPORT-FONT",
                "PDF 한글 폰트를 찾을 수 없습니다. app.report.pdf.font-path를 설정해 주세요."
        );
    }

    private static String verdictLabel(String verdict) {
        String normalized = verdict == null ? "" : verdict.trim().toUpperCase();
        return switch (normalized) {
            case "SAFE", "REAL", "AUTHENTIC" -> "낮은 조작 의심";
            case "SUSPECT", "SUSPICIOUS" -> "추가 확인 필요";
            case "HIGH_RISK", "FAKE" -> "높은 조작 의심";
            case "NOT_APPLICABLE", "UNABLE_TO_EVALUATE" -> "분석 제한";
            default -> blankFallback(verdict, "판정 정보 없음");
        };
    }

    private static String crossCheckLabel(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        return switch (normalized) {
            case "AGREES" -> "1차 판독과 일치";
            case "DISAGREES" -> "1차 판독과 불일치";
            default -> "판단 보류";
        };
    }

    private static String scoreLabel(Double score) {
        if (score == null || !Double.isFinite(score)) {
            return "점수 정보 없음";
        }
        double normalized = score <= 1 ? score * 100 : score;
        normalized = Math.max(0, Math.min(100, normalized));
        return String.format(java.util.Locale.ROOT, "%.1f%%", normalized);
    }

    private static String value(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String blankFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private enum Color {
        BODY(30, 41, 59),
        MUTED(100, 116, 139),
        AMBER(146, 64, 14);

        private final int red;
        private final int green;
        private final int blue;

        Color(int red, int green, int blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    private static final class PdfCanvas {

        private static final float TOP_Y = PDRectangle.A4.getHeight() - 88f;
        private static final float BOTTOM_Y = 58f;

        private final PDDocument document;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;
        private int pageNumber;

        private PdfCanvas(PDDocument document, PDFont font) throws IOException {
            this.document = document;
            this.font = font;
            addPage();
        }

        private void addPage() throws IOException {
            closePage();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            pageNumber++;

            stream.setNonStrokingColor(new java.awt.Color(15, 23, 42));
            stream.addRect(0, PDRectangle.A4.getHeight() - 56f, PDRectangle.A4.getWidth(), 56f);
            stream.fill();
            drawLine("DeepScan | 신고 참고자료", PAGE_MARGIN, PDRectangle.A4.getHeight() - 35f,
                    10f, 255, 255, 255);
            y = TOP_Y;
        }

        private void documentTitle(String text) throws IOException {
            ensureSpace(44f);
            drawLine(text, PAGE_MARGIN, y, 22f, 15, 23, 42);
            y -= 38f;
        }

        private void section(String text) throws IOException {
            ensureSpace(38f);
            y -= 8f;
            stream.setNonStrokingColor(new java.awt.Color(14, 116, 144));
            stream.addRect(PAGE_MARGIN, y - 6f, 4f, 20f);
            stream.fill();
            drawLine(text, PAGE_MARGIN + 12f, y, 14f, 15, 23, 42);
            y -= 28f;
        }

        private void label(String text) throws IOException {
            ensureSpace(22f);
            drawLine(text, PAGE_MARGIN, y, 9f, 71, 85, 105);
            y -= 16f;
        }

        private void keyValue(String key, String value) throws IOException {
            ensureSpace(22f);
            drawLine(key, PAGE_MARGIN, y, 9.5f, 100, 116, 139);
            List<String> lines = wrap(value, 10.5f, CONTENT_WIDTH - 130f);
            float lineY = y;
            for (String line : lines) {
                drawLine(line, PAGE_MARGIN + 130f, lineY, 10.5f, 30, 41, 59);
                lineY -= 15f;
            }
            y = Math.min(y - 20f, lineY - 5f);
        }

        private void paragraph(String text, float size, Color color) throws IOException {
            List<String> lines = wrap(text, size, CONTENT_WIDTH);
            float leading = size + 5f;
            for (String line : lines) {
                ensureSpace(leading);
                drawLine(line, PAGE_MARGIN, y, size, color.red, color.green, color.blue);
                y -= leading;
            }
            y -= 8f;
        }

        private void bullet(String text) throws IOException {
            String safe = blankFallback(text, "확인된 세부 단서가 없습니다.");
            List<String> lines = wrap(safe, 9.5f, CONTENT_WIDTH - 16f);
            for (int i = 0; i < lines.size(); i++) {
                ensureSpace(15f);
                drawLine(i == 0 ? "-" : "", PAGE_MARGIN, y, 9.5f, 14, 116, 144);
                drawLine(lines.get(i), PAGE_MARGIN + 16f, y, 9.5f, 51, 65, 85);
                y -= 15f;
            }
            y -= 3f;
        }

        private void image(byte[] bytes, String name) throws IOException {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null || (long) source.getWidth() * source.getHeight() > MAX_IMAGE_PIXELS) {
                paragraph(name + "를 PDF에 포함하지 못했습니다.", 9f, Color.MUTED);
                return;
            }

            PDImageXObject image = PDImageXObject.createFromByteArray(document, bytes, name);
            float maxHeight = 280f;
            float scale = Math.min(CONTENT_WIDTH / image.getWidth(), maxHeight / image.getHeight());
            scale = Math.min(scale, 1f);
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            ensureSpace(height + 20f);
            float x = PAGE_MARGIN + (CONTENT_WIDTH - width) / 2f;
            stream.drawImage(image, x, y - height, width, height);
            y -= height + 16f;
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < BOTTOM_Y) {
                addPage();
            }
        }

        private List<String> wrap(String value, float size, float maxWidth) throws IOException {
            String safe = sanitize(value);
            List<String> lines = new ArrayList<>();
            for (String paragraph : safe.replace("\r", "").split("\n", -1)) {
                if (paragraph.isEmpty()) {
                    lines.add("");
                    continue;
                }
                StringBuilder line = new StringBuilder();
                for (int offset = 0; offset < paragraph.length();) {
                    int codePoint = paragraph.codePointAt(offset);
                    String character = new String(Character.toChars(codePoint));
                    String candidate = line + character;
                    if (line.length() > 0 && textWidth(candidate, size) > maxWidth) {
                        lines.add(line.toString().stripTrailing());
                        line.setLength(0);
                        if (!character.isBlank()) {
                            line.append(character);
                        }
                    } else {
                        line.append(character);
                    }
                    offset += Character.charCount(codePoint);
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString().stripTrailing());
                }
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private String sanitize(String value) {
            String source = value == null ? "" : value.replace('\t', ' ');
            StringBuilder result = new StringBuilder(source.length());
            for (int offset = 0; offset < source.length();) {
                int codePoint = source.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                if (codePoint == '\n' || codePoint == '\r') {
                    result.append(character);
                } else if (!Character.isISOControl(codePoint)) {
                    try {
                        font.encode(character);
                        result.append(character);
                    } catch (IOException | IllegalArgumentException e) {
                        result.append('?');
                    }
                }
                offset += Character.charCount(codePoint);
            }
            return result.toString();
        }

        private float textWidth(String text, float size) throws IOException {
            return font.getStringWidth(text) / 1000f * size;
        }

        private void drawLine(String text,
                              float x,
                              float baseline,
                              float size,
                              int red,
                              int green,
                              int blue) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(new java.awt.Color(red, green, blue));
            stream.newLineAtOffset(x, baseline);
            stream.showText(sanitize(text));
            stream.endText();
        }

        private void finish() throws IOException {
            closePage();
        }

        private void closePage() throws IOException {
            if (stream == null) {
                return;
            }
            stream.setStrokingColor(new java.awt.Color(203, 213, 225));
            stream.moveTo(PAGE_MARGIN, 43f);
            stream.lineTo(PDRectangle.A4.getWidth() - PAGE_MARGIN, 43f);
            stream.stroke();
            drawLine("자동 분석 참고자료 | " + pageNumber,
                    PAGE_MARGIN, 27f, 8f, 100, 116, 139);
            stream.close();
            stream = null;
        }
    }
}

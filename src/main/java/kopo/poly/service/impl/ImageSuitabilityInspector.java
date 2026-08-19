package kopo.poly.service.impl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 업로드 이미지가 단일 얼굴 딥페이크 분석에 적합한지 확인한다.
 */
final class ImageSuitabilityInspector {

    ImageSuitability inspect(Path imagePath) {
        try {
            BufferedImage image = ImageIO.read(imagePath.toFile());
            if (image == null) {
                return ImageSuitability.notApplicable("이미지를 읽을 수 없어 분석할 수 없습니다.");
            }

            int width = image.getWidth();
            int height = image.getHeight();
            int minSide = Math.min(width, height);
            double aspectRatio = Math.max(width, height) / (double) Math.max(1, minSide);

            if (width < 220 || height < 220) {
                return ImageSuitability.notApplicable("이미지 해상도가 낮아 신뢰할 수 있는 분석을 진행할 수 없습니다.");
            }

            double blurScore = estimateSharpness(image);
            if (blurScore < 28.0) {
                return ImageSuitability.notApplicable("이미지가 흐릿해 얼굴 특징을 안정적으로 확인할 수 없습니다.");
            }

            SkinRegionStats skinStats = analyzeSkinRegions(image);
            ImageSuitability crowdedSceneSuitability = inspectCrowdedHumanScene(skinStats);
            if (crowdedSceneSuitability.notApplicable()) {
                return crowdedSceneSuitability;
            }

            if (minSide >= 300 || aspectRatio <= 1.65) {
                return ImageSuitability.applicable();
            }

            if (skinStats.skinRatio < 0.006) {
                return ImageSuitability.notApplicable("사람 얼굴로 판단할 수 있는 영역이 부족해 딥페이크 분석 대상이 아닙니다.");
            }

            if (skinStats.largestComponentRatio < 0.018) {
                return ImageSuitability.notApplicable("사람 얼굴로 판단할 수 있는 영역이 부족해 딥페이크 분석 대상이 아닙니다.");
            }

            if (skinStats.componentCount >= 7 && skinStats.largestComponentRatio < 0.035) {
                return ImageSuitability.notApplicable("하나의 얼굴 영역을 확인하기 어려워 딥페이크 분석 대상이 아닙니다.");
            }

            if (skinStats.componentCount >= 4) {
                return ImageSuitability.notApplicable("사람이 너무 많거나 얼굴 영역이 여러 개로 나뉘어 단일 얼굴 기준 분석을 진행할 수 없습니다.");
            }

            if (minSide < 300 && aspectRatio > 1.65) {
                return ImageSuitability.notApplicable("얼굴이 너무 작게 잡혀 신뢰할 수 있는 분석을 진행할 수 없습니다.");
            }

            return ImageSuitability.applicable();
        } catch (Exception e) {
            return ImageSuitability.notApplicable("이미지 상태를 확인할 수 없어 신뢰할 수 있는 분석을 진행할 수 없습니다.");
        }
    }

    private ImageSuitability inspectCrowdedHumanScene(SkinRegionStats skinStats) {
        if (skinStats.componentCount >= 10
                && skinStats.skinRatio >= 0.012
                && skinStats.largestComponentRatio < 0.12) {
            return ImageSuitability.notApplicable("사람이 너무 많거나 얼굴 영역이 여러 개로 나뉘어 단일 얼굴 기준 분석을 진행할 수 없습니다.");
        }

        if (skinStats.componentCount >= 6
                && skinStats.skinRatio >= 0.035
                && skinStats.largestComponentRatio < 0.08) {
            return ImageSuitability.notApplicable("사람이 너무 많거나 얼굴 영역이 여러 개로 나뉘어 단일 얼굴 기준 분석을 진행할 수 없습니다.");
        }

        return ImageSuitability.applicable();
    }

    private double estimateSharpness(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int step = Math.max(1, Math.max(width, height) / 450);
        double sum = 0.0;
        double sumSquares = 0.0;
        int count = 0;

        for (int y = step; y < height - step; y += step) {
            for (int x = step; x < width - step; x += step) {
                int center = luminance(image.getRGB(x, y));
                int laplacian = (4 * center)
                        - luminance(image.getRGB(x - step, y))
                        - luminance(image.getRGB(x + step, y))
                        - luminance(image.getRGB(x, y - step))
                        - luminance(image.getRGB(x, y + step));
                sum += laplacian;
                sumSquares += laplacian * laplacian;
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }

        double mean = sum / count;
        return (sumSquares / count) - (mean * mean);
    }

    private int luminance(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return (int) Math.round((0.299 * r) + (0.587 * g) + (0.114 * b));
    }

    private SkinRegionStats analyzeSkinRegions(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int scale = Math.max(1, Math.max(width, height) / 180);
        int sampledWidth = Math.max(1, width / scale);
        int sampledHeight = Math.max(1, height / scale);
        boolean[][] skin = new boolean[sampledHeight][sampledWidth];
        boolean[][] visited = new boolean[sampledHeight][sampledWidth];
        int skinPixels = 0;

        for (int y = 0; y < sampledHeight; y++) {
            for (int x = 0; x < sampledWidth; x++) {
                int rgb = image.getRGB(Math.min(width - 1, x * scale), Math.min(height - 1, y * scale));
                skin[y][x] = isSkinLike(rgb);
                if (skin[y][x]) {
                    skinPixels++;
                }
            }
        }

        int totalPixels = sampledWidth * sampledHeight;
        int components = 0;
        int largest = 0;
        int minComponentSize = Math.max(4, totalPixels / 1200);

        for (int y = 0; y < sampledHeight; y++) {
            for (int x = 0; x < sampledWidth; x++) {
                if (!skin[y][x] || visited[y][x]) {
                    continue;
                }

                int size = floodFillSize(skin, visited, x, y, sampledWidth, sampledHeight);
                if (size >= minComponentSize) {
                    components++;
                    largest = Math.max(largest, size);
                }
            }
        }

        return new SkinRegionStats(
                skinPixels / (double) Math.max(1, totalPixels),
                largest / (double) Math.max(1, totalPixels),
                components
        );
    }

    private int floodFillSize(boolean[][] skin, boolean[][] visited, int startX, int startY, int width, int height) {
        int size = 0;
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int[] point = queue.poll();
            size++;
            for (int i = 0; i < 4; i++) {
                int nx = point[0] + dx[i];
                int ny = point[1] + dy[i];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || visited[ny][nx] || !skin[ny][nx]) {
                    continue;
                }
                visited[ny][nx] = true;
                queue.add(new int[]{nx, ny});
            }
        }
        return size;
    }

    private boolean isSkinLike(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));

        boolean rgbRule = r > 80 && g > 45 && b > 25 && r > g && r > b && max - min > 15 && Math.abs(r - g) > 8;
        double cb = 128 - (0.168736 * r) - (0.331264 * g) + (0.5 * b);
        double cr = 128 + (0.5 * r) - (0.418688 * g) - (0.081312 * b);
        boolean yCbCrRule = cb >= 77 && cb <= 135 && cr >= 133 && cr <= 180;
        return rgbRule && yCbCrRule;
    }

    record ImageSuitability(boolean notApplicable, String reason) {
        private static ImageSuitability applicable() {
            return new ImageSuitability(false, null);
        }

        private static ImageSuitability notApplicable(String reason) {
            return new ImageSuitability(true, reason);
        }
    }

    private record SkinRegionStats(double skinRatio, double largestComponentRatio, int componentCount) {
    }
}

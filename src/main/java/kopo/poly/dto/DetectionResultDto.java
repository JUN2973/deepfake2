package kopo.poly.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 화면과 API 사이에서 데이터를 전달하기 위한 DTO 클래스다.
 */
public class DetectionResultDto {

    private Long id;
    private Long userId;
    private String status;
    private Integer finalScore;
    private String explanationText;
    private String detailedReason;
    private String originalFilename;
    private String imageUrl;
    private String savedPath;
    private String notApplicableReason;
    private String overlaySource;
    private String createdAt;
    private Integer imageWidth;
    private Integer imageHeight;
    private boolean mockMode;
    private List<SuspiciousRegionDto> suspiciousRegions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Integer finalScore) {
        this.finalScore = finalScore;
    }

    public String getExplanationText() {
        return explanationText;
    }

    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
    }

    public String getDetailedReason() {
        return detailedReason;
    }

    public void setDetailedReason(String detailedReason) {
        this.detailedReason = detailedReason;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSavedPath() {
        return savedPath;
    }

    public void setSavedPath(String savedPath) {
        this.savedPath = savedPath;
    }

    public String getNotApplicableReason() {
        return notApplicableReason;
    }

    public void setNotApplicableReason(String notApplicableReason) {
        this.notApplicableReason = notApplicableReason;
    }

    public String getOverlaySource() {
        return overlaySource;
    }

    public void setOverlaySource(String overlaySource) {
        this.overlaySource = overlaySource;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }

    public List<SuspiciousRegionDto> getSuspiciousRegions() {
        return suspiciousRegions;
    }

    public void setSuspiciousRegions(List<SuspiciousRegionDto> suspiciousRegions) {
        this.suspiciousRegions = suspiciousRegions;
    }
}

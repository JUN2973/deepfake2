package kopo.poly.service;

import kopo.poly.dto.ReviewRequestCreateDTO;
import kopo.poly.dto.ReviewRequestResponseDTO;

import java.util.List;

/**
 * 오탐 신고 및 재검토 요청 등록과 조회를 위한 서비스 계약이다.
 */
public interface IReviewRequestService {

    ReviewRequestResponseDTO createRequest(Long verificationId,
                                           Long userId,
                                           ReviewRequestCreateDTO request);

    ReviewRequestResponseDTO getRequest(Long id, Long userId);

    List<ReviewRequestResponseDTO> getRequests(Long userId);
}

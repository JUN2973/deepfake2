package kopo.poly.mapper;

import kopo.poly.dto.ReviewRequestResponseDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 오탐 신고 및 재검토 요청을 저장하고 조회하는 MyBatis 매퍼다.
 */
public interface IReviewRequestMapper {

    int insertReviewRequest(ReviewRequestResponseDTO request);

    ReviewRequestResponseDTO selectActiveByVerificationIdAndUserId(
            @Param("verificationId") Long verificationId,
            @Param("userId") Long userId
    );

    ReviewRequestResponseDTO selectByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    List<ReviewRequestResponseDTO> selectListByUserId(@Param("userId") Long userId);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("reviewerNote") String reviewerNote
    );
}

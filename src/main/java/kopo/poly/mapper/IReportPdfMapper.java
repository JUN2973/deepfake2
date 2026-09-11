package kopo.poly.mapper;

import kopo.poly.dto.ReportPdfResponseDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 신고 제출용 PDF 생성 이력과 생성 당시 분석 스냅샷을 관리하는 MyBatis 매퍼다.
 */
public interface IReportPdfMapper {

    int insertReport(ReportPdfResponseDTO report);

    ReportPdfResponseDTO selectByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    ReportPdfResponseDTO selectLatestByVerificationIdAndUserId(
            @Param("verificationId") Long verificationId,
            @Param("userId") Long userId
    );

    List<ReportPdfResponseDTO> selectListByUserId(@Param("userId") Long userId);

    int deleteByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    int deleteByVerificationIdAndUserId(
            @Param("verificationId") Long verificationId,
            @Param("userId") Long userId
    );
}

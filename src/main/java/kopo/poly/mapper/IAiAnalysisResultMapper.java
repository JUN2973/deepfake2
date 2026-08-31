package kopo.poly.mapper;

import kopo.poly.dto.AiAnalysisResultDTO;
import org.apache.ibatis.annotations.Param;

public interface IAiAnalysisResultMapper {
    AiAnalysisResultDTO selectByRequestHash(@Param("verificationId") Long verificationId,
                                            @Param("requestHash") String requestHash);
    AiAnalysisResultDTO selectLatest(@Param("verificationId") Long verificationId);
    int upsert(AiAnalysisResultDTO result);
}

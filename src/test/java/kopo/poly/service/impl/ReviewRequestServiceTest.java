package kopo.poly.service.impl;

import kopo.poly.dto.ReviewRequestCreateDTO;
import kopo.poly.dto.ReviewRequestResponseDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IReviewRequestMapper;
import kopo.poly.mapper.IVerifyMapper;
import kopo.poly.service.ReviewRequestServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewRequestServiceTest {

    private IReviewRequestMapper reviewRequestMapper;
    private IVerifyMapper verifyMapper;
    private ReviewRequestService service;

    @BeforeEach
    void setUp() {
        reviewRequestMapper = mock(IReviewRequestMapper.class);
        verifyMapper = mock(IVerifyMapper.class);
        service = new ReviewRequestService(reviewRequestMapper, verifyMapper);
    }

    @Test
    void createRequestValidatesOwnerAndStoresPendingRequest() {
        VerifyDTO verification = verification();
        when(verifyMapper.selectVerificationByIdAndUserId(7L, 3L)).thenReturn(verification);
        when(reviewRequestMapper.insertReviewRequest(any())).thenAnswer(setGeneratedId(11L));
        when(reviewRequestMapper.selectByIdAndUserId(11L, 3L)).thenAnswer(invocation -> {
            ReviewRequestResponseDTO saved = new ReviewRequestResponseDTO();
            saved.setId(11L);
            saved.setVerificationId(7L);
            saved.setUserId(3L);
            saved.setRequestType(ReviewRequestCreateDTO.TYPE_FALSE_POSITIVE);
            saved.setReason("실제 촬영한 원본 이미지입니다.");
            saved.setStatus(ReviewRequestResponseDTO.STATUS_PENDING);
            saved.setOriginalName("sample.png");
            return saved;
        });

        ReviewRequestCreateDTO request = new ReviewRequestCreateDTO();
        request.setRequestType(" false_positive ");
        request.setReason("  실제 촬영한 원본 이미지입니다.  ");

        ReviewRequestResponseDTO result = service.createRequest(7L, 3L, request);

        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getRequestType()).isEqualTo(ReviewRequestCreateDTO.TYPE_FALSE_POSITIVE);
        assertThat(result.getStatus()).isEqualTo(ReviewRequestResponseDTO.STATUS_PENDING);
        verify(reviewRequestMapper).insertReviewRequest(any(ReviewRequestResponseDTO.class));
    }

    @Test
    void createRequestReturnsExistingActiveRequestWithoutInsert() {
        when(verifyMapper.selectVerificationByIdAndUserId(7L, 3L)).thenReturn(verification());
        ReviewRequestResponseDTO active = new ReviewRequestResponseDTO();
        active.setId(9L);
        active.setStatus(ReviewRequestResponseDTO.STATUS_REVIEWING);
        when(reviewRequestMapper.selectActiveByVerificationIdAndUserId(7L, 3L)).thenReturn(active);

        ReviewRequestResponseDTO result = service.createRequest(7L, 3L, validRequest());

        assertThat(result.getId()).isEqualTo(9L);
        verify(reviewRequestMapper, never()).insertReviewRequest(any());
    }

    @Test
    void createRequestRejectsVerificationOwnedByAnotherUser() {
        when(verifyMapper.selectVerificationByIdAndUserId(7L, 3L)).thenReturn(null);

        assertThatThrownBy(() -> service.createRequest(7L, 3L, validRequest()))
                .isInstanceOf(ReviewRequestServiceException.class)
                .extracting(error -> ((ReviewRequestServiceException) error).getCode())
                .isEqualTo("REVIEW-4040");
        verify(reviewRequestMapper, never()).insertReviewRequest(any());
    }

    @Test
    void createRequestRejectsUnsupportedType() {
        ReviewRequestCreateDTO request = validRequest();
        request.setRequestType("UNKNOWN");

        assertThatThrownBy(() -> service.createRequest(7L, 3L, request))
                .isInstanceOf(ReviewRequestServiceException.class)
                .extracting(error -> ((ReviewRequestServiceException) error).getCode())
                .isEqualTo("REVIEW-TYPE");
        verify(verifyMapper, never()).selectVerificationByIdAndUserId(any(), any());
    }

    @Test
    void getRequestsReturnsEmptyListWhenMapperReturnsNull() {
        when(reviewRequestMapper.selectListByUserId(3L)).thenReturn(null);

        assertThat(service.getRequests(3L)).isEmpty();
    }

    private Answer<Integer> setGeneratedId(Long id) {
        return invocation -> {
            ReviewRequestResponseDTO saved = invocation.getArgument(0);
            saved.setId(id);
            return 1;
        };
    }

    private ReviewRequestCreateDTO validRequest() {
        ReviewRequestCreateDTO request = new ReviewRequestCreateDTO();
        request.setRequestType(ReviewRequestCreateDTO.TYPE_RECHECK);
        request.setReason("판정 결과를 다시 확인해 주세요.");
        return request;
    }

    private VerifyDTO verification() {
        VerifyDTO verification = new VerifyDTO();
        verification.setId(7L);
        verification.setUserId(3L);
        verification.setOriginalName("sample.png");
        verification.setVerdict("SUSPICIOUS");
        verification.setScore(0.82);
        return verification;
    }
}

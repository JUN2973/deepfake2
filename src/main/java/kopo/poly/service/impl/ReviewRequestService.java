package kopo.poly.service.impl;

import kopo.poly.dto.ReviewRequestCreateDTO;
import kopo.poly.dto.ReviewRequestResponseDTO;
import kopo.poly.dto.VerifyDTO;
import kopo.poly.mapper.IReviewRequestMapper;
import kopo.poly.mapper.IVerifyMapper;
import kopo.poly.service.IReviewRequestService;
import kopo.poly.service.ReviewRequestServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 검증 기록 소유권과 입력값을 확인한 뒤 재검토 요청을 DB에 저장한다.
 */
@Service
public class ReviewRequestService implements IReviewRequestService {

    private static final int MAX_REASON_LENGTH = 1_000;
    private static final Set<String> REQUEST_TYPES = Set.of(
            ReviewRequestCreateDTO.TYPE_FALSE_POSITIVE,
            ReviewRequestCreateDTO.TYPE_FALSE_NEGATIVE,
            ReviewRequestCreateDTO.TYPE_RECHECK
    );

    private final IReviewRequestMapper reviewRequestMapper;
    private final IVerifyMapper verifyMapper;
    private final ConcurrentMap<String, Object> requestLocks = new ConcurrentHashMap<>();

    public ReviewRequestService(IReviewRequestMapper reviewRequestMapper,
                                IVerifyMapper verifyMapper) {
        this.reviewRequestMapper = reviewRequestMapper;
        this.verifyMapper = verifyMapper;
    }

    @Override
    @Transactional
    public ReviewRequestResponseDTO createRequest(Long verificationId,
                                                  Long userId,
                                                  ReviewRequestCreateDTO request) {
        requireUserId(userId);
        if (verificationId == null) {
            throw new ReviewRequestServiceException("REVIEW-4000", "검증 기록 번호가 필요합니다.");
        }

        String requestType = normalizeRequestType(request);
        String reason = normalizeReason(request);
        VerifyDTO verification = verifyMapper.selectVerificationByIdAndUserId(verificationId, userId);
        if (verification == null) {
            throw new ReviewRequestServiceException("REVIEW-4040", "재검토할 검증 기록을 찾을 수 없습니다.");
        }

        String lockKey = userId + ":" + verificationId;
        Object lock = requestLocks.computeIfAbsent(lockKey, key -> new Object());
        try {
            synchronized (lock) {
                ReviewRequestResponseDTO active = reviewRequestMapper
                        .selectActiveByVerificationIdAndUserId(verificationId, userId);
                if (active != null) {
                    return active;
                }

                ReviewRequestResponseDTO saved = new ReviewRequestResponseDTO();
                saved.setVerificationId(verificationId);
                saved.setUserId(userId);
                saved.setRequestType(requestType);
                saved.setReason(reason);
                saved.setStatus(ReviewRequestResponseDTO.STATUS_PENDING);

                int inserted = reviewRequestMapper.insertReviewRequest(saved);
                if (inserted != 1 || saved.getId() == null) {
                    throw new ReviewRequestServiceException("REVIEW-SAVE", "재검토 요청을 저장하지 못했습니다.");
                }

                ReviewRequestResponseDTO result = reviewRequestMapper.selectByIdAndUserId(saved.getId(), userId);
                if (result != null) {
                    return result;
                }

                saved.setOriginalName(verification.getOriginalName());
                saved.setVerdict(verification.getVerdict());
                saved.setScore(verification.getScore());
                return saved;
            }
        } finally {
            requestLocks.remove(lockKey, lock);
        }
    }

    @Override
    public ReviewRequestResponseDTO getRequest(Long id, Long userId) {
        requireUserId(userId);
        if (id == null) {
            throw new ReviewRequestServiceException("REVIEW-4000", "재검토 요청 번호가 필요합니다.");
        }

        ReviewRequestResponseDTO result = reviewRequestMapper.selectByIdAndUserId(id, userId);
        if (result == null) {
            throw new ReviewRequestServiceException("REVIEW-4041", "재검토 요청을 찾을 수 없습니다.");
        }
        return result;
    }

    @Override
    public List<ReviewRequestResponseDTO> getRequests(Long userId) {
        requireUserId(userId);
        List<ReviewRequestResponseDTO> requests = reviewRequestMapper.selectListByUserId(userId);
        return requests == null ? List.of() : requests;
    }

    private String normalizeRequestType(ReviewRequestCreateDTO request) {
        String value = request == null ? null : request.getRequestType();
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!REQUEST_TYPES.contains(normalized)) {
            throw new ReviewRequestServiceException(
                    "REVIEW-TYPE",
                    "요청 유형은 FALSE_POSITIVE, FALSE_NEGATIVE, RECHECK 중 하나여야 합니다."
            );
        }
        return normalized;
    }

    private String normalizeReason(ReviewRequestCreateDTO request) {
        String value = request == null ? null : request.getReason();
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ReviewRequestServiceException("REVIEW-REASON", "재검토 요청 사유를 입력해 주세요.");
        }
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new ReviewRequestServiceException(
                    "REVIEW-REASON",
                    "재검토 요청 사유는 1,000자 이내로 입력해 주세요."
            );
        }
        return normalized;
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new ReviewRequestServiceException("REVIEW-AUTH", "로그인이 필요합니다.");
        }
    }
}

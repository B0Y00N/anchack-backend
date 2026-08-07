package com.kbait.anchack.review.service.impl;

import com.kbait.anchack.review.domain.Review;
import com.kbait.anchack.review.domain.ReviewScore;
import com.kbait.anchack.review.dto.request.AdminReviewStatusRequest;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
import com.kbait.anchack.review.dto.response.ReviewResponse;
import com.kbait.anchack.review.mapper.ReviewMapper;
import com.kbait.anchack.review.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl
    implements ReviewService {

    private static final Set<String> CATEGORY_CODES =
        Set.of(
            "NOISE",
            "CLEANLINESS",
            "SAFETY",
            "ATMOSPHERE",
            "TRANSIT"
        );

    private static final Set<String> REVIEW_STATUSES =
        Set.of(
            "ACTIVE",
            "HIDDEN",
            "DELETED"
        );

    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(
        ReviewMapper reviewMapper
    ) {
        this.reviewMapper = reviewMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByAdminDong(
        Long adminDongId
    ) {
        if (adminDongId == null) {
            throw new IllegalArgumentException(
                "행정동 ID는 필수입니다."
            );
        }

        return reviewMapper
            .findActiveByAdminDongId(adminDongId)
            .stream()
            .map(review -> {
                loadCategoryScores(review);
                return ReviewResponse.from(review);
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(
        Long reviewId
    ) {
        Review review =
            findReviewOrThrow(reviewId);

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new IllegalStateException(
                "조회할 수 없는 리뷰입니다."
            );
        }

        loadCategoryScores(review);

        return ReviewResponse.from(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(
        Long userId
    ) {
        validateUserId(userId);

        return reviewMapper
            .findByUserId(userId)
            .stream()
            .map(review -> {
                loadCategoryScores(review);
                return ReviewResponse.fromForOwner(review);
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewResponse createReview(
        Long userId,
        ReviewCreateRequest request
    ) {
        validateUserId(userId);
        validateCreateRequest(request);

        if (reviewMapper.existsAdminDong(
            request.getAdminDongId()
        ) == 0) {
            throw new IllegalArgumentException(
                "존재하지 않는 행정동입니다."
            );
        }

        Review review =
            new Review();

        review.setAdminDongId(
            request.getAdminDongId()
        );
        review.setUserId(userId);
        review.setOverallRating(
            request.getOverallRating()
        );
        review.setContent(
            request.getContent().trim()
        );
        review.setAnonymous(
            Boolean.TRUE.equals(
                request.getAnonymous()
            )
        );
        review.setStatus("ACTIVE");

        int inserted =
            reviewMapper.insertReview(review);

        if (inserted != 1 ||
            review.getReviewId() == null) {
            throw new IllegalStateException(
                "리뷰 등록에 실패했습니다."
            );
        }

        insertCategoryScores(
            review.getReviewId(),
            request.getCategoryScores()
        );

        Review savedReview =
            findReviewOrThrow(
                review.getReviewId()
            );

        loadCategoryScores(savedReview);

        return ReviewResponse.fromForOwner(
            savedReview
        );
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(
        Long userId,
        Long reviewId,
        ReviewUpdateRequest request
    ) {
        validateUserId(userId);
        validateUpdateRequest(request);

        Review review =
            findReviewOrThrow(reviewId);

        validateOwner(userId, review);

        if ("DELETED".equals(review.getStatus())) {
            throw new IllegalStateException(
                "삭제된 리뷰는 수정할 수 없습니다."
            );
        }

        review.setOverallRating(
            request.getOverallRating()
        );
        review.setContent(
            request.getContent().trim()
        );
        review.setAnonymous(
            Boolean.TRUE.equals(
                request.getAnonymous()
            )
        );

        int updated =
            reviewMapper.updateReview(review);

        if (updated != 1) {
            throw new IllegalStateException(
                "리뷰 수정에 실패했습니다."
            );
        }

        reviewMapper.deleteReviewScores(
            reviewId
        );

        insertCategoryScores(
            reviewId,
            request.getCategoryScores()
        );

        Review updatedReview =
            findReviewOrThrow(reviewId);

        loadCategoryScores(updatedReview);

        return ReviewResponse.fromForOwner(
            updatedReview
        );
    }

    @Override
    @Transactional
    public void deleteReview(
        Long userId,
        Long reviewId
    ) {
        validateUserId(userId);

        Review review =
            findReviewOrThrow(reviewId);

        validateOwner(userId, review);

        if ("DELETED".equals(review.getStatus())) {
            return;
        }

        int updated =
            reviewMapper.updateReviewStatus(
                reviewId,
                "DELETED"
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "리뷰 삭제에 실패했습니다."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForAdmin(
        Long adminId,
        String status
    ) {
        validateAdmin(adminId);

        String normalizedStatus = null;

        if (status != null &&
            !status.isBlank()) {
            normalizedStatus =
                status.trim().toUpperCase();

            validateStatus(normalizedStatus);
        }

        return reviewMapper
            .findAllForAdmin(normalizedStatus)
            .stream()
            .map(review -> {
                loadCategoryScores(review);
                return ReviewResponse.fromForOwner(review);
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewResponse updateReviewStatusByAdmin(
        Long adminId,
        Long reviewId,
        AdminReviewStatusRequest request
    ) {
        validateAdmin(adminId);

        if (request == null ||
            request.getStatus() == null) {
            throw new IllegalArgumentException(
                "변경할 리뷰 상태는 필수입니다."
            );
        }

        String status =
            request.getStatus()
                .trim()
                .toUpperCase();

        validateStatus(status);

        String reason =
            request.getReason() == null
                ? null
                : request.getReason().trim();

        if (reason == null ||
            reason.isBlank()) {
            throw new IllegalArgumentException(
                "관리자 처리 사유는 필수입니다."
            );
        }

        if (reason.length() > 255) {
            throw new IllegalArgumentException(
                "관리자 처리 사유는 255자 이하로 입력해주세요."
            );
        }

        findReviewOrThrow(reviewId);

        int updated =
            reviewMapper.updateReviewStatus(
                reviewId,
                status
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "리뷰 상태 변경에 실패했습니다."
            );
        }

        reviewMapper.insertAdminAction(
            reviewId,
            convertActionType(status),
            reason
        );

        Review updatedReview =
            findReviewOrThrow(reviewId);

        loadCategoryScores(updatedReview);

        return ReviewResponse.fromForOwner(
            updatedReview
        );
    }

    private Review findReviewOrThrow(
        Long reviewId
    ) {
        if (reviewId == null) {
            throw new IllegalArgumentException(
                "리뷰 ID는 필수입니다."
            );
        }

        Review review =
            reviewMapper.findById(reviewId);

        if (review == null) {
            throw new IllegalArgumentException(
                "리뷰를 찾을 수 없습니다."
            );
        }

        return review;
    }

    private void loadCategoryScores(
        Review review
    ) {
        List<ReviewScore> scores =
            reviewMapper.findScoresByReviewId(
                review.getReviewId()
            );

        Map<String, Integer> categoryScores =
            new LinkedHashMap<>();

        for (ReviewScore score : scores) {
            categoryScores.put(
                score.getCategoryCode(),
                score.getScore()
            );
        }

        review.setCategoryScores(categoryScores);
    }

    private void insertCategoryScores(
        Long reviewId,
        Map<String, Integer> categoryScores
    ) {
        if (categoryScores == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry
            : categoryScores.entrySet()) {

            String categoryCode =
                entry.getKey() == null
                    ? null
                    : entry.getKey()
                    .trim()
                    .toUpperCase();

            Integer score =
                entry.getValue();

            validateCategoryScore(
                categoryCode,
                score
            );

            int inserted =
                reviewMapper.insertReviewScore(
                    reviewId,
                    categoryCode,
                    score
                );

            if (inserted != 1) {
                throw new IllegalArgumentException(
                    "존재하지 않는 리뷰 카테고리입니다: "
                        + categoryCode
                );
            }
        }
    }

    private void validateCreateRequest(
        ReviewCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                "리뷰 정보가 없습니다."
            );
        }

        if (request.getAdminDongId() == null) {
            throw new IllegalArgumentException(
                "행정동 ID는 필수입니다."
            );
        }

        validateReviewValues(
            request.getOverallRating(),
            request.getContent(),
            request.getCategoryScores()
        );
    }

    private void validateUpdateRequest(
        ReviewUpdateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                "수정할 리뷰 정보가 없습니다."
            );
        }

        validateReviewValues(
            request.getOverallRating(),
            request.getContent(),
            request.getCategoryScores()
        );
    }

    private void validateReviewValues(
        Integer overallRating,
        String content,
        Map<String, Integer> categoryScores
    ) {
        if (overallRating == null ||
            overallRating < 1 ||
            overallRating > 5) {
            throw new IllegalArgumentException(
                "전체 평점은 1점부터 5점까지 입력할 수 있습니다."
            );
        }

        if (content == null ||
            content.isBlank()) {
            throw new IllegalArgumentException(
                "리뷰 내용은 필수입니다."
            );
        }

        if (content.trim().length() > 500) {
            throw new IllegalArgumentException(
                "리뷰 내용은 500자 이하로 입력해주세요."
            );
        }

        if (categoryScores != null) {
            categoryScores.forEach(
                this::validateCategoryScore
            );
        }
    }

    private void validateCategoryScore(
        String categoryCode,
        Integer score
    ) {
        if (categoryCode == null ||
            !CATEGORY_CODES.contains(
                categoryCode.toUpperCase()
            )) {
            throw new IllegalArgumentException(
                "지원하지 않는 리뷰 카테고리입니다: "
                    + categoryCode
            );
        }

        if (score == null ||
            score < 1 ||
            score > 5) {
            throw new IllegalArgumentException(
                "카테고리 평점은 1점부터 5점까지 입력할 수 있습니다."
            );
        }
    }

    private void validateOwner(
        Long userId,
        Review review
    ) {
        if (!userId.equals(
            review.getUserId()
        )) {
            throw new SecurityException(
                "본인이 작성한 리뷰만 변경할 수 있습니다."
            );
        }
    }

    private void validateUserId(
        Long userId
    ) {
        if (userId == null) {
            throw new SecurityException(
                "인증 정보가 없습니다."
            );
        }
    }

    private void validateAdmin(
        Long userId
    ) {
        validateUserId(userId);

        String role =
            reviewMapper.findUserRole(userId);

        if (!"ADMIN".equals(role)) {
            throw new SecurityException(
                "관리자 권한이 필요합니다."
            );
        }
    }

    private void validateStatus(
        String status
    ) {
        if (!REVIEW_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                "리뷰 상태는 ACTIVE, HIDDEN, DELETED 중 하나여야 합니다."
            );
        }
    }

    private String convertActionType(
        String status
    ) {
        switch (status) {
            case "HIDDEN":
                return "HIDE";
            case "DELETED":
                return "DELETE";
            case "ACTIVE":
                return "RESTORE";
            default:
                throw new IllegalArgumentException(
                    "지원하지 않는 리뷰 상태입니다."
                );
        }
    }
}

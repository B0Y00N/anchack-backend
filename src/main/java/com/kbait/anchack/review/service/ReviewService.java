package com.kbait.anchack.review.service;

import com.kbait.anchack.review.domain.Review;
import com.kbait.anchack.review.domain.ReviewScore;
import com.kbait.anchack.review.dto.request.AdminReviewStatusRequest;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
import com.kbait.anchack.review.dto.response.ReviewResponse;
import com.kbait.anchack.review.mapper.ReviewMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;

    public ReviewService(
        ReviewMapper reviewMapper
    ) {
        this.reviewMapper = reviewMapper;
    }

    public List<ReviewResponse> getReviewsByAdminDong(
        Long adminDongId
    ) {
        if (adminDongId == null) {
            throw new IllegalArgumentException(
                "행정동 ID가 필요합니다."
            );
        }

        if (
            reviewMapper.existsAdminDong(
                adminDongId
            ) == 0
        ) {
            throw new IllegalArgumentException(
                "존재하지 않는 행정동입니다."
            );
        }

        return reviewMapper
            .findActiveByAdminDongId(adminDongId)
            .stream()
            .map(review -> {
                review.setCategoryScores(
                    getCategoryScores(
                        review.getReviewId()
                    )
                );

                return ReviewResponse.from(review);
            })
            .collect(Collectors.toList());
    }

    public ReviewResponse getReview(
        Long reviewId
    ) {
        Review review = findReview(reviewId);

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new IllegalStateException(
                "현재 조회할 수 없는 리뷰입니다."
            );
        }

        review.setCategoryScores(
            getCategoryScores(reviewId)
        );

        return ReviewResponse.from(review);
    }

    public List<ReviewResponse> getMyReviews(
        Long userId
    ) {
        if (userId == null) {
            throw new IllegalArgumentException(
                "사용자 ID가 필요합니다."
            );
        }

        return reviewMapper
            .findByUserId(userId)
            .stream()
            .map(review -> {
                review.setCategoryScores(
                    getCategoryScores(
                        review.getReviewId()
                    )
                );

                return ReviewResponse
                    .fromForOwner(review);
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse createReview(
        Long userId,
        ReviewCreateRequest request
    ) {
        if (userId == null) {
            throw new IllegalArgumentException(
                "사용자 ID가 필요합니다."
            );
        }

        validateCreateRequest(request);

        if (
            reviewMapper.existsAdminDong(
                request.getAdminDongId()
            ) == 0
        ) {
            throw new IllegalArgumentException(
                "존재하지 않는 행정동입니다."
            );
        }

        Review review = new Review();

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

        int insertedCount =
            reviewMapper.insertReview(review);

        if (
            insertedCount != 1 ||
                review.getReviewId() == null
        ) {
            throw new IllegalStateException(
                "리뷰 저장에 실패했습니다."
            );
        }

        insertCategoryScores(
            review.getReviewId(),
            request.getCategoryScores()
        );

        Review savedReview =
            reviewMapper.findById(
                review.getReviewId()
            );

        if (savedReview == null) {
            throw new IllegalStateException(
                "저장된 리뷰를 조회할 수 없습니다."
            );
        }

        savedReview.setCategoryScores(
            getCategoryScores(
                savedReview.getReviewId()
            )
        );

        return ReviewResponse.fromForOwner(
            savedReview
        );
    }

    @Transactional
    public ReviewResponse updateReview(
        Long userId,
        Long reviewId,
        ReviewUpdateRequest request
    ) {
        Review review = findReview(reviewId);

        validateOwner(userId, review);
        validateUpdateRequest(request);

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new IllegalStateException(
                "활성 상태의 리뷰만 수정할 수 있습니다."
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

        int updatedCount =
            reviewMapper.updateReview(review);

        if (updatedCount != 1) {
            throw new IllegalStateException(
                "리뷰 수정에 실패했습니다."
            );
        }

        reviewMapper.deleteReviewScores(reviewId);

        insertCategoryScores(
            reviewId,
            request.getCategoryScores()
        );

        Review updatedReview =
            reviewMapper.findById(reviewId);

        if (updatedReview == null) {
            throw new IllegalStateException(
                "수정된 리뷰를 조회할 수 없습니다."
            );
        }

        updatedReview.setCategoryScores(
            getCategoryScores(reviewId)
        );

        return ReviewResponse.fromForOwner(
            updatedReview
        );
    }

    @Transactional
    public void deleteReview(
        Long userId,
        Long reviewId
    ) {
        Review review = findReview(reviewId);

        validateOwner(userId, review);

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new IllegalStateException(
                "이미 삭제되었거나 숨김 처리된 리뷰입니다."
            );
        }

        int updatedCount =
            reviewMapper.updateReviewStatus(
                reviewId,
                "DELETED"
            );

        if (updatedCount != 1) {
            throw new IllegalStateException(
                "리뷰 삭제에 실패했습니다."
            );
        }
    }

    public List<ReviewResponse> getReviewsForAdmin(
        Long adminId,
        String status
    ) {
        validateAdmin(adminId);

        String normalizedStatus =
            normalizeStatus(status);

        return reviewMapper
            .findAllForAdmin(normalizedStatus)
            .stream()
            .map(review -> {
                review.setCategoryScores(
                    getCategoryScores(
                        review.getReviewId()
                    )
                );

                return ReviewResponse
                    .fromForOwner(review);
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse updateReviewStatusByAdmin(
        Long adminId,
        Long reviewId,
        AdminReviewStatusRequest request
    ) {
        validateAdmin(adminId);

        if (request == null) {
            throw new IllegalArgumentException(
                "변경할 상태 정보가 필요합니다."
            );
        }

        findReview(reviewId);

        String status =
            normalizeRequiredStatus(
                request.getStatus()
            );

        int updatedCount =
            reviewMapper.updateReviewStatus(
                reviewId,
                status
            );

        if (updatedCount != 1) {
            throw new IllegalStateException(
                "리뷰 상태 변경에 실패했습니다."
            );
        }

        reviewMapper.insertAdminAction(
            reviewId,
            status,
            request.getReason()
        );

        Review updatedReview =
            reviewMapper.findById(reviewId);

        if (updatedReview == null) {
            throw new IllegalStateException(
                "변경된 리뷰를 조회할 수 없습니다."
            );
        }

        updatedReview.setCategoryScores(
            getCategoryScores(reviewId)
        );

        return ReviewResponse.fromForOwner(
            updatedReview
        );
    }

    private Review findReview(
        Long reviewId
    ) {
        if (reviewId == null) {
            throw new IllegalArgumentException(
                "리뷰 ID가 필요합니다."
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

    private void insertCategoryScores(
        Long reviewId,
        Map<String, Integer> categoryScores
    ) {
        for (
            Map.Entry<String, Integer> entry
            : categoryScores.entrySet()
        ) {
            int insertedCount =
                reviewMapper.insertReviewScore(
                    reviewId,
                    entry.getKey(),
                    entry.getValue()
                );

            if (insertedCount != 1) {
                throw new IllegalArgumentException(
                    "존재하지 않는 리뷰 평가 항목입니다: "
                        + entry.getKey()
                );
            }
        }
    }

    private Map<String, Integer> getCategoryScores(
        Long reviewId
    ) {
        Map<String, Integer> categoryScores =
            new LinkedHashMap<>();

        List<ReviewScore> scores =
            reviewMapper.findScoresByReviewId(
                reviewId
            );

        for (ReviewScore score : scores) {
            categoryScores.put(
                score.getCategoryCode(),
                score.getScore()
            );
        }

        return categoryScores;
    }

    private void validateCreateRequest(
        ReviewCreateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                "리뷰 정보가 필요합니다."
            );
        }

        if (request.getAdminDongId() == null) {
            throw new IllegalArgumentException(
                "행정동 정보가 필요합니다."
            );
        }

        validateReviewFields(
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
                "수정할 리뷰 정보가 필요합니다."
            );
        }

        validateReviewFields(
            request.getOverallRating(),
            request.getContent(),
            request.getCategoryScores()
        );
    }

    private void validateReviewFields(
        Integer overallRating,
        String content,
        Map<String, Integer> categoryScores
    ) {
        if (
            overallRating == null ||
                overallRating < 1 ||
                overallRating > 5
        ) {
            throw new IllegalArgumentException(
                "종합 별점은 1점부터 5점까지 입력해야 합니다."
            );
        }

        if (
            content == null ||
                content.trim().length() < 20
        ) {
            throw new IllegalArgumentException(
                "리뷰 내용은 최소 20자 이상 입력해야 합니다."
            );
        }

        if (
            categoryScores == null ||
                categoryScores.isEmpty()
        ) {
            throw new IllegalArgumentException(
                "항목별 별점을 입력해주세요."
            );
        }

        for (
            Map.Entry<String, Integer> entry
            : categoryScores.entrySet()
        ) {
            String categoryCode = entry.getKey();
            Integer score = entry.getValue();

            if (
                categoryCode == null ||
                    categoryCode.trim().isEmpty()
            ) {
                throw new IllegalArgumentException(
                    "평가 항목 코드가 필요합니다."
                );
            }

            if (
                score == null ||
                    score < 1 ||
                    score > 5
            ) {
                throw new IllegalArgumentException(
                    "항목별 별점은 1점부터 5점까지 입력해야 합니다."
                );
            }
        }
    }

    private void validateOwner(
        Long userId,
        Review review
    ) {
        if (
            userId == null ||
                !userId.equals(review.getUserId())
        ) {
            throw new SecurityException(
                "본인이 작성한 리뷰만 변경할 수 있습니다."
            );
        }
    }

    private void validateAdmin(
        Long adminId
    ) {
        if (adminId == null) {
            throw new SecurityException(
                "로그인이 필요합니다."
            );
        }

        String role =
            reviewMapper.findUserRole(adminId);

        if (!"ADMIN".equals(role)) {
            throw new SecurityException(
                "관리자 권한이 필요합니다."
            );
        }
    }

    private String normalizeStatus(
        String status
    ) {
        if (
            status == null ||
                status.trim().isEmpty()
        ) {
            return null;
        }

        return normalizeRequiredStatus(status);
    }

    private String normalizeRequiredStatus(
        String status
    ) {
        if (status == null) {
            throw new IllegalArgumentException(
                "리뷰 상태가 필요합니다."
            );
        }

        String normalized =
            status.trim().toUpperCase();

        if (
            !"ACTIVE".equals(normalized) &&
                !"HIDDEN".equals(normalized) &&
                !"DELETED".equals(normalized)
        ) {
            throw new IllegalArgumentException(
                "리뷰 상태는 ACTIVE, HIDDEN, DELETED 중 하나여야 합니다."
            );
        }

        return normalized;
    }
}

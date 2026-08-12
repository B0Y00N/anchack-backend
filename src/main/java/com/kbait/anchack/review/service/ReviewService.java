package com.kbait.anchack.review.service;

import com.kbait.anchack.review.dto.request.AdminReviewStatusRequest;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
import com.kbait.anchack.review.dto.response.ReviewResponse;

import java.util.List;

/**
 * 리뷰 조회, 작성, 수정, 삭제 및 관리자 상태 변경을 담당한다.
 */
public interface ReviewService {

    /**
     * 특정 행정동의 공개 리뷰 목록을 조회한다.
     */
    List<ReviewResponse> getReviewsByAdminDong(
            Long adminDongId,
            Long viewerId
    );

    /**
     * 리뷰 상세 정보를 조회한다.
     */
    ReviewResponse getReview(
            Long reviewId,
            Long viewerId
    );

    /**
     * 로그인한 사용자가 작성한 리뷰 목록을 조회한다.
     */
    List<ReviewResponse> getMyReviews(Long userId);

    /**
     * 리뷰를 등록한다.
     */
    ReviewResponse createReview(
            Long userId,
            ReviewCreateRequest request
    );

    /**
     * 리뷰를 수정한다.
     */
    ReviewResponse updateReview(
            Long userId,
            Long reviewId,
            ReviewUpdateRequest request
    );

    /**
     * 리뷰 상태를 DELETED로 변경한다.
     */
    void deleteReview(
            Long userId,
            Long reviewId
    );

    /*
     * 좋아요/싫어요 기능 임시 비활성화
     *
     * 다시 활성화할 때 다음 import가 필요하다.
     * import com.kbait.anchack.review.dto.response.ReviewReactionResponse;
     *
     * ReviewReactionResponse reactToReview(
     *     Long userId,
     *     Long reviewId,
     *     String reactionType
     * );
     */

    /**
     * 관리자용 리뷰 목록을 조회한다.
     * status가 없으면 모든 상태를 조회한다.
     */
    List<ReviewResponse> getReviewsForAdmin(
            Long adminId,
            String status
    );

    /**
     * 관리자가 리뷰 상태를 변경한다.
     */
    ReviewResponse updateReviewStatusByAdmin(
            Long adminId,
            Long reviewId,
            AdminReviewStatusRequest request
    );
}

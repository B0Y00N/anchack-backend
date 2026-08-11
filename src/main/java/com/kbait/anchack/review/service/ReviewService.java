package com.kbait.anchack.review.service;

import com.kbait.anchack.review.dto.request.AdminReviewStatusRequest;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
import com.kbait.anchack.review.dto.response.ReviewReactionResponse;
import com.kbait.anchack.review.dto.response.ReviewResponse;

import java.util.List;

/**
 * 리뷰 조회/작성/수정/삭제, 좋아요·싫어요, 관리자 상태 변경을 담당한다.
 */
public interface ReviewService {

    /**
     * 특정 행정동의 공개(ACTIVE) 리뷰 목록을 조회한다.
     * viewerId가 있으면 그 사용자가 남긴 반응(myReaction)도 함께 채운다.
     */
    List<ReviewResponse> getReviewsByAdminDong(Long adminDongId, Long viewerId);

    /**
     * 리뷰 상세를 조회한다. ACTIVE 상태가 아니면 예외를 던진다.
     */
    ReviewResponse getReview(Long reviewId, Long viewerId);

    /**
     * 로그인한 사용자가 작성한 리뷰 목록을 조회한다(삭제된 리뷰 제외).
     */
    List<ReviewResponse> getMyReviews(Long userId);

    ReviewResponse createReview(Long userId, ReviewCreateRequest request);

    ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest request);

    /**
     * 리뷰를 삭제(상태를 DELETED로 변경)한다. 실제 행은 지우지 않는다.
     */
    void deleteReview(Long userId, Long reviewId);

    /**
     * 리뷰에 좋아요/싫어요를 남긴다.
     * 같은 반응을 다시 누르면 취소되고, 반대 반응을 누르면 바뀐다.
     */
    ReviewReactionResponse reactToReview(Long userId, Long reviewId, String reactionType);

    /**
     * 관리자용 전체 리뷰 조회. status가 없으면 모든 상태를 조회한다.
     */
    List<ReviewResponse> getReviewsForAdmin(Long adminId, String status);

    /**
     * 관리자가 리뷰 상태(ACTIVE/HIDDEN/DELETED)를 변경한다.
     */
    ReviewResponse updateReviewStatusByAdmin(Long adminId, Long reviewId, AdminReviewStatusRequest request);
}

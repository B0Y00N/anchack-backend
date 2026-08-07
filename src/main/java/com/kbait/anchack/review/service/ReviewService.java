package com.kbait.anchack.review.service;

import com.kbait.anchack.review.dto.request.AdminReviewStatusRequest;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
import com.kbait.anchack.review.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    List<ReviewResponse> getReviewsByAdminDong(
        Long adminDongId
    );

    ReviewResponse getReview(
        Long reviewId
    );

    List<ReviewResponse> getMyReviews(
        Long userId
    );

    ReviewResponse createReview(
        Long userId,
        ReviewCreateRequest request
    );

    ReviewResponse updateReview(
        Long userId,
        Long reviewId,
        ReviewUpdateRequest request
    );

    void deleteReview(
        Long userId,
        Long reviewId
    );

    List<ReviewResponse> getReviewsForAdmin(
        Long adminId,
        String status
    );

    ReviewResponse updateReviewStatusByAdmin(
        Long adminId,
        Long reviewId,
        AdminReviewStatusRequest request
    );
}

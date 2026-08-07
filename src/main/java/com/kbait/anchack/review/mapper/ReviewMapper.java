package com.kbait.anchack.review.mapper;

import com.kbait.anchack.review.domain.Review;
import com.kbait.anchack.review.domain.ReviewScore;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReviewMapper {

    List<Review> findActiveByAdminDongId(
        @Param("adminDongId") Long adminDongId
    );

    List<Review> findByUserId(
        @Param("userId") Long userId
    );

    List<Review> findAllForAdmin(
        @Param("status") String status
    );

    Review findById(
        @Param("reviewId") Long reviewId
    );

    List<ReviewScore> findScoresByReviewId(
        @Param("reviewId") Long reviewId
    );

    int existsAdminDong(
        @Param("adminDongId") Long adminDongId
    );

    String findUserRole(
        @Param("userId") Long userId
    );

    int insertReview(
        Review review
    );

    int insertReviewScore(
        @Param("reviewId") Long reviewId,
        @Param("categoryCode") String categoryCode,
        @Param("score") Integer score
    );

    int updateReview(
        Review review
    );

    int deleteReviewScores(
        @Param("reviewId") Long reviewId
    );

    int updateReviewStatus(
        @Param("reviewId") Long reviewId,
        @Param("status") String status
    );

    int insertAdminAction(
        @Param("reviewId") Long reviewId,
        @Param("actionType") String actionType,
        @Param("reason") String reason
    );
}

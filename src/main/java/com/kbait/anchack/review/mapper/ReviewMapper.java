package com.kbait.anchack.review.mapper;

import com.kbait.anchack.review.domain.Review;
import com.kbait.anchack.review.domain.ReviewScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
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

    /**
     * 여러 리뷰의 항목별 별점을 한 번에 조회한다.
     * 리뷰 목록 화면에서 리뷰마다 findScoresByReviewId를 반복 호출하지 않기 위해 사용한다.
     */
    List<ReviewScore> findScoresByReviewIds(
        @Param("reviewIds") List<Long> reviewIds
    );

    int existsAdminDong(
        @Param("adminDongId") Long adminDongId
    );

    String findUserRole(
        @Param("userId") Long userId
    );

    int insertReview(Review review);

    int insertReviewScore(
        @Param("reviewId") Long reviewId,
        @Param("categoryCode") String categoryCode,
        @Param("score") Integer score
    );

    int updateReview(Review review);

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

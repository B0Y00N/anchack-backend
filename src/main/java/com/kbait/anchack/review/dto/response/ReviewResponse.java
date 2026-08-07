package com.kbait.anchack.review.dto.response;

import com.kbait.anchack.review.domain.Review;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Getter
public class ReviewResponse {

    private Long reviewId;
    private Long adminDongId;
    private String adminDongName;

    private Integer overallRating;
    private String content;
    private Boolean anonymous;
    private String status;

    private Long writerId;
    private String writerNickname;
    private String writerProfileImageUrl;

    private Map<String, Integer> categoryScores;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse from(
        Review review
    ) {
        ReviewResponse response =
            new ReviewResponse();

        response.reviewId =
            review.getReviewId();
        response.adminDongId =
            review.getAdminDongId();
        response.adminDongName =
            review.getAdminDongName();
        response.overallRating =
            review.getOverallRating();
        response.content =
            review.getContent();
        response.anonymous =
            review.getAnonymous();
        response.status =
            review.getStatus();

        if (Boolean.TRUE.equals(review.getAnonymous())) {
            response.writerId = null;
            response.writerNickname = "익명";
            response.writerProfileImageUrl = null;
        } else {
            response.writerId =
                review.getUserId();
            response.writerNickname =
                review.getNickname();
            response.writerProfileImageUrl =
                review.getProfileImageUrl();
        }

        response.categoryScores =
            review.getCategoryScores() == null
                ? Collections.emptyMap()
                : review.getCategoryScores();

        response.createdAt =
            review.getCreatedAt();
        response.updatedAt =
            review.getUpdatedAt();

        return response;
    }

    /**
     * 마이페이지나 관리자 화면에서는 익명 리뷰라도
     * 실제 작성자 정보를 확인할 때 사용한다.
     */
    public static ReviewResponse fromForOwner(
        Review review
    ) {
        ReviewResponse response =
            from(review);

        response.writerId =
            review.getUserId();
        response.writerNickname =
            review.getNickname();
        response.writerProfileImageUrl =
            review.getProfileImageUrl();

        return response;
    }
}

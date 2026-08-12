package com.kbait.anchack.review.dto.response;

import com.kbait.anchack.review.domain.Review;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Getter
public class ReviewResponse {

    private static final String ANONYMOUS_NICKNAME = "익명";

    private Long reviewId;
    private Long adminDongId;
    private String adminDongName;
    private String guName;

    private Integer overallRating;
    private String content;
    private Boolean anonymous;
    private String status;

    private Long writerId;
    private String writerNickname;
    private String writerProfileImageUrl;

    private Map<String, Integer> categoryScores;

//    private long likeCount;
//    private long dislikeCount;
//    private String myReaction;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse from(Review review) {
        ReviewResponse response = new ReviewResponse();

        response.reviewId = review.getReviewId();
        response.adminDongId = review.getAdminDongId();
        response.adminDongName = review.getAdminDongName();
        response.guName = review.getGuName();
        response.overallRating = review.getOverallRating();
        response.content = review.getContent();
        response.anonymous = review.getAnonymous();
        response.status = review.getStatus();

        applyWriter(response, review, Boolean.TRUE.equals(review.getAnonymous()));

        response.categoryScores = review.getCategoryScores() == null
            ? Collections.emptyMap()
            : review.getCategoryScores();

//        response.likeCount = review.getLikeCount();
//        response.dislikeCount = review.getDislikeCount();
//        response.myReaction = review.getMyReaction();

        response.createdAt = review.getCreatedAt();
        response.updatedAt = review.getUpdatedAt();

        return response;
    }

    /**
     * 마이페이지나 관리자 화면에서는 익명 리뷰라도
     * 실제 작성자 정보를 확인할 때 사용한다.
     */
    public static ReviewResponse fromForOwner(Review review) {
        ReviewResponse response = from(review);

        applyWriter(response, review, false);

        return response;
    }

    private static void applyWriter(ReviewResponse response, Review review, boolean hideWriter) {
        if (hideWriter) {
            response.writerId = null;
            response.writerNickname = ANONYMOUS_NICKNAME;
            response.writerProfileImageUrl = null;
            return;
        }

        response.writerId = review.getUserId();
        response.writerNickname = review.getNickname();
        response.writerProfileImageUrl = review.getProfileImageUrl();
    }
}

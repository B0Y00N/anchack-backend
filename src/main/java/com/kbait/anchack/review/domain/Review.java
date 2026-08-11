package com.kbait.anchack.review.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class Review {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_HIDDEN = "HIDDEN";
    public static final String STATUS_DELETED = "DELETED";

    private Long reviewId;
    private Long adminDongId;
    private Long userId;

    private Integer overallRating;
    private String content;
    private Boolean anonymous;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // JOIN 조회 결과
    private String nickname;
    private String profileImageUrl;
    private String adminDongName;
    private String guName;

    // review_scores 조회 결과
    private Map<String, Integer> categoryScores =
            new LinkedHashMap<>();

    /*
     * 좋아요/싫어요 기능 임시 비활성화
     *
     * // review_reactions 집계 결과
     * private long likeCount = 0;
     * private long dislikeCount = 0;
     *
     * // 현재 조회 중인 사용자의 반응
     * // "LIKE" / "DISLIKE" / null
     * private String myReaction;
     */

    /**
     * 현재 조회할 수 있는 리뷰인지 판단한다.
     */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /**
     * 전달받은 userId가 리뷰 작성자인지 판단한다.
     */
    public boolean isWrittenBy(Long userId) {
        return userId != null && userId.equals(this.userId);
    }
}

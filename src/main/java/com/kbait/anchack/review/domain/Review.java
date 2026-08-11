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
    private Map<String, Integer> categoryScores = new LinkedHashMap<>();

    // review_reactions(좋아요/싫어요) 집계 결과
    private long likeCount = 0;
    private long dislikeCount = 0;

    // 현재 조회 중인 사용자의 반응("LIKE" / "DISLIKE" / null, 비로그인이면 null)
    private String myReaction;

    /**
     * 현재 조회할 수 있는(숨김/삭제되지 않은) 리뷰인지 판단한다.
     */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /**
     * userId가 이 리뷰의 작성자인지 판단한다.
     */
    public boolean isWrittenBy(Long userId) {
        return userId != null && userId.equals(this.userId);
    }
}

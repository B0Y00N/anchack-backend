package com.kbait.anchack.review.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class Review {

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

    // review_scores 조회 결과
    private Map<String, Integer> categoryScores =
        new LinkedHashMap<>();
}

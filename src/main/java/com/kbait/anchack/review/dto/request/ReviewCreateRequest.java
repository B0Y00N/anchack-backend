package com.kbait.anchack.review.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ReviewCreateRequest {

    private Long adminDongId;
    private Integer overallRating;
    private String content;
    private Boolean anonymous;

    /**
     * 예시:
     * {
     *   "NOISE": 4,
     *   "CLEANLINESS": 5,
     *   "SAFETY": 3,
     *   "ATMOSPHERE": 4,
     *   "TRANSIT": 5
     * }
     */
    private Map<String, Integer> categoryScores =
        new LinkedHashMap<>();
}

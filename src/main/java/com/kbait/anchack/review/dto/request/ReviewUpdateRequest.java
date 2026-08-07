package com.kbait.anchack.review.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ReviewUpdateRequest {

    private Integer overallRating;
    private String content;
    private Boolean anonymous;

    private Map<String, Integer> categoryScores =
        new LinkedHashMap<>();
}

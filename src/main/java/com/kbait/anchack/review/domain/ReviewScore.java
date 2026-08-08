package com.kbait.anchack.review.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewScore {

    private Long reviewId;
    private Long reviewCategoryId;
    private String categoryCode;
    private Integer score;
}

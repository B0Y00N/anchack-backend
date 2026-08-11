package com.kbait.anchack.review.dto.response;

import com.kbait.anchack.review.domain.ReviewCategory;
import lombok.Getter;

@Getter
public class ReviewCategoryResponse {

    private final Long reviewCategoryId;
    private final String code;

    private ReviewCategoryResponse(Long reviewCategoryId, String code) {
        this.reviewCategoryId = reviewCategoryId;
        this.code = code;
    }

    public static ReviewCategoryResponse from(ReviewCategory category) {
        return new ReviewCategoryResponse(category.getReviewCategoryId(), category.getCode());
    }
}

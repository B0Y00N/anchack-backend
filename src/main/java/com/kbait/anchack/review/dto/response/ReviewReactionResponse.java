package com.kbait.anchack.review.dto.response;

import lombok.Getter;

@Getter
public class ReviewReactionResponse {

    private final Long reviewId;
    private final long likeCount;
    private final long dislikeCount;
    private final String myReaction;

    public ReviewReactionResponse(
        Long reviewId,
        long likeCount,
        long dislikeCount,
        String myReaction
    ) {
        this.reviewId = reviewId;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.myReaction = myReaction;
    }
}

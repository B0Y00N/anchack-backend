package com.kbait.anchack.review.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewReactionRequest {

    /**
     * "LIKE" 또는 "DISLIKE"
     */
    private String reactionType;
}

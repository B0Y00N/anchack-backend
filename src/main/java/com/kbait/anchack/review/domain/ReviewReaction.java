package com.kbait.anchack.review.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewReaction {

    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_DISLIKE = "DISLIKE";

    private Long reactionId;
    private Long reviewId;
    private Long userId;
    private String reactionType;
    private LocalDateTime createdAt;

    public boolean isSameType(String otherReactionType) {
        return reactionType != null && reactionType.equals(otherReactionType);
    }
}

package com.kbait.anchack.review.exception;

import com.kbait.anchack.common.exception.ForbiddenException;

public class ReviewAccessDeniedException extends ForbiddenException {

    public ReviewAccessDeniedException(Long reviewId) {
        super("본인이 작성한 리뷰만 변경할 수 있습니다. reviewId=" + reviewId);
    }
}

package com.kbait.anchack.review.exception;

import com.kbait.anchack.common.exception.NotFoundException;

public class ReviewNotFoundException extends NotFoundException {

    public ReviewNotFoundException(Long reviewId) {
        super("리뷰를 찾을 수 없습니다. reviewId=" + reviewId);
    }
}

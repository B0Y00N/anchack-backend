package com.kbait.anchack.user.exception;

import com.kbait.anchack.common.exception.NotFoundException;

/**
 * 요청한 사용자를 찾을 수 없을 때 던진다.
 */
public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(Long userId) {
        super("사용자 정보를 찾을 수 없습니다. userId=" + userId);
    }
}

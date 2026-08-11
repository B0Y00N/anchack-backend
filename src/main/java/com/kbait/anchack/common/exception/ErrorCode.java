package com.kbait.anchack.common.exception;

import lombok.Getter;

/** API 오류 코드. {도메인}_{사유} 형식의 대문자 스네이크로 이 enum 한 곳에서만
 * 정의한다. */
@Getter
public enum ErrorCode {

    AUTH_UNAUTHORIZED("인증 정보가 없습니다."),
    AUTH_FORBIDDEN("접근 권한이 없습니다."),
    COMMON_INVALID_REQUEST("요청 값이 올바르지 않습니다."),
    COMMON_INTERNAL_ERROR("요청 처리 중 오류가 발생했습니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}

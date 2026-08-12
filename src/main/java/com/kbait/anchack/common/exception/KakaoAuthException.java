package com.kbait.anchack.auth.exception;

/**
 * 카카오 API(토큰 발급, 사용자 정보 조회) 연동 중 발생한 오류를 감싼다.
 * 카카오 응답의 상세 내용은 서버 로그에만 남기고, 클라이언트에는
 * GlobalExceptionHandler가 공통 오류 메시지로만 응답한다.
 */
public class KakaoAuthException extends RuntimeException {

    public KakaoAuthException(String message) {
        super(message);
    }

    public KakaoAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}

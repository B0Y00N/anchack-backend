package com.kbait.anchack.route.exception;

/**
 * geocoding 결과가 0건일 때. 목적지 주소 자체가 잘못됐다는 뜻이라 요청 전체를
 * 실패시킨다.
 * IllegalArgumentException을 상속해서 GlobalExceptionHandler의 기존
 * handleBadRequest(400, COMMON_INVALID_REQUEST)로 자연스럽게 처리되게 한다 - common
 * 패키지가 route 같은 특정 도메인 예외를 알 필요가 없도록.
 */
public class AddressNotFoundException extends IllegalArgumentException {

    public AddressNotFoundException(String message) {
        super(message);
    }
}

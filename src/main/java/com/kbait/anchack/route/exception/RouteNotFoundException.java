package com.kbait.anchack.route.exception;

/** 카카오 응답 status가 OK가 아닐 때(NO_RESULTS 등). 해당 후보만 하드필터에서 제외한다. */
public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String message) {
        super(message);
    }
}

package com.kbait.anchack.route.service;

import com.kbait.anchack.route.dto.CommuteResult;

import java.math.BigDecimal;

public interface RouteService {

    CommuteResult calculateCommute(
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            String destAddress,
            String commuteType
    );
}

package com.kbait.anchack.route.service;

import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.Coordinates;

import java.math.BigDecimal;

public interface RouteService {

    Coordinates geocode(String address);

    CommuteResult calculateCommute(
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            BigDecimal destLatitude,
            BigDecimal destLongitude,
            String commuteType
    );
}

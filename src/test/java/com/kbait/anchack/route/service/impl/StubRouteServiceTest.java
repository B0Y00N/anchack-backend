package com.kbait.anchack.route.service.impl;

import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.Coordinates;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StubRouteServiceTest {

    private final StubRouteService service = new StubRouteService();

    @Test
    void geocode는_카카오_호출_없이_고정_좌표를_반환한다() {
        Coordinates result = service.geocode("아무 주소");

        assertThat(result.getLatitude()).isNotNull();
        assertThat(result.getLongitude()).isNotNull();
    }

    @Test
    void calculateCommute는_카카오_호출_없이_고정_통근결과를_반환한다() {
        CommuteResult result = service.calculateCommute(
                new BigDecimal("37.5"), new BigDecimal("127.0"),
                new BigDecimal("37.6"), new BigDecimal("127.1"),
                "대중교통");

        assertThat(result.getCommuteTime()).isNotNull();
        assertThat(result.getTransferCount()).isNotNull();
    }
}

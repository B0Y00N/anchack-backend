package com.kbait.anchack.place.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GeoJsonAdminDongBoundaryRepositoryTest {

    private final AdminDongBoundaryRepository repository =
            new GeoJsonAdminDongBoundaryRepository(new ObjectMapper());

    @Test
    void findsAdminDongBoundaryByWgs84Coordinate() {
        Optional<AdminDongBoundary> actual = repository.findByCoordinate(
                new BigDecimal("37.573888"),
                new BigDecimal("126.970376")
        );

        assertThat(actual).isPresent();
        assertThat(actual.get().getGuCode()).isEqualTo("11010");
        assertThat(actual.get().getDongCode()).isEqualTo("530");
        assertThat(actual.get().getDongName()).isEqualTo("\uC0AC\uC9C1\uB3D9");
    }

    @Test
    void returnsEmptyWhenCoordinateIsOutsideSeoul() {
        Optional<AdminDongBoundary> actual = repository.findByCoordinate(
                new BigDecimal("35.179554"),
                new BigDecimal("129.075642")
        );

        assertThat(actual).isEmpty();
    }
}

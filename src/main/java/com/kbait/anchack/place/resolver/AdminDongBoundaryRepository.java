package com.kbait.anchack.place.resolver;

import java.math.BigDecimal;
import java.util.Optional;

public interface AdminDongBoundaryRepository {

    Optional<AdminDongBoundary> findByCoordinate(
            BigDecimal latitude,
            BigDecimal longitude
    );
}

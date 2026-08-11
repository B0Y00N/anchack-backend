package com.kbait.anchack.place.resolver;

import lombok.Getter;

@Getter
final class ProjectedPoint {

    private final double x;
    private final double y;

    ProjectedPoint(
            double x,
            double y
    ) {
        this.x = x;
        this.y = y;
    }
}

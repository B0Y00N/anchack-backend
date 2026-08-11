package com.kbait.anchack.place.resolver;

import lombok.Getter;

import java.util.List;

@Getter
public final class AdminDongBoundary {

    private final String guCode;
    private final String dongCode;
    private final String dongName;
    private final List<AdminDongPolygon> polygons;
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;

    public AdminDongBoundary(
            String guCode,
            String dongCode,
            String dongName,
            List<AdminDongPolygon> polygons
    ) {
        this.guCode = guCode;
        this.dongCode = dongCode;
        this.dongName = dongName;
        this.polygons = List.copyOf(polygons);

        BoundingBox boundingBox = BoundingBox.from(polygons);
        this.minX = boundingBox.minX;
        this.maxX = boundingBox.maxX;
        this.minY = boundingBox.minY;
        this.maxY = boundingBox.maxY;
    }

    public boolean contains(double x, double y) {
        if (x < minX || x > maxX || y < minY || y > maxY) {
            return false;
        }

        for (AdminDongPolygon polygon : polygons) {
            if (polygon.contains(x, y)) {
                return true;
            }
        }

        return false;
    }

    private static final class BoundingBox {

        private final double minX;
        private final double maxX;
        private final double minY;
        private final double maxY;

        private BoundingBox(
                double minX,
                double maxX,
                double minY,
                double maxY
        ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }

        private static BoundingBox from(List<AdminDongPolygon> polygons) {
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            for (AdminDongPolygon polygon : polygons) {
                minX = Math.min(minX, polygon.getMinX());
                maxX = Math.max(maxX, polygon.getMaxX());
                minY = Math.min(minY, polygon.getMinY());
                maxY = Math.max(maxY, polygon.getMaxY());
            }

            return new BoundingBox(minX, maxX, minY, maxY);
        }
    }
}

package com.kbait.anchack.place.resolver;

import lombok.Getter;

import java.util.List;

@Getter
final class AdminDongPolygon {

    private final List<List<ProjectedPoint>> rings;
    private final double minX;
    private final double maxX;
    private final double minY;
    private final double maxY;

    AdminDongPolygon(List<List<ProjectedPoint>> rings) {
        this.rings = List.copyOf(rings);

        BoundingBox boundingBox = BoundingBox.from(rings);
        this.minX = boundingBox.minX;
        this.maxX = boundingBox.maxX;
        this.minY = boundingBox.minY;
        this.maxY = boundingBox.maxY;
    }

    boolean contains(double x, double y) {
        if (x < minX || x > maxX || y < minY || y > maxY || rings.isEmpty()) {
            return false;
        }
        if (!containsInRing(rings.get(0), x, y)) {
            return false;
        }

        for (int index = 1; index < rings.size(); index++) {
            if (containsInRing(rings.get(index), x, y)) {
                return false;
            }
        }

        return true;
    }

    private boolean containsInRing(List<ProjectedPoint> ring, double x, double y) {
        boolean inside = false;
        int pointCount = ring.size();

        for (int i = 0, j = pointCount - 1; i < pointCount; j = i++) {
            ProjectedPoint current = ring.get(i);
            ProjectedPoint previous = ring.get(j);

            boolean intersects = (current.getY() > y) != (previous.getY() > y)
                    && x < (previous.getX() - current.getX()) * (y - current.getY())
                    / (previous.getY() - current.getY()) + current.getX();
            if (intersects) {
                inside = !inside;
            }
        }

        return inside;
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

        private static BoundingBox from(List<List<ProjectedPoint>> rings) {
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            for (List<ProjectedPoint> ring : rings) {
                for (ProjectedPoint point : ring) {
                    minX = Math.min(minX, point.getX());
                    maxX = Math.max(maxX, point.getX());
                    minY = Math.min(minY, point.getY());
                    maxY = Math.max(maxY, point.getY());
                }
            }

            return new BoundingBox(minX, maxX, minY, maxY);
        }
    }
}

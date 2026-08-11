package com.kbait.anchack.place.resolver;

import java.math.BigDecimal;

final class KoreanCentralBeltProjection {

    private static final double SEMI_MAJOR_AXIS = 6_378_137.0;
    private static final double INVERSE_FLATTENING = 298.257222101;
    private static final double FLATTENING = 1.0 / INVERSE_FLATTENING;
    private static final double ECCENTRICITY_SQUARED = 2 * FLATTENING - FLATTENING * FLATTENING;
    private static final double SECOND_ECCENTRICITY_SQUARED =
            ECCENTRICITY_SQUARED / (1 - ECCENTRICITY_SQUARED);
    private static final double ORIGIN_LATITUDE = Math.toRadians(38.0);
    private static final double ORIGIN_LONGITUDE = Math.toRadians(127.0);
    private static final double FALSE_EASTING = 200_000.0;
    private static final double FALSE_NORTHING = 600_000.0;
    private static final double SCALE_FACTOR = 1.0;

    private KoreanCentralBeltProjection() {
    }

    static ProjectedPoint project(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        double lat = Math.toRadians(latitude.doubleValue());
        double lon = Math.toRadians(longitude.doubleValue());

        double sinLat = Math.sin(lat);
        double cosLat = Math.cos(lat);
        double tanLat = Math.tan(lat);

        double radiusOfCurvature = SEMI_MAJOR_AXIS
                / Math.sqrt(1 - ECCENTRICITY_SQUARED * sinLat * sinLat);
        double tangentSquared = tanLat * tanLat;
        double secondEccentricityTerm = SECOND_ECCENTRICITY_SQUARED * cosLat * cosLat;
        double longitudeDelta = cosLat * (lon - ORIGIN_LONGITUDE);
        double meridianArc = meridianArc(lat);
        double originMeridianArc = meridianArc(ORIGIN_LATITUDE);

        double x = FALSE_EASTING + SCALE_FACTOR * radiusOfCurvature * (
                longitudeDelta
                        + (1 - tangentSquared + secondEccentricityTerm) * Math.pow(longitudeDelta, 3) / 6
                        + (5 - 18 * tangentSquared + tangentSquared * tangentSquared
                        + 72 * secondEccentricityTerm - 58 * SECOND_ECCENTRICITY_SQUARED)
                        * Math.pow(longitudeDelta, 5) / 120
        );

        double y = FALSE_NORTHING + SCALE_FACTOR * (
                meridianArc - originMeridianArc
                        + radiusOfCurvature * tanLat * (
                        Math.pow(longitudeDelta, 2) / 2
                                + (5 - tangentSquared + 9 * secondEccentricityTerm
                                + 4 * secondEccentricityTerm * secondEccentricityTerm)
                                * Math.pow(longitudeDelta, 4) / 24
                                + (61 - 58 * tangentSquared + tangentSquared * tangentSquared
                                + 600 * secondEccentricityTerm - 330 * SECOND_ECCENTRICITY_SQUARED)
                                * Math.pow(longitudeDelta, 6) / 720
                )
        );

        return new ProjectedPoint(x, y);
    }

    private static double meridianArc(double latitude) {
        double e2 = ECCENTRICITY_SQUARED;
        double e4 = e2 * e2;
        double e6 = e4 * e2;

        return SEMI_MAJOR_AXIS * (
                (1 - e2 / 4 - 3 * e4 / 64 - 5 * e6 / 256) * latitude
                        - (3 * e2 / 8 + 3 * e4 / 32 + 45 * e6 / 1024) * Math.sin(2 * latitude)
                        + (15 * e4 / 256 + 45 * e6 / 1024) * Math.sin(4 * latitude)
                        - (35 * e6 / 3072) * Math.sin(6 * latitude)
        );
    }
}

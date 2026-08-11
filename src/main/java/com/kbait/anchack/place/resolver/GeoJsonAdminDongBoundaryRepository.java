package com.kbait.anchack.place.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GeoJsonAdminDongBoundaryRepository implements AdminDongBoundaryRepository {

    private static final String DEFAULT_GEOJSON_PATH = "geo/admin-dong/seoul_dong.geojson";
    private static final int GU_CODE_LENGTH = 5;

    private final List<AdminDongBoundary> boundaries;

    public GeoJsonAdminDongBoundaryRepository(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_GEOJSON_PATH);
    }

    public GeoJsonAdminDongBoundaryRepository(
            ObjectMapper objectMapper,
            String geoJsonPath
    ) {
        this.boundaries = List.copyOf(loadBoundaries(objectMapper, geoJsonPath));
    }

    @Override
    public Optional<AdminDongBoundary> findByCoordinate(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }

        ProjectedPoint projectedPoint = KoreanCentralBeltProjection.project(latitude, longitude);
        for (AdminDongBoundary boundary : boundaries) {
            if (boundary.contains(projectedPoint.getX(), projectedPoint.getY())) {
                return Optional.of(boundary);
            }
        }

        return Optional.empty();
    }

    private List<AdminDongBoundary> loadBoundaries(
            ObjectMapper objectMapper,
            String geoJsonPath
    ) {
        try (InputStream inputStream = new ClassPathResource(geoJsonPath).getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode features = root.path("features");
            List<AdminDongBoundary> loadedBoundaries = new ArrayList<>(features.size());

            for (JsonNode feature : features) {
                loadedBoundaries.add(toBoundary(feature));
            }

            return loadedBoundaries;
        } catch (IOException exception) {
            throw new IllegalStateException("행정동 geojson 파일을 읽을 수 없습니다.", exception);
        }
    }

    private AdminDongBoundary toBoundary(JsonNode feature) {
        JsonNode properties = feature.path("properties");
        String adminCode = properties.path("ADM_CD").asText();
        String dongName = properties.path("ADM_NM").asText();

        return new AdminDongBoundary(
                adminCode.substring(0, GU_CODE_LENGTH),
                adminCode.substring(GU_CODE_LENGTH),
                dongName,
                toPolygons(feature.path("geometry"))
        );
    }

    private List<AdminDongPolygon> toPolygons(JsonNode geometry) {
        String geometryType = geometry.path("type").asText();
        JsonNode coordinates = geometry.path("coordinates");

        if ("Polygon".equals(geometryType)) {
            return List.of(toPolygon(coordinates));
        }
        if ("MultiPolygon".equals(geometryType)) {
            List<AdminDongPolygon> polygons = new ArrayList<>(coordinates.size());
            for (JsonNode polygonNode : coordinates) {
                polygons.add(toPolygon(polygonNode));
            }
            return polygons;
        }

        throw new IllegalStateException("지원하지 않는 geojson geometry type입니다: " + geometryType);
    }

    private AdminDongPolygon toPolygon(JsonNode polygonNode) {
        List<List<ProjectedPoint>> rings = new ArrayList<>(polygonNode.size());
        for (JsonNode ringNode : polygonNode) {
            rings.add(toRing(ringNode));
        }

        return new AdminDongPolygon(rings);
    }

    private List<ProjectedPoint> toRing(JsonNode ringNode) {
        List<ProjectedPoint> ring = new ArrayList<>(ringNode.size());
        for (JsonNode pointNode : ringNode) {
            ring.add(new ProjectedPoint(
                    pointNode.get(0).asDouble(),
                    pointNode.get(1).asDouble()
            ));
        }

        return ring;
    }
}

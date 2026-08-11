package com.kbait.anchack.place.normalizer;

import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.exception.InvalidPlaceDataException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PlaceNormalizer {

    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_EXTERNAL_ID_LENGTH = 200;
    private static final int COORDINATE_SCALE = 6;
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    public Place normalize(ExternalPlace externalPlace) {
        if (externalPlace == null) {
            throw new InvalidPlaceDataException("externalPlace는 null일 수 없습니다.");
        }

        Long dataSourceId = requirePositiveId(externalPlace.getDataSourceId(), "dataSourceId");
        PlaceCategory category = requireCategory(externalPlace.getPlaceCategory());
        String name = normalizeName(externalPlace.getName());
        BigDecimal latitude = normalizeLatitude(externalPlace.getLatitude());
        BigDecimal longitude = normalizeLongitude(externalPlace.getLongitude());

        return Place.builder()
                .id(null)
                .externalId(normalizeExternalId(
                        externalPlace.getSourcePlaceId(),
                        dataSourceId,
                        category,
                        name,
                        latitude,
                        longitude
                ))
                .adminDongId(null)
                .category(category)
                .name(name)
                .latitude(latitude)
                .longitude(longitude)
                .dataSourceId(dataSourceId)
                .build();
    }

    private Long requirePositiveId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new InvalidPlaceDataException(fieldName + "는 0보다 커야 합니다.");
        }

        return value;
    }

    private PlaceCategory requireCategory(PlaceCategory category) {
        if (category == null) {
            throw new InvalidPlaceDataException("placeCategory는 null일 수 없습니다.");
        }

        return category;
    }

    private String normalizeName(String name) {
        String normalizedName = requireText(name, "name");
        int length = normalizedName.codePointCount(0, normalizedName.length());

        if (length > MAX_NAME_LENGTH) {
            throw new InvalidPlaceDataException("name은 200자를 초과할 수 없습니다.");
        }

        return normalizedName;
    }

    private BigDecimal normalizeLatitude(String latitude) {
        return normalizeCoordinate(latitude, "latitude", MIN_LATITUDE, MAX_LATITUDE);
    }

    private BigDecimal normalizeLongitude(String longitude) {
        return normalizeCoordinate(longitude, "longitude", MIN_LONGITUDE, MAX_LONGITUDE);
    }

    private BigDecimal normalizeCoordinate(
            String coordinate,
            String fieldName,
            BigDecimal minimum,
            BigDecimal maximum
    ) {
        String normalizedCoordinate = requireText(coordinate, fieldName);

        try {
            BigDecimal parsedCoordinate = new BigDecimal(normalizedCoordinate);

            if (parsedCoordinate.compareTo(minimum) < 0 || parsedCoordinate.compareTo(maximum) > 0) {
                throw new InvalidPlaceDataException(fieldName + "의 범위가 올바르지 않습니다.");
            }

            return parsedCoordinate.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new InvalidPlaceDataException(fieldName + " 숫자 형식이 올바르지 않습니다.", exception);
        }
    }

    private String normalizeExternalId(
            String sourcePlaceId,
            Long dataSourceId,
            PlaceCategory category,
            String name,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        if (sourcePlaceId == null || sourcePlaceId.trim().isEmpty()) {
            return generateExternalId(dataSourceId, category, name, latitude, longitude);
        }

        String normalizedExternalId = sourcePlaceId.trim();
        int length = normalizedExternalId.codePointCount(0, normalizedExternalId.length());

        if (length > MAX_EXTERNAL_ID_LENGTH) {
            throw new InvalidPlaceDataException("sourcePlaceId는 200자를 초과할 수 없습니다.");
        }

        return normalizedExternalId;
    }

    private String generateExternalId(
            Long dataSourceId,
            PlaceCategory category,
            String name,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        String source = String.join(
                "|",
                String.valueOf(dataSourceId),
                category.name(),
                name,
                latitude.toPlainString(),
                longitude.toPlainString()
        );
        return sha256Hex(source);
    }

    private String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);

            for (byte valueByte : digest) {
                result.append(String.format("%02x", valueByte));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
    

    private String requireText(String value, String fieldName) {
        if (value == null) {
            throw new InvalidPlaceDataException(fieldName + "은 null일 수 없습니다.");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new InvalidPlaceDataException(fieldName + "은 빈 값일 수 없습니다.");
        }

        return normalizedValue;
    }
}

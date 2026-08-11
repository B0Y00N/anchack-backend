package com.kbait.anchack.place.resolver;

import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.mapper.PlaceAdminDongMapper;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@RequiredArgsConstructor
public final class PlaceAdminDongResolverImpl implements PlaceAdminDongResolver {

    private final PlaceAdminDongMapper placeAdminDongMapper;
    private final AdminDongBoundaryRepository boundaryRepository;

    @Override
    public Optional<Long> resolveAdminDongId(ExternalPlace externalPlace) {
        if (externalPlace == null) {
            throw new IllegalArgumentException("externalPlace는 null일 수 없습니다.");
        }

        Optional<Long> resolvedByCoordinate = resolveByCoordinate(externalPlace);
        if (resolvedByCoordinate.isPresent()) {
            return resolvedByCoordinate;
        }

        return resolveByAddress(externalPlace);
    }

    private Optional<Long> resolveByAddress(ExternalPlace externalPlace) {
        String dongName = firstText(externalPlace.getAdminDongName(), extractDongName(externalPlace.getAddress()));
        String guName = firstText(extractGuName(externalPlace.getAddress()), extractGuName(externalPlace.getRoadAddress()));

        if (dongName == null || guName == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(placeAdminDongMapper.findIdByGuNameAndDongName(guName, dongName));
    }

    private Optional<Long> resolveByCoordinate(ExternalPlace externalPlace) {
        Optional<Coordinate> coordinate = parseCoordinate(externalPlace);
        if (coordinate.isEmpty()) {
            return Optional.empty();
        }

        return boundaryRepository.findByCoordinate(
                        coordinate.get().latitude,
                        coordinate.get().longitude
                )
                .flatMap(this::findAdminDongId);
    }

    private Optional<Long> findAdminDongId(AdminDongBoundary boundary) {
        return Optional.ofNullable(placeAdminDongMapper.findIdByGuCodeAndDongCode(
                boundary.getGuCode(),
                boundary.getDongCode()
        ));
    }

    private Optional<Coordinate> parseCoordinate(ExternalPlace externalPlace) {
        if (isBlank(externalPlace.getLatitude()) || isBlank(externalPlace.getLongitude())) {
            return Optional.empty();
        }

        try {
            return Optional.of(new Coordinate(
                    new BigDecimal(externalPlace.getLatitude().trim()),
                    new BigDecimal(externalPlace.getLongitude().trim())
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String extractGuName(String address) {
        if (isBlank(address)) {
            return null;
        }

        for (String token : address.trim().split("\\s+")) {
            if (token.endsWith("구")) {
                return token;
            }
        }

        return null;
    }

    private String extractDongName(String address) {
        if (isBlank(address)) {
            return null;
        }

        String[] tokens = address.trim().split("\\s+");
        for (int index = tokens.length - 1; index >= 0; index--) {
            if (tokens[index].endsWith("동")) {
                return tokens[index];
            }
        }

        return null;
    }

    private String firstText(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }
        if (!isBlank(second)) {
            return second.trim();
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class Coordinate {

        private final BigDecimal latitude;
        private final BigDecimal longitude;

        private Coordinate(
                BigDecimal latitude,
                BigDecimal longitude
        ) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}

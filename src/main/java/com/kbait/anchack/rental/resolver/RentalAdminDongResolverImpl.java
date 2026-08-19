package com.kbait.anchack.rental.resolver;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.place.resolver.AdminDongBoundary;
import com.kbait.anchack.place.resolver.AdminDongBoundaryRepository;
import com.kbait.anchack.rental.client.MolitRentApiCategory;
import com.kbait.anchack.rental.dto.external.RawRentalTransaction;
import com.kbait.anchack.rental.exception.InvalidMolitRentDataException;
import com.kbait.anchack.rental.registry.SeoulLawdCodeRegistry;
import com.kbait.anchack.route.client.KakaoGeocodingClient;
import com.kbait.anchack.route.dto.Coordinates;
import com.kbait.anchack.route.exception.AddressNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RentalAdminDongResolverImpl implements RentalAdminDongResolver {

    private final KakaoGeocodingClient kakaoGeocodingClient;
    private final AdminDongBoundaryRepository boundaryRepository;
    private final AdminDongMapper adminDongMapper;
    private final SeoulLawdCodeRegistry lawdCodeRegistry;
    private final Object adminDongIndexLock = new Object();

    private volatile Map<AdminDongCodeKey, Long> adminDongIdsByCode;

    public RentalAdminDongResolverImpl(
            KakaoGeocodingClient kakaoGeocodingClient,
            AdminDongBoundaryRepository boundaryRepository,
            AdminDongMapper adminDongMapper,
            SeoulLawdCodeRegistry lawdCodeRegistry
    ) {
        this.kakaoGeocodingClient = Objects.requireNonNull(
                kakaoGeocodingClient,
                "kakaoGeocodingClient는 null일 수 없습니다."
        );
        this.boundaryRepository = Objects.requireNonNull(
                boundaryRepository,
                "boundaryRepository는 null일 수 없습니다."
        );
        this.adminDongMapper = Objects.requireNonNull(
                adminDongMapper,
                "adminDongMapper는 null일 수 없습니다."
        );
        this.lawdCodeRegistry = Objects.requireNonNull(
                lawdCodeRegistry,
                "lawdCodeRegistry는 null일 수 없습니다."
        );
    }

    @Override
    public ResolutionSession openSession() {
        return new ResolutionSessionImpl(getOrLoadAdminDongIndex());
    }

    private Map<AdminDongCodeKey, Long> getOrLoadAdminDongIndex() {
        Map<AdminDongCodeKey, Long> currentIndex = adminDongIdsByCode;
        if (currentIndex != null) {
            return currentIndex;
        }

        synchronized (adminDongIndexLock) {
            currentIndex = adminDongIdsByCode;
            if (currentIndex == null) {
                currentIndex = loadAdminDongIndex();
                adminDongIdsByCode = currentIndex;
            }
            return currentIndex;
        }
    }

    private Map<AdminDongCodeKey, Long> loadAdminDongIndex() {
        List<AdminDong> adminDongs = adminDongMapper.findAllCodeMappings();
        if (adminDongs == null || adminDongs.isEmpty()) {
            throw new IllegalStateException("행정동 코드 인덱스를 구성할 DB 행이 없습니다.");
        }

        Map<AdminDongCodeKey, Long> loadedIndex = new HashMap<>();
        for (AdminDong adminDong : adminDongs) {
            if (adminDong == null
                    || adminDong.getAdminDongId() == null
                    || isBlank(adminDong.getGuCode())
                    || isBlank(adminDong.getDongCode())) {
                throw new IllegalStateException("행정동 코드 인덱스 행에 필수값이 없습니다.");
            }

            AdminDongCodeKey key = new AdminDongCodeKey(
                    adminDong.getGuCode().trim(),
                    adminDong.getDongCode().trim()
            );
            Long previousId = loadedIndex.putIfAbsent(key, adminDong.getAdminDongId());
            if (previousId != null) {
                throw new IllegalStateException("중복된 행정 구·동 코드가 있습니다.");
            }
        }

        return Map.copyOf(loadedIndex);
    }

    private final class ResolutionSessionImpl implements ResolutionSession {

        private final Map<AdminDongCodeKey, Long> adminDongIndex;
        private final Map<AddressCacheKey, RentalAdminDongResolution> addressCache = new HashMap<>();
        private boolean closed;

        private ResolutionSessionImpl(Map<AdminDongCodeKey, Long> adminDongIndex) {
            this.adminDongIndex = adminDongIndex;
        }

        @Override
        public RentalAdminDongResolution resolve(RawRentalTransaction rawTransaction) {
            ensureOpen();
            validateRawTransaction(rawTransaction);

            String jibun = trimToNull(rawTransaction.getJibun());
            if (jibun == null) {
                return RentalAdminDongResolution.unmapped(
                        RentalAdminDongResolution.Status.JIBUN_MISSING
                );
            }

            AddressCacheKey cacheKey = new AddressCacheKey(
                    requireText(rawTransaction.getGuCode(), "guCode"),
                    requireText(rawTransaction.getLegalDongName(), "legalDongName"),
                    jibun
            );
            RentalAdminDongResolution cachedResolution = addressCache.get(cacheKey);
            if (cachedResolution != null) {
                return cachedResolution;
            }

            RentalAdminDongResolution resolution = resolveAddress(cacheKey);
            addressCache.put(cacheKey, resolution);
            return resolution;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            addressCache.clear();
        }

        private RentalAdminDongResolution resolveAddress(AddressCacheKey cacheKey) {
            String address = "서울특별시 "
                    + lawdCodeRegistry.requireGuName(cacheKey.guCode)
                    + " " + cacheKey.legalDongName
                    + " " + cacheKey.jibun;

            Coordinates coordinates;
            try {
                coordinates = kakaoGeocodingClient.geocode(address);
            } catch (AddressNotFoundException exception) {
                return RentalAdminDongResolution.unmapped(
                        RentalAdminDongResolution.Status.ADDRESS_NOT_FOUND
                );
            }

            Optional<AdminDongBoundary> boundary = boundaryRepository.findByCoordinate(
                    coordinates.getLatitude(),
                    coordinates.getLongitude()
            );
            if (boundary.isEmpty()) {
                return RentalAdminDongResolution.unmapped(
                        RentalAdminDongResolution.Status.BOUNDARY_NOT_FOUND
                );
            }

            Long adminDongId = adminDongIndex.get(new AdminDongCodeKey(
                    boundary.get().getGuCode(),
                    boundary.get().getDongCode()
            ));
            if (adminDongId == null) {
                return RentalAdminDongResolution.unmapped(
                        RentalAdminDongResolution.Status.ADMIN_DONG_NOT_FOUND
                );
            }

            return RentalAdminDongResolution.mapped(adminDongId);
        }

        private void ensureOpen() {
            if (closed) {
                throw new IllegalStateException("이미 닫힌 행정동 매핑 session입니다.");
            }
        }
    }

    private void validateRawTransaction(RawRentalTransaction rawTransaction) {
        if (rawTransaction == null) {
            throw new InvalidMolitRentDataException("rawTransaction은 null일 수 없습니다.");
        }
        MolitRentApiCategory apiCategory = rawTransaction.getApiCategory();
        if (apiCategory == null) {
            throw new InvalidMolitRentDataException("apiCategory는 null일 수 없습니다.");
        }
    }

    private String requireText(String value, String fieldName) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            throw new InvalidMolitRentDataException(fieldName + "은 비어 있을 수 없습니다.");
        }
        return normalizedValue;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record AddressCacheKey(String guCode, String legalDongName, String jibun) {
    }

    private record AdminDongCodeKey(String guCode, String dongCode) {
    }
}

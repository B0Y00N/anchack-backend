package com.kbait.anchack.place.resolver;

import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.mapper.PlaceAdminDongMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceAdminDongResolverTest {

    private static final String GWANAK_GU = "\uAD00\uC545\uAD6C";
    private static final String CHEONGNYONG_DONG = "\uCCAD\uB8E1\uB3D9";
    private static final String BONGCHEON_DONG = "\uBD09\uCC9C\uB3D9";
    private static final String SILLIM_DONG = "\uC2E0\uB9BC\uB3D9";

    @Mock
    private PlaceAdminDongMapper placeAdminDongMapper;

    @Mock
    private AdminDongBoundaryRepository boundaryRepository;

    private PlaceAdminDongResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PlaceAdminDongResolverImpl(placeAdminDongMapper, boundaryRepository);
    }

    @Test
    void coordinateResolveAdminDongIdFirst() {
        ExternalPlace externalPlace = validPlaceBuilder().build();
        AdminDongBoundary boundary = new AdminDongBoundary(
                "11210",
                "610",
                CHEONGNYONG_DONG,
                java.util.List.of()
        );
        when(boundaryRepository.findByCoordinate(
                new BigDecimal("37.478154"),
                new BigDecimal("126.951484")
        )).thenReturn(Optional.of(boundary));
        when(placeAdminDongMapper.findIdByGuCodeAndDongCode("11210", "610"))
                .thenReturn(123L);

        Optional<Long> actual = resolver.resolveAdminDongId(externalPlace);

        assertThat(actual).contains(123L);
        verify(placeAdminDongMapper).findIdByGuCodeAndDongCode("11210", "610");
        verifyNoMoreInteractions(placeAdminDongMapper);
    }

    @Test
    void adminDongNameAndAddressGuNameResolveAdminDongIdWhenCoordinateFails() {
        ExternalPlace externalPlace = validPlaceBuilder()
                .address("서울 " + GWANAK_GU + " " + BONGCHEON_DONG)
                .adminDongName(CHEONGNYONG_DONG)
                .build();
        when(boundaryRepository.findByCoordinate(
                new BigDecimal("37.478154"),
                new BigDecimal("126.951484")
        )).thenReturn(Optional.empty());
        when(placeAdminDongMapper.findIdByGuNameAndDongName(GWANAK_GU, CHEONGNYONG_DONG))
                .thenReturn(123L);

        Optional<Long> actual = resolver.resolveAdminDongId(externalPlace);

        assertThat(actual).contains(123L);
        verify(placeAdminDongMapper).findIdByGuNameAndDongName(GWANAK_GU, CHEONGNYONG_DONG);
    }

    @Test
    void roadAddressGuNameIsUsedWhenAddressHasNoGuName() {
        ExternalPlace externalPlace = validPlaceBuilder()
                .address(BONGCHEON_DONG)
                .roadAddress("서울 " + GWANAK_GU + " 관악로 1")
                .adminDongName(CHEONGNYONG_DONG)
                .latitude("")
                .longitude("")
                .build();
        when(placeAdminDongMapper.findIdByGuNameAndDongName(GWANAK_GU, CHEONGNYONG_DONG))
                .thenReturn(123L);

        Optional<Long> actual = resolver.resolveAdminDongId(externalPlace);

        assertThat(actual).contains(123L);
    }

    @Test
    void dongNameCanBeExtractedFromAddressWhenAdminDongNameIsAbsent() {
        ExternalPlace externalPlace = validPlaceBuilder()
                .address("서울 " + GWANAK_GU + " " + SILLIM_DONG)
                .adminDongName(null)
                .latitude("")
                .longitude("")
                .build();
        when(placeAdminDongMapper.findIdByGuNameAndDongName(GWANAK_GU, SILLIM_DONG))
                .thenReturn(456L);

        Optional<Long> actual = resolver.resolveAdminDongId(externalPlace);

        assertThat(actual).contains(456L);
    }

    @Test
    void addressFallbackIsUsedWhenCoordinateMapperMisses() {
        ExternalPlace externalPlace = validPlaceBuilder()
                .address("서울 " + GWANAK_GU + " " + BONGCHEON_DONG)
                .adminDongName(CHEONGNYONG_DONG)
                .latitude("37.573888")
                .longitude("126.970376")
                .build();
        AdminDongBoundary boundary = new AdminDongBoundary(
                "11010",
                "530",
                "\uC0AC\uC9C1\uB3D9",
                java.util.List.of()
        );
        when(boundaryRepository.findByCoordinate(
                new BigDecimal("37.573888"),
                new BigDecimal("126.970376")
        )).thenReturn(Optional.of(boundary));
        when(placeAdminDongMapper.findIdByGuCodeAndDongCode("11010", "530"))
                .thenReturn(null);
        when(placeAdminDongMapper.findIdByGuNameAndDongName(GWANAK_GU, CHEONGNYONG_DONG))
                .thenReturn(123L);

        Optional<Long> actual = resolver.resolveAdminDongId(externalPlace);

        assertThat(actual).contains(123L);
    }

    @Test
    void unresolvedInputReturnsEmpty() {
        ExternalPlace externalPlace = validPlaceBuilder()
                .address("")
                .roadAddress("")
                .adminDongName(null)
                .latitude("")
                .longitude("")
                .build();

        Optional<Long> actual = resolver.resolveAdminDongId(externalPlace);

        assertThat(actual).isEmpty();
        verifyNoInteractions(placeAdminDongMapper, boundaryRepository);
    }

    @Test
    void mapperMissReturnsEmpty() {
        ExternalPlace externalPlace = validPlaceBuilder()
                .address("서울 " + GWANAK_GU + " " + BONGCHEON_DONG)
                .adminDongName(CHEONGNYONG_DONG)
                .build();
        when(boundaryRepository.findByCoordinate(
                new BigDecimal("37.478154"),
                new BigDecimal("126.951484")
        )).thenReturn(Optional.empty());
        when(placeAdminDongMapper.findIdByGuNameAndDongName(GWANAK_GU, CHEONGNYONG_DONG))
                .thenReturn(null);

        Optional<Long> actual = resolver.resolveAdminDongId(externalPlace);

        assertThat(actual).isEmpty();
    }

    @Test
    void externalPlaceIsRequired() {
        Throwable actual = catchThrowable(() -> resolver.resolveAdminDongId(null));

        assertThat(actual).isExactlyInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(placeAdminDongMapper, boundaryRepository);
    }

    private ExternalPlace.ExternalPlaceBuilder validPlaceBuilder() {
        return ExternalPlace.builder()
                .dataSourceId(13L)
                .sourcePlaceId("kakao-123")
                .placeCategory(PlaceCategory.PHARMACY)
                .name("test pharmacy")
                .rawCategoryCode("PM9")
                .rawCategoryName("medical > pharmacy")
                .address("서울 " + GWANAK_GU + " " + BONGCHEON_DONG)
                .roadAddress("서울 " + GWANAK_GU + " 관악로 1")
                .latitude("37.478154")
                .longitude("126.951484");
    }
}

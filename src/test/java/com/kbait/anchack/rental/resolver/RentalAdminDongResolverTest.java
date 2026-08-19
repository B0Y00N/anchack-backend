package com.kbait.anchack.rental.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.place.resolver.AdminDongBoundary;
import com.kbait.anchack.place.resolver.AdminDongBoundaryRepository;
import com.kbait.anchack.place.resolver.GeoJsonAdminDongBoundaryRepository;
import com.kbait.anchack.rental.client.MolitRentApiCategory;
import com.kbait.anchack.rental.dto.external.RawRentalTransaction;
import com.kbait.anchack.rental.registry.SeoulLawdCodeRegistry;
import com.kbait.anchack.route.client.KakaoGeocodingClient;
import com.kbait.anchack.route.dto.Coordinates;
import com.kbait.anchack.route.exception.AddressNotFoundException;
import com.kbait.anchack.route.exception.KakaoRouteApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalAdminDongResolverTest {

    private static final Coordinates SAJIK_DONG_COORDINATES = Coordinates.builder()
            .latitude(new BigDecimal("37.573888"))
            .longitude(new BigDecimal("126.970376"))
            .build();

    @Mock
    private KakaoGeocodingClient kakaoGeocodingClient;

    @Mock
    private AdminDongBoundaryRepository boundaryRepository;

    @Mock
    private AdminDongMapper adminDongMapper;

    private RentalAdminDongResolverImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new RentalAdminDongResolverImpl(
                kakaoGeocodingClient,
                boundaryRepository,
                adminDongMapper,
                new SeoulLawdCodeRegistry()
        );
    }

    @Test
    void 생성만으로는_DB를_조회하지_않는다() {
        verifyNoInteractions(adminDongMapper);
    }

    @Test
    void 주소를_좌표와_실제_GeoJSON_경계로_판별하고_ID를_찾는다() {
        when(adminDongMapper.findAllCodeMappings()).thenReturn(List.of(adminDong(77L, "11010", "530")));
        when(kakaoGeocodingClient.geocode("서울특별시 관악구 봉천동 881-26"))
                .thenReturn(SAJIK_DONG_COORDINATES);
        RentalAdminDongResolver geoJsonResolver = new RentalAdminDongResolverImpl(
                kakaoGeocodingClient,
                new GeoJsonAdminDongBoundaryRepository(new ObjectMapper()),
                adminDongMapper,
                new SeoulLawdCodeRegistry()
        );

        try (RentalAdminDongResolver.ResolutionSession session = geoJsonResolver.openSession()) {
            RentalAdminDongResolution result = session.resolve(rawTransaction("11620", "봉천동", "881-26"));

            assertThat(result.getStatus()).isEqualTo(RentalAdminDongResolution.Status.MAPPED);
            assertThat(result.getAdminDongId()).isEqualTo(77L);
        }
    }

    @Test
    void 동일_session의_동일_주소는_성공_결과를_캐시한다() {
        stubMappedResolution(10L);

        try (RentalAdminDongResolver.ResolutionSession session = resolver.openSession()) {
            RentalAdminDongResolution first = session.resolve(rawTransaction("11620", "봉천동", "881-26"));
            RentalAdminDongResolution second = session.resolve(rawTransaction("11620", "봉천동", "881-26"));

            assertThat(first.getAdminDongId()).isEqualTo(10L);
            assertThat(second).isSameAs(first);
        }

        verify(kakaoGeocodingClient, times(1)).geocode("서울특별시 관악구 봉천동 881-26");
        verify(adminDongMapper, times(1)).findAllCodeMappings();
    }

    @Test
    void 주소_검색_결과_없음도_동일_session에서_캐시한다() {
        when(adminDongMapper.findAllCodeMappings()).thenReturn(List.of(adminDong(10L, "11210", "650")));
        when(kakaoGeocodingClient.geocode("서울특별시 관악구 봉천동 881-26"))
                .thenThrow(new AddressNotFoundException("테스트 주소 없음"));

        try (RentalAdminDongResolver.ResolutionSession session = resolver.openSession()) {
            RentalAdminDongResolution first = session.resolve(rawTransaction("11620", "봉천동", "881-26"));
            RentalAdminDongResolution second = session.resolve(rawTransaction("11620", "봉천동", "881-26"));

            assertThat(first.getStatus()).isEqualTo(RentalAdminDongResolution.Status.ADDRESS_NOT_FOUND);
            assertThat(second).isSameAs(first);
        }

        verify(kakaoGeocodingClient, times(1)).geocode("서울특별시 관악구 봉천동 881-26");
        verifyNoInteractions(boundaryRepository);
    }

    @Test
    void 지번이_없으면_카카오와_경계를_호출하지_않는다() {
        when(adminDongMapper.findAllCodeMappings()).thenReturn(List.of(adminDong(10L, "11210", "650")));

        try (RentalAdminDongResolver.ResolutionSession session = resolver.openSession()) {
            RentalAdminDongResolution result = session.resolve(rawTransaction("11620", "봉천동", null));

            assertThat(result.getStatus()).isEqualTo(RentalAdminDongResolution.Status.JIBUN_MISSING);
            assertThat(result.getAdminDongId()).isNull();
        }

        verifyNoInteractions(kakaoGeocodingClient, boundaryRepository);
    }

    @Test
    void 법정동명과_지번이_같아도_구_코드가_다르면_별도_주소다() {
        stubMappedResolution(10L);

        try (RentalAdminDongResolver.ResolutionSession session = resolver.openSession()) {
            session.resolve(rawTransaction("11620", "신사동", "1-1"));
            session.resolve(rawTransaction("11680", "신사동", "1-1"));
        }

        verify(kakaoGeocodingClient).geocode("서울특별시 관악구 신사동 1-1");
        verify(kakaoGeocodingClient).geocode("서울특별시 강남구 신사동 1-1");
    }

    @Test
    void 새_session에서는_주소를_다시_resolution한다() {
        stubMappedResolution(10L);
        RawRentalTransaction transaction = rawTransaction("11620", "봉천동", "881-26");

        try (RentalAdminDongResolver.ResolutionSession firstSession = resolver.openSession()) {
            firstSession.resolve(transaction);
        }
        try (RentalAdminDongResolver.ResolutionSession secondSession = resolver.openSession()) {
            secondSession.resolve(transaction);
        }

        verify(kakaoGeocodingClient, times(2)).geocode("서울특별시 관악구 봉천동 881-26");
        verify(adminDongMapper, times(1)).findAllCodeMappings();
    }

    @Test
    void 카카오_API_장애는_전파하고_캐시하지_않는다() {
        when(adminDongMapper.findAllCodeMappings()).thenReturn(List.of(adminDong(10L, "11210", "650")));
        KakaoRouteApiException failure = new KakaoRouteApiException("카카오 장애");
        when(kakaoGeocodingClient.geocode("서울특별시 관악구 봉천동 881-26"))
                .thenThrow(failure)
                .thenReturn(SAJIK_DONG_COORDINATES);
        when(boundaryRepository.findByCoordinate(
                SAJIK_DONG_COORDINATES.getLatitude(),
                SAJIK_DONG_COORDINATES.getLongitude()
        )).thenReturn(Optional.of(boundary("11210", "650")));

        try (RentalAdminDongResolver.ResolutionSession session = resolver.openSession()) {
            assertThatThrownBy(() -> session.resolve(rawTransaction("11620", "봉천동", "881-26")))
                    .isSameAs(failure);
            assertThat(session.resolve(rawTransaction("11620", "봉천동", "881-26")).getAdminDongId())
                    .isEqualTo(10L);
        }

        verify(kakaoGeocodingClient, times(2)).geocode("서울특별시 관악구 봉천동 881-26");
    }

    @Test
    void 경계_없음과_DB_ID_없음을_각각_구분한다() {
        when(adminDongMapper.findAllCodeMappings()).thenReturn(List.of(adminDong(10L, "11210", "650")));
        when(kakaoGeocodingClient.geocode("서울특별시 관악구 봉천동 1-1"))
                .thenReturn(SAJIK_DONG_COORDINATES);
        when(kakaoGeocodingClient.geocode("서울특별시 관악구 봉천동 2-2"))
                .thenReturn(SAJIK_DONG_COORDINATES);
        when(boundaryRepository.findByCoordinate(
                SAJIK_DONG_COORDINATES.getLatitude(),
                SAJIK_DONG_COORDINATES.getLongitude()
        )).thenReturn(Optional.empty()).thenReturn(Optional.of(boundary("99999", "999")));

        try (RentalAdminDongResolver.ResolutionSession session = resolver.openSession()) {
            assertThat(session.resolve(rawTransaction("11620", "봉천동", "1-1")).getStatus())
                    .isEqualTo(RentalAdminDongResolution.Status.BOUNDARY_NOT_FOUND);
            assertThat(session.resolve(rawTransaction("11620", "봉천동", "2-2")).getStatus())
                    .isEqualTo(RentalAdminDongResolution.Status.ADMIN_DONG_NOT_FOUND);
        }
    }

    @Test
    void close는_멱등적이고_닫힌_session의_resolve만_거부한다() {
        when(adminDongMapper.findAllCodeMappings()).thenReturn(List.of(adminDong(10L, "11210", "650")));
        RentalAdminDongResolver.ResolutionSession session = resolver.openSession();

        session.close();
        session.close();

        assertThatThrownBy(() -> session.resolve(rawTransaction("11620", "봉천동", null)))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("닫힌");
    }

    @Test
    void 동시_최초_session_생성이_성공하면_Mapper를_한_번만_호출한다() throws Exception {
        when(adminDongMapper.findAllCodeMappings()).thenReturn(List.of(adminDong(10L, "11210", "650")));
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<?> first = executorService.submit(() -> openAndCloseAfter(startLatch));
            Future<?> second = executorService.submit(() -> openAndCloseAfter(startLatch));
            startLatch.countDown();
            first.get();
            second.get();
        } finally {
            executorService.shutdownNow();
        }

        verify(adminDongMapper, times(1)).findAllCodeMappings();
    }

    @Test
    void 인덱스_적재_실패는_publish하지_않고_다음_session에서_재시도한다() {
        IllegalStateException failure = new IllegalStateException("DB 장애");
        when(adminDongMapper.findAllCodeMappings())
                .thenThrow(failure)
                .thenReturn(List.of(adminDong(10L, "11210", "650")));

        assertThatThrownBy(resolver::openSession).isSameAs(failure);
        try (RentalAdminDongResolver.ResolutionSession ignored = resolver.openSession()) {
            assertThat(ignored).isNotNull();
        }

        verify(adminDongMapper, times(2)).findAllCodeMappings();
    }

    private void openAndCloseAfter(CountDownLatch startLatch) {
        try {
            startLatch.await();
            resolver.openSession().close();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private void stubMappedResolution(long adminDongId) {
        when(adminDongMapper.findAllCodeMappings())
                .thenReturn(List.of(adminDong(adminDongId, "11210", "650")));
        when(kakaoGeocodingClient.geocode(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(SAJIK_DONG_COORDINATES);
        when(boundaryRepository.findByCoordinate(
                SAJIK_DONG_COORDINATES.getLatitude(),
                SAJIK_DONG_COORDINATES.getLongitude()
        )).thenReturn(Optional.of(boundary("11210", "650")));
    }

    private RawRentalTransaction rawTransaction(String guCode, String legalDongName, String jibun) {
        return RawRentalTransaction.builder()
                .apiCategory(MolitRentApiCategory.ROW_HOUSE)
                .guCode(guCode)
                .legalDongName(legalDongName)
                .jibun(jibun)
                .build();
    }

    private AdminDong adminDong(Long id, String guCode, String dongCode) {
        AdminDong adminDong = new AdminDong();
        adminDong.setAdminDongId(id);
        adminDong.setGuCode(guCode);
        adminDong.setDongCode(dongCode);
        return adminDong;
    }

    private AdminDongBoundary boundary(String guCode, String dongCode) {
        return new AdminDongBoundary(guCode, dongCode, "테스트동", List.of());
    }
}

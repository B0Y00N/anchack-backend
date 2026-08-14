package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.AdminDongLocation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.RecommendationAdminDongMapper;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.Coordinates;
import com.kbait.anchack.route.exception.KakaoRouteApiException;
import com.kbait.anchack.route.exception.RouteNotFoundException;
import com.kbait.anchack.route.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommuteFilterTest {

    private static final Coordinates DEST = Coordinates.builder()
            .latitude(new BigDecimal("37.6"))
            .longitude(new BigDecimal("127.1"))
            .build();

    @Mock
    private RecommendationAdminDongMapper adminDongMapper;

    @Mock
    private RouteService routeService;

    private CommuteFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CommuteFilter(adminDongMapper, routeService);
    }

    @Test
    void destAddress가_없으면_geocoding_없이_전체_후보를_통과시킨다() {
        List<Long> candidates = List.of(1L, 2L);

        List<RecommendationCandidate> result = filter.filter(candidates, null, "대중교통", 60, 2);

        assertThat(result).extracting(RecommendationCandidate::getAdminDongId).containsExactly(1L, 2L);
        verify(routeService, never()).geocode(any());
    }

    @Test
    void destAddress가_있으면_geocoding을_한_번만_호출한다() {
        List<Long> candidates = List.of(1L, 2L);
        when(routeService.geocode("목적지")).thenReturn(DEST);
        when(adminDongMapper.findLocationsByIds(candidates)).thenReturn(List.of(
                location(1L, "37.5", "127.0"),
                location(2L, "37.55", "127.05")
        ));
        when(routeService.calculateCommute(
                any(), any(), eq(DEST.getLatitude()), eq(DEST.getLongitude()), eq("대중교통")))
                .thenReturn(CommuteResult.builder().commuteTime(20).transferCount(1).build());

        filter.filter(candidates, "목적지", "대중교통", 60, 2);

        verify(routeService, times(1)).geocode("목적지");
    }

    @Test
    void 경로를_찾지_못한_후보는_결과에서_제외된다() {
        List<Long> candidates = List.of(1L, 2L);
        when(routeService.geocode("목적지")).thenReturn(DEST);
        when(adminDongMapper.findLocationsByIds(candidates)).thenReturn(List.of(
                location(1L, "37.5", "127.0"),
                location(2L, "37.55", "127.05")
        ));
        when(routeService.calculateCommute(eq(new BigDecimal("37.5")), any(), any(), any(), any()))
                .thenReturn(CommuteResult.builder().commuteTime(20).transferCount(1).build());
        when(routeService.calculateCommute(eq(new BigDecimal("37.55")), any(), any(), any(), any()))
                .thenThrow(new RouteNotFoundException("no route"));

        List<RecommendationCandidate> result = filter.filter(candidates, "목적지", "대중교통", 60, 2);

        assertThat(result).extracting(RecommendationCandidate::getAdminDongId).containsExactly(1L);
    }

    @Test
    void 최대_통근시간을_초과하면_제외된다() {
        List<Long> candidates = List.of(1L);
        when(routeService.geocode("목적지")).thenReturn(DEST);
        when(adminDongMapper.findLocationsByIds(candidates)).thenReturn(List.of(location(1L, "37.5", "127.0")));
        when(routeService.calculateCommute(any(), any(), any(), any(), any()))
                .thenReturn(CommuteResult.builder().commuteTime(90).transferCount(0).build());

        List<RecommendationCandidate> result = filter.filter(candidates, "목적지", "대중교통", 60, 2);

        assertThat(result).isEmpty();
    }

    @Test
    void 카카오_API_호출이_실패한_후보는_제외되지_않고_통근정보_없이_포함된다() {
        List<Long> candidates = List.of(1L, 2L);
        when(routeService.geocode("목적지")).thenReturn(DEST);
        when(adminDongMapper.findLocationsByIds(candidates)).thenReturn(List.of(
                location(1L, "37.5", "127.0"),
                location(2L, "37.55", "127.05")
        ));
        when(routeService.calculateCommute(eq(new BigDecimal("37.5")), any(), any(), any(), any()))
                .thenReturn(CommuteResult.builder().commuteTime(20).transferCount(1).build());
        when(routeService.calculateCommute(eq(new BigDecimal("37.55")), any(), any(), any(), any()))
                .thenThrow(new KakaoRouteApiException("카카오 대중교통 길찾기 API 호출 실패: httpStatus=400"));

        List<RecommendationCandidate> result = filter.filter(candidates, "목적지", "대중교통", 60, 2);

        assertThat(result).extracting(RecommendationCandidate::getAdminDongId).containsExactlyInAnyOrder(1L, 2L);
        RecommendationCandidate failed = result.stream()
                .filter(candidate -> candidate.getAdminDongId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertThat(failed.getCommuteTime()).isNull();
        assertThat(failed.getTransferCount()).isNull();
    }

    @Test
    void 카카오_API_호출이_실패한_후보는_최대_통근시간_제한이_있어도_제외되지_않는다() {
        List<Long> candidates = List.of(1L);
        when(routeService.geocode("목적지")).thenReturn(DEST);
        when(adminDongMapper.findLocationsByIds(candidates)).thenReturn(List.of(location(1L, "37.5", "127.0")));
        when(routeService.calculateCommute(any(), any(), any(), any(), any()))
                .thenThrow(new KakaoRouteApiException("카카오 대중교통 길찾기 API 호출 실패", new RuntimeException("network")));

        List<RecommendationCandidate> result = filter.filter(candidates, "목적지", "대중교통", 60, 2);

        assertThat(result).extracting(RecommendationCandidate::getAdminDongId).containsExactly(1L);
    }

    @Test
    void 목적지_geocoding_자체가_카카오_API_실패로_안되면_전체_후보를_통근정보_없이_통과시킨다() {
        List<Long> candidates = List.of(1L, 2L);
        when(routeService.geocode("목적지"))
                .thenThrow(new KakaoRouteApiException("카카오 주소 검색 API 호출 실패: httpStatus=500"));

        List<RecommendationCandidate> result = filter.filter(candidates, "목적지", "대중교통", 60, 2);

        assertThat(result).extracting(RecommendationCandidate::getAdminDongId).containsExactly(1L, 2L);
        assertThat(result).allSatisfy(candidate -> {
            assertThat(candidate.getCommuteTime()).isNull();
            assertThat(candidate.getTransferCount()).isNull();
        });
        verify(adminDongMapper, never()).findLocationsByIds(any());
    }

    @Test
    void 후보가_50개를_초과하면_직선거리가_가까운_상위_50개만_카카오_API를_호출한다() {
        List<AdminDongLocation> locations = generateLocationsOrderedByDistanceFromDest(51);
        List<Long> candidateIds = locations.stream().map(AdminDongLocation::getAdminDongId).toList();
        when(routeService.geocode("목적지")).thenReturn(DEST);
        when(adminDongMapper.findLocationsByIds(candidateIds)).thenReturn(locations);
        when(routeService.calculateCommute(any(), any(), any(), any(), any()))
                .thenReturn(CommuteResult.builder().commuteTime(20).transferCount(0).build());

        List<RecommendationCandidate> result = filter.filter(candidateIds, "목적지", "대중교통", null, null);

        verify(routeService, times(50)).calculateCommute(any(), any(), any(), any(), any());
        assertThat(result).hasSize(50);

        AdminDongLocation nearest = locations.get(0);
        verify(routeService, times(1)).calculateCommute(
                eq(nearest.getLatitude()), eq(nearest.getLongitude()), any(), any(), any());

        AdminDongLocation farthest = locations.get(50);
        verify(routeService, never()).calculateCommute(
                eq(farthest.getLatitude()), eq(farthest.getLongitude()), any(), any(), any());
    }

    @Test
    void 후보가_50개_이하이면_직선거리_필터링_없이_전부_카카오_API를_호출한다() {
        List<AdminDongLocation> locations = generateLocationsOrderedByDistanceFromDest(50);
        List<Long> candidateIds = locations.stream().map(AdminDongLocation::getAdminDongId).toList();
        when(routeService.geocode("목적지")).thenReturn(DEST);
        when(adminDongMapper.findLocationsByIds(candidateIds)).thenReturn(locations);
        when(routeService.calculateCommute(any(), any(), any(), any(), any()))
                .thenReturn(CommuteResult.builder().commuteTime(20).transferCount(0).build());

        List<RecommendationCandidate> result = filter.filter(candidateIds, "목적지", "대중교통", null, null);

        verify(routeService, times(50)).calculateCommute(any(), any(), any(), any(), any());
        assertThat(result).hasSize(50);
    }

    private AdminDongLocation location(Long adminDongId, String lat, String lng) {
        AdminDongLocation location = new AdminDongLocation();
        location.setAdminDongId(adminDongId);
        location.setLatitude(new BigDecimal(lat));
        location.setLongitude(new BigDecimal(lng));

        return location;
    }

    /**
     * index가 클수록 DEST로부터 위도 방향으로 0.001도(약 111m)씩 멀어지는 후보를 만든다.
     * index 0은 DEST와 좌표가 같아(직선거리 0) 가장 가깝고, index가 커질수록 단조롭게
     * 멀어지므로 상위 N개 트리밍 대상이 정확히 어느 index까지인지 결정적으로 검증할 수 있다.
     */
    private List<AdminDongLocation> generateLocationsOrderedByDistanceFromDest(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> location(
                        (long) (i + 1),
                        DEST.getLatitude().add(BigDecimal.valueOf(i).multiply(new BigDecimal("0.001"))).toPlainString(),
                        DEST.getLongitude().toPlainString()))
                .toList();
    }
}

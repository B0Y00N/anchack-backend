package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.AdminDongLocation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.RecommendationAdminDongMapper;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.Coordinates;
import com.kbait.anchack.route.exception.RouteNotFoundException;
import com.kbait.anchack.route.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

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

    private AdminDongLocation location(Long adminDongId, String lat, String lng) {
        AdminDongLocation location = new AdminDongLocation();
        location.setAdminDongId(adminDongId);
        location.setLatitude(new BigDecimal(lat));
        location.setLongitude(new BigDecimal(lng));

        return location;
    }
}

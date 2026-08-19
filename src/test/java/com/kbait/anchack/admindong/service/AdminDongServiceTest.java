package com.kbait.anchack.admindong.service;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.dto.AdminDongMetricsRow;
import com.kbait.anchack.admindong.dto.RentalAmountRow;
import com.kbait.anchack.admindong.dto.response.AdminDongDetailResponse;
import com.kbait.anchack.admindong.mapper.AdminDongDetailMapper;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.mapper.PlaceMapper;
import com.kbait.anchack.rental.mapper.RentalTransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDongServiceTest {

    @Mock
    private AdminDongMapper adminDongMapper;

    @Mock
    private AdminDongDetailMapper adminDongDetailMapper;

    @Mock
    private PlaceMapper placeMapper;

    @Mock
    private RentalTransactionMapper rentalTransactionMapper;

    private AdminDongService service;

    @BeforeEach
    void setUp() {
        service = new AdminDongService(adminDongMapper, adminDongDetailMapper, placeMapper, rentalTransactionMapper);
    }

    @Test
    void ids가_비어있으면_예외() {
        assertThatThrownBy(() -> service.getAdminDongDetails(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ids가_20개를_초과하면_예외() {
        List<Long> tooMany = java.util.stream.LongStream.rangeClosed(1, 21).boxed().toList();

        assertThatThrownBy(() -> service.getAdminDongDetails(tooMany))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 기본_필드와_지표가_정상_매핑된다() {
        List<Long> ids = List.of(1L);
        when(adminDongMapper.findByIds(ids)).thenReturn(List.of(adminDong(1L, "은평구", "증산동")));
        when(adminDongDetailMapper.findMetricsByIds(ids)).thenReturn(List.of(metricsRow(1L)));
        when(rentalTransactionMapper.findAmountsByAdminDongIdsAndRentalType(eq(ids), eq("월세")))
                .thenReturn(List.of());
        when(placeMapper.findByCategory("POLICE")).thenReturn(List.of());

        List<AdminDongDetailResponse> result = service.getAdminDongDetails(ids);

        assertThat(result).hasSize(1);
        AdminDongDetailResponse response = result.get(0);
        assertThat(response.getAdminDongId()).isEqualTo(1L);
        assertThat(response.getGuName()).isEqualTo("은평구");
        assertThat(response.getDongName()).isEqualTo("증산동");
        assertThat(response.getSafetyScore()).isEqualByComparingTo("78.00");
        assertThat(response.getCctv()).isEqualByComparingTo("2.30");
        assertThat(response.getCrimeRate()).isEqualByComparingTo("3.20");
        assertThat(response.getHospitals()).isEqualTo(3);
        assertThat(response.getDepartment()).isEqualTo(0);
        assertThat(response.getMart()).isEqualTo(0);
        assertThat(response.getGyms()).isEqualTo(5);
        assertThat(response.getConvenience()).isEqualTo(7);
        assertThat(response.getParks()).isEqualTo(2);
    }

    @Test
    void 존재하지_않는_id는_결과에서_제외된다() {
        List<Long> ids = List.of(1L, 999L);
        when(adminDongMapper.findByIds(ids)).thenReturn(List.of(adminDong(1L, "은평구", "증산동")));
        when(adminDongDetailMapper.findMetricsByIds(ids)).thenReturn(List.of());
        when(rentalTransactionMapper.findAmountsByAdminDongIdsAndRentalType(eq(ids), any())).thenReturn(List.of());
        when(placeMapper.findByCategory("POLICE")).thenReturn(List.of());

        List<AdminDongDetailResponse> result = service.getAdminDongDetails(ids);

        assertThat(result).extracting(AdminDongDetailResponse::getAdminDongId).containsExactly(1L);
    }

    @Test
    void 월세_거래가_없으면_중위값은_null이고_분포는_비어있다() {
        List<Long> ids = List.of(1L);
        when(adminDongMapper.findByIds(ids)).thenReturn(List.of(adminDong(1L, "은평구", "증산동")));
        when(adminDongDetailMapper.findMetricsByIds(ids)).thenReturn(List.of());
        when(rentalTransactionMapper.findAmountsByAdminDongIdsAndRentalType(eq(ids), eq("월세")))
                .thenReturn(List.of());
        when(placeMapper.findByCategory("POLICE")).thenReturn(List.of());

        AdminDongDetailResponse response = service.getAdminDongDetails(ids).get(0);

        assertThat(response.getDeposit()).isNull();
        assertThat(response.getMonthly()).isNull();
        assertThat(response.getRentDist()).isEmpty();
    }

    @Test
    void 짝수_개수_거래의_중위값은_가운데_두_값의_평균이다() {
        List<Long> ids = List.of(1L);
        when(adminDongMapper.findByIds(ids)).thenReturn(List.of(adminDong(1L, "은평구", "증산동")));
        when(adminDongDetailMapper.findMetricsByIds(ids)).thenReturn(List.of());
        when(rentalTransactionMapper.findAmountsByAdminDongIdsAndRentalType(eq(ids), eq("월세"))).thenReturn(List.of(
                rentalAmount(1L, 900L, 60),
                rentalAmount(1L, 1000L, 64),
                rentalAmount(1L, 1100L, 70),
                rentalAmount(1L, 1200L, 90)
        ));
        when(placeMapper.findByCategory("POLICE")).thenReturn(List.of());

        AdminDongDetailResponse response = service.getAdminDongDetails(ids).get(0);

        assertThat(response.getDeposit()).isEqualTo(1050L);
        assertThat(response.getMonthly()).isEqualTo(67L);
    }

    @Test
    void 월세_구간별_분포를_경계값_포함해서_센다() {
        List<Long> ids = List.of(1L);
        when(adminDongMapper.findByIds(ids)).thenReturn(List.of(adminDong(1L, "은평구", "증산동")));
        when(adminDongDetailMapper.findMetricsByIds(ids)).thenReturn(List.of());
        when(rentalTransactionMapper.findAmountsByAdminDongIdsAndRentalType(eq(ids), eq("월세"))).thenReturn(List.of(
                rentalAmount(1L, 1000L, 49),
                rentalAmount(1L, 1000L, 50),
                rentalAmount(1L, 1000L, 59),
                rentalAmount(1L, 1000L, 80),
                rentalAmount(1L, 1000L, 81)
        ));
        when(placeMapper.findByCategory("POLICE")).thenReturn(List.of());

        AdminDongDetailResponse response = service.getAdminDongDetails(ids).get(0);

        assertThat(response.getRentDist())
                .extracting("label", "count")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("50만원↓", 1),
                        org.assertj.core.groups.Tuple.tuple("50~60", 2),
                        org.assertj.core.groups.Tuple.tuple("60~70", 0),
                        org.assertj.core.groups.Tuple.tuple("70~80", 0),
                        org.assertj.core.groups.Tuple.tuple("80만원↑", 2)
                );
    }

    @Test
    void 가장_가까운_지구대를_도보시간과_함께_반환한다() {
        List<Long> ids = List.of(1L);
        AdminDong dong = adminDong(1L, "은평구", "증산동");
        dong.setLatitude(new BigDecimal("37.5871"));
        dong.setLongitude(new BigDecimal("126.9095"));
        when(adminDongMapper.findByIds(ids)).thenReturn(List.of(dong));
        when(adminDongDetailMapper.findMetricsByIds(ids)).thenReturn(List.of());
        when(rentalTransactionMapper.findAmountsByAdminDongIdsAndRentalType(eq(ids), any())).thenReturn(List.of());
        when(placeMapper.findByCategory("POLICE")).thenReturn(List.of(
                place("먼 지구대", "37.4000", "126.8000"),
                place("증산지구대", "37.5875", "126.9090")
        ));

        AdminDongDetailResponse response = service.getAdminDongDetails(ids).get(0);

        assertThat(response.getPolice()).startsWith("증산지구대");
        assertThat(response.getPolice()).contains("도보");
    }

    private AdminDong adminDong(Long id, String guName, String dongName) {
        AdminDong adminDong = new AdminDong();
        adminDong.setAdminDongId(id);
        adminDong.setGuName(guName);
        adminDong.setName(dongName);
        adminDong.setLatitude(new BigDecimal("37.5871"));
        adminDong.setLongitude(new BigDecimal("126.9095"));

        return adminDong;
    }

    private AdminDongMetricsRow metricsRow(Long adminDongId) {
        AdminDongMetricsRow row = new AdminDongMetricsRow();
        row.setAdminDongId(adminDongId);
        row.setSafetyScore(new BigDecimal("78.00"));
        row.setCctvPer1000(new BigDecimal("2.30"));
        row.setCrimeRate(new BigDecimal("3.20"));
        row.setHospitalCount(3);
        row.setDepartmentStoreCount(0);
        row.setMartCount(0);
        row.setGymCount(5);
        row.setParkCount(2);
        row.setConvenienceStoreCount(7);

        return row;
    }

    private RentalAmountRow rentalAmount(Long adminDongId, Long deposit, int monthly) {
        RentalAmountRow row = new RentalAmountRow();
        row.setAdminDongId(adminDongId);
        row.setDepositAmount(deposit);
        row.setMonthlyRentAmount(monthly);

        return row;
    }

    private Place place(String name, String lat, String lng) {
        return Place.builder()
                .name(name)
                .latitude(new BigDecimal(lat))
                .longitude(new BigDecimal(lng))
                .build();
    }
}

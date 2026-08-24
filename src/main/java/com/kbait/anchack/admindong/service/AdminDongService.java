package com.kbait.anchack.admindong.service;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.dto.AdminDongMetricsRow;
import com.kbait.anchack.admindong.dto.RentalAmountRow;
import com.kbait.anchack.admindong.dto.response.AdminDongDetailResponse;
import com.kbait.anchack.admindong.dto.response.AdminDongResponse;
import com.kbait.anchack.admindong.dto.response.DongReviewStatsResponse;
import com.kbait.anchack.admindong.dto.response.PlaceResponse;
import com.kbait.anchack.admindong.dto.response.RentDistBucket;
import com.kbait.anchack.admindong.mapper.AdminDongDetailMapper;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.mapper.PlaceMapper;
import com.kbait.anchack.rental.mapper.RentalTransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminDongService {

    private static final int MAX_BATCH_SIZE = 20;
    private static final String MONTHLY_RENT_TYPE = "월세";
    private static final String POLICE_CATEGORY = "POLICE";
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    /** 실제 도보 경로가 아니라 직선거리 기반 근사치다(평균 도보 속도 약 4km/h 가정). */
    private static final double WALKING_SPEED_METERS_PER_MINUTE = 67;

    private final AdminDongMapper adminDongMapper;
    private final AdminDongDetailMapper adminDongDetailMapper;
    private final PlaceMapper placeMapper;
    private final RentalTransactionMapper rentalTransactionMapper;

    public AdminDongService(
        AdminDongMapper adminDongMapper,
        AdminDongDetailMapper adminDongDetailMapper,
        PlaceMapper placeMapper,
        RentalTransactionMapper rentalTransactionMapper
    ) {
        this.adminDongMapper = adminDongMapper;
        this.adminDongDetailMapper = adminDongDetailMapper;
        this.placeMapper = placeMapper;
        this.rentalTransactionMapper = rentalTransactionMapper;
    }

    /**
     * 프론트엔드에서 다루는 "구 이름 + 행정동 이름" 문자열을
     * 실제 DB의 admin_dong_id로 변환한다.
     * 리뷰 작성/조회는 admin_dong_id를 기준으로 동작하기 때문에
     * 화면에서 리뷰 기능을 쓰기 전에 이 조회가 선행되어야 한다.
     */
    @Transactional(readOnly = true)
    public AdminDongResponse getAdminDong(
        String guName,
        String dongName
    ) {
        if (
            guName == null ||
                guName.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "구 이름이 필요합니다."
            );
        }

        if (
            dongName == null ||
                dongName.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "행정동 이름이 필요합니다."
            );
        }

        AdminDong adminDong =
            adminDongMapper
                .findByGuNameAndDongName(
                    guName.trim(),
                    dongName.trim()
                );

        if (adminDong == null) {
            throw new IllegalArgumentException(
                "해당 행정동 정보를 찾을 수 없습니다: "
                    + guName + " " + dongName
            );
        }

        return AdminDongResponse.from(adminDong);
    }

    /**
     * 상세보기/비교 화면(P1-b)용 행정동 고정 정보를 id 목록으로 배치 조회한다.
     * 존재하지 않는 id는 조용히 결과에서 빠진다(추천 응답에서 막 받아온 id들이라
     * 정상적으로는 전부 존재해야 하고, 없더라도 요청 전체를 실패시킬 이유는 없다).
     * 응답 순서는 요청한 ids 순서를 그대로 따른다.
     *
     * 보증금/월세 중위값·구간별 분포는 이 엔드포인트가 rentalType/houseType을 받지 않아
     * "조건에 따라 달라지는 값"이 아니라 "동네 자체의 고정 정보"라는 문서 취지에 맞춰
     * 월세 거래 전체(주거유형 구분 없음) 기준으로 계산한다.
     */
    @Transactional(readOnly = true)
    public List<AdminDongDetailResponse> getAdminDongDetails(List<Long> adminDongIds) {
        if (adminDongIds == null || adminDongIds.isEmpty()) {
            throw new IllegalArgumentException("ids가 필요합니다.");
        }

        if (adminDongIds.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("ids는 한 번에 최대 " + MAX_BATCH_SIZE + "개까지 조회할 수 있습니다.");
        }

        Map<Long, AdminDong> adminDongsById = adminDongMapper.findByIds(adminDongIds).stream()
            .collect(Collectors.toMap(AdminDong::getAdminDongId, Function.identity()));

        Map<Long, AdminDongMetricsRow> metricsById = adminDongDetailMapper.findMetricsByIds(adminDongIds).stream()
            .collect(Collectors.toMap(AdminDongMetricsRow::getAdminDongId, Function.identity()));

        Map<Long, List<RentalAmountRow>> rentalRowsById =
            rentalTransactionMapper.findAmountsByAdminDongIdsAndRentalType(adminDongIds, MONTHLY_RENT_TYPE).stream()
                .collect(Collectors.groupingBy(RentalAmountRow::getAdminDongId));

        List<Place> policeStations = placeMapper.findByCategory(POLICE_CATEGORY);

        return adminDongIds.stream()
            .filter(adminDongsById::containsKey)
            .map(adminDongId -> toDetailResponse(
                adminDongsById.get(adminDongId),
                metricsById.get(adminDongId),
                rentalRowsById.getOrDefault(adminDongId, List.of()),
                policeStations))
            .toList();
    }

    /**
     * 구에 속한 모든 행정동의 리뷰 개수/평균 별점을 한 번에 조회한다.
     * (동네 둘러보기 - 구 선택 시 동 목록에 리뷰 요약을 보여주는 용도)
     */
    @Transactional(readOnly = true)
    public List<DongReviewStatsResponse> getReviewStatsByGuName(String guName) {
        if (guName == null || guName.trim().isEmpty()) {
            throw new IllegalArgumentException("구 이름이 필요합니다.");
        }

        return adminDongMapper.findReviewStatsByGuName(guName.trim());
    }

    /**
     * 행정동 상세 탭 지도 마커용 장소 좌표 조회(P2). categories가 없으면 전체 카테고리를
     * 반환한다. CCTV처럼 한 동에 몇 백 개씩 있을 수 있어도 자르지 않는다 - 지도에 몇 개까지
     * 찍을지/클러스터링할지는 프론트가 결정한다.
     */
    @Transactional(readOnly = true)
    public List<PlaceResponse> getPlaces(Long adminDongId, List<String> categories) {
        if (adminDongId == null) {
            throw new IllegalArgumentException("adminDongId가 필요합니다.");
        }

        List<String> validatedCategories = categories == null
            ? null
            : categories.stream().map(this::toValidCategoryName).toList();

        return placeMapper.findByAdminDongIdAndCategories(adminDongId, validatedCategories).stream()
            .map(this::toPlaceResponse)
            .toList();
    }

    private String toValidCategoryName(String category) {
        try {
            return PlaceCategory.valueOf(category).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 값입니다: " + category);
        }
    }

    private PlaceResponse toPlaceResponse(Place place) {
        return PlaceResponse.builder()
            .placeId(place.getId())
            .category(place.getCategory())
            .name(place.getName())
            .lat(place.getLatitude())
            .lng(place.getLongitude())
            .build();
    }

    private AdminDongDetailResponse toDetailResponse(
        AdminDong adminDong,
        AdminDongMetricsRow metrics,
        List<RentalAmountRow> rentalRows,
        List<Place> policeStations
    ) {
        List<Long> deposits = rentalRows.stream().map(RentalAmountRow::getDepositAmount).sorted().toList();
        List<Long> monthlyRents = rentalRows.stream()
            .map(row -> (long) row.getMonthlyRentAmount())
            .sorted()
            .toList();

        return AdminDongDetailResponse.builder()
                .adminDongId(adminDong.getAdminDongId())
                .guName(adminDong.getGuName())
                .dongName(adminDong.getName())
                .lat(adminDong.getLatitude())
                .lng(adminDong.getLongitude())
                .deposit(median(deposits))
                .monthly(median(monthlyRents))
                .rentDist(toRentDist(monthlyRents))
                .cctv(metrics == null ? null : metrics.getCctvPer1000())
                .cctvPer100m(metrics == null ? null : metrics.getCctvPer100m())
                .police(nearestPoliceDescription(adminDong, policeStations))
                .crimeRate(metrics == null ? null : metrics.getCrimeRate())
                .safetyScore(metrics == null ? null : metrics.getSafetyScore())
                .gyms(metrics == null ? null : metrics.getGymCount())
                .convenience(metrics == null ? null : metrics.getConvenienceStoreCount())
                .hospitals(metrics == null ? null : metrics.getHospitalCount())
                .parks(metrics == null ? null : metrics.getParkCount())
                .department(metrics == null ? null : metrics.getDepartmentStoreCount())
                .mart(metrics == null ? null : metrics.getMartCount())
                .build();
    }

    /** sortedValues는 이미 오름차순 정렬되어 있어야 한다. */
    private Long median(List<Long> sortedValues) {
        if (sortedValues.isEmpty()) {
            return null;
        }

        int size = sortedValues.size();

        if (size % 2 == 1) {
            return sortedValues.get(size / 2);
        }

        long lower = sortedValues.get(size / 2 - 1);
        long upper = sortedValues.get(size / 2);

        return Math.round((lower + upper) / 2.0);
    }

    private List<RentDistBucket> toRentDist(List<Long> sortedMonthlyRents) {
        if (sortedMonthlyRents.isEmpty()) {
            return List.of();
        }

        List<RentDistBucket> buckets = new ArrayList<>();
        buckets.add(RentDistBucket.builder().label("50만원↓").count(countInRange(sortedMonthlyRents, Long.MIN_VALUE, 50)).build());
        buckets.add(RentDistBucket.builder().label("50~60").count(countInRange(sortedMonthlyRents, 50, 60)).build());
        buckets.add(RentDistBucket.builder().label("60~70").count(countInRange(sortedMonthlyRents, 60, 70)).build());
        buckets.add(RentDistBucket.builder().label("70~80").count(countInRange(sortedMonthlyRents, 70, 80)).build());
        buckets.add(RentDistBucket.builder().label("80만원↑").count(countInRange(sortedMonthlyRents, 80, Long.MAX_VALUE)).build());

        return buckets;
    }

    /** [fromInclusive, toExclusive) 구간의 건수를 센다. */
    private int countInRange(List<Long> sortedValues, long fromInclusive, long toExclusive) {
        return (int) sortedValues.stream().filter(value -> value >= fromInclusive && value < toExclusive).count();
    }

    private String nearestPoliceDescription(AdminDong adminDong, List<Place> policeStations) {
        return policeStations.stream()
            .min(Comparator.comparingDouble(place -> haversineDistanceMeters(
                adminDong.getLatitude(), adminDong.getLongitude(),
                place.getLatitude(), place.getLongitude())))
            .map(nearest -> {
                double distance = haversineDistanceMeters(
                    adminDong.getLatitude(), adminDong.getLongitude(),
                    nearest.getLatitude(), nearest.getLongitude());
                long walkMinutes = Math.max(1, Math.round(distance / WALKING_SPEED_METERS_PER_MINUTE));

                return nearest.getName() + " (도보 " + walkMinutes + "분)";
            })
            .orElse(null);
    }

    private double haversineDistanceMeters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double deltaLat = radLat2 - radLat1;
        double deltaLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}

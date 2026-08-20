package com.kbait.anchack.condition.service.impl;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.common.exception.ForbiddenException;
import com.kbait.anchack.common.exception.NotFoundException;
import com.kbait.anchack.condition.dto.ConditionEssentialRow;
import com.kbait.anchack.condition.dto.ConditionGuRow;
import com.kbait.anchack.condition.dto.ConditionWeightRow;
import com.kbait.anchack.condition.dto.PreferredHouseTypeRow;
import com.kbait.anchack.condition.dto.RecommendedDongResponse;
import com.kbait.anchack.condition.dto.SavedConditionResponse;
import com.kbait.anchack.condition.dto.UserConditionCreateRequest;
import com.kbait.anchack.condition.dto.UserConditionCreateResponse;
import com.kbait.anchack.condition.dto.UserConditionRow;
import com.kbait.anchack.condition.mapper.ConditionEssentialMapper;
import com.kbait.anchack.condition.mapper.ConditionGuMapper;
import com.kbait.anchack.condition.mapper.ConditionWeightMapper;
import com.kbait.anchack.condition.mapper.PreferredHouseTypeMapper;
import com.kbait.anchack.condition.mapper.UserConditionMapper;
import com.kbait.anchack.condition.service.ConditionService;
import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.RecommendationRow;
import com.kbait.anchack.recommendation.mapper.RecommendationMapper;
import com.kbait.anchack.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 프론트 영문 코드(예: "MONTHLY", "CONVENIENCE_STORE")를 DB의 한글 ENUM 값으로
 * 변환하면서 user_conditions + 하위 테이블을 저장하고, 바로 이어서
 * RecommendationService.generate()를 호출해 추천 결과까지 반환한다.
 */
@Service
@RequiredArgsConstructor
public class ConditionServiceImpl implements ConditionService {

    private static final Map<String, String> RENTAL_TYPE_CODES = Map.of(
            "MONTHLY", "월세",
            "JEONSE", "전세"
    );

    private static final Map<String, String> COMMUTE_TYPE_CODES = Map.of(
            "PUBLIC_TRANSIT", "대중교통",
            "CAR", "자가용"
    );

    /** RENTAL_TYPE_CODES/COMMUTE_TYPE_CODES의 역방향. 저장된 조건을 응답으로 돌려줄 때
     * DB의 한글 ENUM 값을 프론트가 원래 보냈던 영문 코드로 되돌리는 데 쓴다. */
    private static final Map<String, String> RENTAL_TYPE_LABELS = Map.of(
            "월세", "MONTHLY",
            "전세", "JEONSE"
    );

    private static final Map<String, String> COMMUTE_TYPE_LABELS = Map.of(
            "대중교통", "PUBLIC_TRANSIT",
            "자가용", "CAR"
    );

    private static final Map<String, String> ESSENTIAL_CATEGORY_CODES = Map.of(
            "CONVENIENCE_STORE", "편의점",
            "GYM", "헬스장",
            "HOSPITAL", "병원",
            "PARK", "공원",
            "MART", "대형마트"
    );

    private static final Map<String, String> HOUSE_TYPE_CODES = Map.of(
            "OFFICETEL", "오피스텔",
            "VILLA", "빌라",
            "DETACHED", "단독",
            "MULTI_HOUSEHOLD", "다가구",
            "APARTMENT", "아파트",
            "ONE_ROOM", "원룸"
    );

    private static final Set<String> VALID_PRIORITY_CATEGORIES = Set.of(
            "TRANSIT", "SAFETY", "SPORTS", "FOOD", "CONVENIENCE", "HEALTHCARE", "CULTURE", "NATURE", "SILENCE"
    );

    private final UserConditionMapper userConditionMapper;
    private final ConditionWeightMapper conditionWeightMapper;
    private final ConditionEssentialMapper conditionEssentialMapper;
    private final PreferredHouseTypeMapper preferredHouseTypeMapper;
    private final ConditionGuMapper conditionGuMapper;
    private final RecommendationService recommendationService;
    private final RecommendationMapper recommendationMapper;
    private final AdminDongMapper adminDongMapper;

    @Override
    @Transactional
    public UserConditionCreateResponse createAndRecommend(Long userId, UserConditionCreateRequest request) {
        Map<String, BigDecimal> categoryWeights = buildCategoryWeights(request.getPriorityCategories());

        UserConditionRow userCondition = buildUserConditionRow(userId, request);
        userConditionMapper.insert(userCondition);
        Long conditionId = userCondition.getConditionId();

        insertWeights(conditionId, categoryWeights);
        insertEssentials(conditionId, request.getEssentialCategories());
        insertHouseTypes(conditionId, request.getPreferredHouseTypes());
        insertGus(conditionId, request.getGuCodes());

        ConditionBundle bundle = toConditionBundle(conditionId, request, categoryWeights);
        List<RecommendationRow> recommendations = recommendationService.generate(bundle);

        return toResponse(conditionId, recommendations);
    }

    @Override
    @Transactional
    public void saveCondition(Long userId, Long conditionId, String title) {
        UserConditionRow condition = requireOwnedCondition(userId, conditionId);

        userConditionMapper.markSaved(condition.getConditionId(), normalizeTitle(title));
    }

    /** 공백만 있는 title은 "입력 안 함"과 동일하게 취급해 기존 title을 덮어쓰지 않는다. */
    private String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }

        String trimmed = title.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional
    public void unsaveCondition(Long userId, Long conditionId) {
        UserConditionRow condition = requireOwnedCondition(userId, conditionId);

        userConditionMapper.updateIsSaved(condition.getConditionId(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedConditionResponse> getSavedConditions(Long userId) {
        return userConditionMapper.findSavedByUserId(userId).stream()
                .map(this::toSavedConditionResponse)
                .toList();
    }

    /**
     * 저장된 조건의 추천 결과를 재계산 없이 그대로 반환한다. route/transportType/lineNum/
     * vehicleType/walkMin/transitMin은 최초 생성 시점 응답에만 있고 DB에 저장되지 않아
     * 여기서는 항상 null이다 - 다시 계산하려면 카카오 API를 재호출해야 하는데 이건 이
     * 조회 API의 범위가 아니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RecommendedDongResponse> getRecommendations(Long userId, Long conditionId) {
        requireOwnedCondition(userId, conditionId);

        List<RecommendationRow> rows = recommendationMapper.findByConditionId(conditionId);

        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> adminDongIds = rows.stream().map(RecommendationRow::getAdminDongId).toList();
        Map<Long, AdminDong> adminDongsById = adminDongMapper.findByIds(adminDongIds).stream()
                .collect(Collectors.toMap(AdminDong::getAdminDongId, Function.identity()));

        return rows.stream()
                .map(row -> toSavedRecommendationResponse(row, adminDongsById.get(row.getAdminDongId())))
                .toList();
    }

    private UserConditionRow requireOwnedCondition(Long userId, Long conditionId) {
        UserConditionRow condition = userConditionMapper.findById(conditionId);

        if (condition == null) {
            throw new NotFoundException("존재하지 않는 조건입니다: " + conditionId);
        }

        if (!condition.getUserId().equals(userId)) {
            throw new ForbiddenException("본인이 등록한 조건만 접근할 수 있습니다.");
        }

        return condition;
    }

    private SavedConditionResponse toSavedConditionResponse(UserConditionRow row) {
        return SavedConditionResponse.builder()
                .conditionId(row.getConditionId())
                .title(row.getTitle())
                .rentalType(reverseTranslateOrThrow(RENTAL_TYPE_LABELS, row.getRentalType()))
                .destAddress(row.getDestAddress())
                .commuteType(row.getCommuteType() == null
                        ? null
                        : reverseTranslateOrThrow(COMMUTE_TYPE_LABELS, row.getCommuteType()))
                .maxCommuteTime(row.getMaxCommuteTime())
                .maxTransferCount(row.getMaxTransferCount())
                .minArea(row.getMinArea())
                .maxDeposit(row.getMaxDeposit())
                .maxRent(row.getMaxRent() == null ? null : Math.toIntExact(row.getMaxRent()))
                .createdAt(row.getCreatedAt())
                .build();
    }

    private RecommendedDongResponse toSavedRecommendationResponse(RecommendationRow row, AdminDong adminDong) {
        return RecommendedDongResponse.builder()
                .adminDongId(row.getAdminDongId())
                .guName(adminDong == null ? null : adminDong.getGuName())
                .dongName(adminDong == null ? null : adminDong.getName())
                .lat(adminDong == null ? null : adminDong.getLatitude())
                .lng(adminDong == null ? null : adminDong.getLongitude())
                .totalScore(row.getTotalScore())
                .dataCoverageRate(row.getDataCoverageRate())
                .rank(row.getRank())
                .commuteTime(row.getCommuteTime())
                .transferCount(row.getTransferCount())
                .recommendationReason(row.getRecommendationReason())
                .caution(row.getCaution())
                .build();
    }

    private String reverseTranslateOrThrow(Map<String, String> labels, String koreanValue) {
        String code = labels.get(koreanValue);

        if (code == null) {
            throw new IllegalStateException("알 수 없는 값입니다: " + koreanValue);
        }

        return code;
    }

    private UserConditionRow buildUserConditionRow(Long userId, UserConditionCreateRequest request) {
        return UserConditionRow.builder()
                .userId(userId)
                .title("")
                .rentalType(translateOrThrow(RENTAL_TYPE_CODES, request.getRentalType()))
                .destAddress(request.getDestAddress())
                .commuteType(translateOrThrow(COMMUTE_TYPE_CODES, request.getCommuteType()))
                .maxCommuteTime(request.getMaxCommuteTime())
                .maxTransferCount(request.getMaxTransferCount())
                .minArea(request.getMinArea())
                .maxDeposit(request.getMaxDeposit())
                .maxRent(request.getMaxRent() == null ? null : request.getMaxRent().longValue())
                .build();
    }

    private Map<String, BigDecimal> buildCategoryWeights(List<String> priorityCategories) {
        priorityCategories.forEach(this::validatePriorityCategory);

        int size = priorityCategories.size();
        Map<String, BigDecimal> weights = new LinkedHashMap<>();

        for (int i = 0; i < size; i++) {
            weights.put(priorityCategories.get(i), BigDecimal.valueOf(size - i));
        }

        return weights;
    }

    private void validatePriorityCategory(String category) {
        if (!VALID_PRIORITY_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("지원하지 않는 값입니다: " + category);
        }
    }

    private void insertWeights(Long conditionId, Map<String, BigDecimal> categoryWeights) {
        List<ConditionWeightRow> rows = categoryWeights.entrySet().stream()
                .map(entry -> ConditionWeightRow.builder()
                        .conditionId(conditionId)
                        .category(entry.getKey())
                        .importance(entry.getValue())
                        .build())
                .toList();

        conditionWeightMapper.insertBatch(rows);
    }

    private void insertEssentials(Long conditionId, List<String> essentialCategories) {
        if (essentialCategories == null || essentialCategories.isEmpty()) {
            return;
        }

        List<ConditionEssentialRow> rows = essentialCategories.stream()
                .map(code -> ConditionEssentialRow.builder()
                        .conditionId(conditionId)
                        .category(translateOrThrow(ESSENTIAL_CATEGORY_CODES, code))
                        .build())
                .toList();

        conditionEssentialMapper.insertBatch(rows);
    }

    private void insertHouseTypes(Long conditionId, List<String> preferredHouseTypes) {
        if (preferredHouseTypes == null || preferredHouseTypes.isEmpty()) {
            return;
        }

        List<PreferredHouseTypeRow> rows = preferredHouseTypes.stream()
                .map(code -> PreferredHouseTypeRow.builder()
                        .conditionId(conditionId)
                        .houseType(translateOrThrow(HOUSE_TYPE_CODES, code))
                        .build())
                .toList();

        preferredHouseTypeMapper.insertBatch(rows);
    }

    private void insertGus(Long conditionId, List<String> guCodes) {
        if (guCodes == null || guCodes.isEmpty()) {
            return;
        }

        List<ConditionGuRow> rows = guCodes.stream()
                .map(guCode -> ConditionGuRow.builder().conditionId(conditionId).guCode(guCode).build())
                .toList();

        conditionGuMapper.insertBatch(rows);
    }

    private ConditionBundle toConditionBundle(
            Long conditionId,
            UserConditionCreateRequest request,
            Map<String, BigDecimal> categoryWeights
    ) {
        return ConditionBundle.builder()
                .conditionId(conditionId)
                .rentalType(translateOrThrow(RENTAL_TYPE_CODES, request.getRentalType()))
                .guCodes(request.getGuCodes())
                .essentialCategories(translateEssentials(request.getEssentialCategories()))
                .preferredHouseTypes(translateHouseTypes(request.getPreferredHouseTypes()))
                .maxDeposit(request.getMaxDeposit())
                .maxRent(request.getMaxRent())
                .minArea(request.getMinArea())
                .destAddress(request.getDestAddress())
                .commuteType(translateOrThrow(COMMUTE_TYPE_CODES, request.getCommuteType()))
                .maxCommuteTime(request.getMaxCommuteTime())
                .maxTransferCount(request.getMaxTransferCount())
                .categoryWeights(categoryWeights)
                .build();
    }

    private List<String> translateEssentials(List<String> essentialCategories) {
        if (essentialCategories == null) {
            return List.of();
        }

        return essentialCategories.stream().map(code -> translateOrThrow(ESSENTIAL_CATEGORY_CODES, code)).toList();
    }

    private List<String> translateHouseTypes(List<String> preferredHouseTypes) {
        if (preferredHouseTypes == null) {
            return List.of();
        }

        return preferredHouseTypes.stream().map(code -> translateOrThrow(HOUSE_TYPE_CODES, code)).toList();
    }

    private UserConditionCreateResponse toResponse(Long conditionId, List<RecommendationRow> recommendations) {
        List<RecommendedDongResponse> responses = recommendations.stream()
                .map(this::toRecommendedDongResponse)
                .toList();

        return UserConditionCreateResponse.builder()
                .conditionId(conditionId)
                .recommendations(responses)
                .build();
    }

    private RecommendedDongResponse toRecommendedDongResponse(RecommendationRow row) {
        return RecommendedDongResponse.builder()
                .adminDongId(row.getAdminDongId())
                .guName(row.getGuName())
                .dongName(row.getDongName())
                .lat(row.getLatitude())
                .lng(row.getLongitude())
                .totalScore(row.getTotalScore())
                .dataCoverageRate(row.getDataCoverageRate())
                .rank(row.getRank())
                .commuteTime(row.getCommuteTime())
                .transferCount(row.getTransferCount())
                .route(row.getRoute())
                .transportType(row.getTransportType())
                .lineNum(row.getLineNum())
                .vehicleType(row.getVehicleType())
                .walkMin(row.getWalkMin())
                .transitMin(row.getTransitMin())
                .recommendationReason(row.getRecommendationReason())
                .caution(row.getCaution())
                .build();
    }

    private String translateOrThrow(Map<String, String> codes, String code) {
        String translated = codes.get(code);

        if (translated == null) {
            throw new IllegalArgumentException("지원하지 않는 값입니다: " + code);
        }

        return translated;
    }
}

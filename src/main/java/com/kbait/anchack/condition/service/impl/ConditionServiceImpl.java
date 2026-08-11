package com.kbait.anchack.condition.service.impl;

import com.kbait.anchack.condition.dto.ConditionEssentialRow;
import com.kbait.anchack.condition.dto.ConditionGuRow;
import com.kbait.anchack.condition.dto.ConditionWeightRow;
import com.kbait.anchack.condition.dto.PreferredHouseTypeRow;
import com.kbait.anchack.condition.dto.RecommendedDongResponse;
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
import com.kbait.anchack.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** 프론트는 "만원" 단위로 보내지만, 컨벤션대로 내부/DB는 "원" 단위 정수로 다룬다. */
    private static final long MANWON_TO_WON = 10_000L;

    private static final Set<String> VALID_PRIORITY_CATEGORIES = Set.of(
            "TRANSIT", "SAFETY", "SPORTS", "FOOD", "CONVENIENCE", "HEALTHCARE", "CULTURE", "NATURE", "SILENCE"
    );

    private final UserConditionMapper userConditionMapper;
    private final ConditionWeightMapper conditionWeightMapper;
    private final ConditionEssentialMapper conditionEssentialMapper;
    private final PreferredHouseTypeMapper preferredHouseTypeMapper;
    private final ConditionGuMapper conditionGuMapper;
    private final RecommendationService recommendationService;

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
                .maxDeposit(toWon(request.getMaxDeposit()))
                .maxRent(toWon(request.getMaxRent() == null ? null : request.getMaxRent().longValue()))
                .build();
    }

    private Long toWon(Long manwon) {
        return manwon == null ? null : manwon * MANWON_TO_WON;
    }

    private Integer toWonInt(Integer manwon) {
        return manwon == null ? null : Math.toIntExact(manwon * MANWON_TO_WON);
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
                        .conditionId2(conditionId)
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
                        .conditionId2(conditionId)
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
                .maxDeposit(toWon(request.getMaxDeposit()))
                .maxRent(toWonInt(request.getMaxRent()))
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
                .totalScore(row.getTotalScore())
                .dataCoverageRate(row.getDataCoverageRate())
                .rank(row.getRank())
                .commuteTime(row.getCommuteTime())
                .transferCount(row.getTransferCount())
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

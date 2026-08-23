package com.kbait.anchack.condition.service.impl;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.common.exception.ForbiddenException;
import com.kbait.anchack.common.exception.NotFoundException;
import com.kbait.anchack.common.util.DeadlockRetry;
import com.kbait.anchack.condition.dto.ConditionWeightRow;
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
import com.kbait.anchack.recommendation.dto.FilterFunnelStage;
import com.kbait.anchack.recommendation.dto.RecommendationComputation;
import com.kbait.anchack.recommendation.dto.RecommendationRow;
import com.kbait.anchack.recommendation.mapper.RecommendationMapper;
import com.kbait.anchack.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * RecommendationService.compute()/persist()를 호출해 추천 결과까지 반환한다.
 *
 * createAndRecommend()/recompute() 둘 다 의도적으로 @Transactional을 안 붙인다 - 저장
 * (UserConditionWriter.insert, 트랜잭션 있음)과 계산(RecommendationService.compute, 카카오/
 * OpenAI 외부 호출 포함, 트랜잭션 없음)과 반영(RecommendationService.persist, 트랜잭션 있음
 * + 데드락 재시도)이 서로 다른 트랜잭션 경계를 가져야 하기 때문이다. 이 메서드 자체에
 * @Transactional을 붙이면 그 안의 모든 호출이 하나의 트랜잭션/커넥션으로 묶여버려서,
 * persist()가 데드락으로 재시도될 때 이미 끝난 외부 API 호출까지 다시 실행되는 문제로
 * 되돌아간다.
 *
 * createAndRecommend()는 저장이 이미 별도 트랜잭션으로 커밋된 뒤 계산/반영이 실패할 수
 * 있어서, 그 경우 방금 만든 조건을 UserConditionWriter.delete()로 보상 삭제한다 - 그래야
 * "추천 결과 없는 조건"이 남지 않고, 예전(모든 단계가 한 트랜잭션이던 시절)과 동일하게
 * 전부 성공 아니면 전부 없음이 유지된다. recompute()는 이 처리가 필요 없다 - 기존
 * recommendations를 DELETE+INSERT로 교체하는 persist()가 실패하면 그 자체가
 * @Transactional이라 기존 값이 그대로 롤백되어 남기 때문에 애초에 고아 상태가 생기지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ConditionServiceImpl implements ConditionService {

    private static final Logger log = LoggerFactory.getLogger(ConditionServiceImpl.class);

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
    private final UserConditionWriter userConditionWriter;
    private final RecommendationService recommendationService;
    private final RecommendationMapper recommendationMapper;
    private final AdminDongMapper adminDongMapper;

    @Override
    public UserConditionCreateResponse createAndRecommend(Long userId, UserConditionCreateRequest request) {
        Map<String, BigDecimal> categoryWeights = buildCategoryWeights(request.getPriorityCategories());
        UserConditionRow userCondition = buildUserConditionRow(userId, request);

        Long conditionId = userConditionWriter.insert(
                userCondition,
                categoryWeights,
                translateEssentials(request.getEssentialCategories()),
                translateHouseTypes(request.getPreferredHouseTypes()),
                request.getGuCodes());

        List<RecommendationRow> recommendations;
        List<FilterFunnelStage> filterFunnel;

        try {
            ConditionBundle bundle = toConditionBundle(conditionId, request, categoryWeights);
            RecommendationComputation computed = recommendationService.compute(bundle);
            recommendations =
                    DeadlockRetry.execute(() -> recommendationService.persist(conditionId, computed.getRows()));
            filterFunnel = computed.getFilterFunnel();
        } catch (RuntimeException e) {
            cleanUpFailedCondition(conditionId, e);
            throw e;
        }

        // toResponse()는 DB 호출이 없는 순수 변환이라 일부러 try 밖에 둔다 - persist()가 이미
        // 성공해 recommendations/recommendation_scores가 커밋된 뒤라, 여기서 예외가 나도 정리
        // 대상이 아니다. try 안에 있으면 UserConditionWriter.delete()가 recommendations는
        // 못 지우면서 user_conditions만 지우려다 그 FK 때문에 실패하고, 그 실패를 삼키는
        // 사이 하위테이블(가중치 등)만 없는 반쯤 망가진 조건이 남는 문제가 있었다.
        return toResponse(conditionId, recommendations, filterFunnel);
    }

    /**
     * 계산/반영 실패로 추천 결과 없이 남게 된 조건을 지운다. 삭제 자체가 실패해도 원래
     * 예외(e)를 가리지 않도록 별도로 잡아 로그만 남기고, 항상 원래 예외를 그대로 던진다.
     */
    private void cleanUpFailedCondition(Long conditionId, RuntimeException cause) {
        try {
            userConditionWriter.delete(conditionId);
        } catch (RuntimeException cleanupException) {
            log.error(
                    "조건 생성 실패 후 정리(삭제)까지 실패했습니다: conditionId={} - 추천 결과 없는 조건이 남았을 수 있습니다.",
                    conditionId, cleanupException);

            return;
        }

        log.warn("조건 생성 중 추천 계산/반영이 실패해 conditionId={}를 정리했습니다.", conditionId, cause);
    }

    /**
     * 이미 저장된 조건의 파라미터(하위 테이블 포함)를 그대로 다시 읽어 추천을 재계산한다.
     * user_conditions/condition_weights/condition_essentials/preferred_house_types/
     * condition_gus에 이미 DB 네이티브 형식(한글 ENUM, 만원 단위 등)으로 저장돼 있어
     * createAndRecommend와 달리 프론트 영문 코드 변환이 필요 없다. RecommendationService.
     * persist()가 conditionId 기준으로 기존 recommendations를 DELETE 후 다시 INSERT하는
     * 방식이라 신규 생성과 동일한 경로로 재계산 결과를 그대로 덮어쓸 수 있다. 재계산이
     * 끝나면 is_latest를 TRUE로 되돌린다(FALSE로 바꾸는 로직은 아직 없다 - 추후 작업).
     * markLatest는 persist()가 성공한 뒤에만 실행된다 - persist()가 재시도를 다 써버리고
     * 예외를 던지면 그 지점에서 그대로 전파되어 markLatest는 호출되지 않는다.
     */
    @Override
    public UserConditionCreateResponse recompute(Long userId, Long conditionId) {
        UserConditionRow condition = requireOwnedCondition(userId, conditionId);
        ConditionBundle bundle = toConditionBundleFromStoredCondition(condition);

        RecommendationComputation computed = recommendationService.compute(bundle);
        List<RecommendationRow> recommendations =
                DeadlockRetry.execute(() -> recommendationService.persist(conditionId, computed.getRows()));

        userConditionMapper.markLatest(conditionId);

        return toResponse(conditionId, recommendations, computed.getFilterFunnel());
    }

    private ConditionBundle toConditionBundleFromStoredCondition(UserConditionRow condition) {
        Long conditionId = condition.getConditionId();
        Map<String, BigDecimal> categoryWeights = conditionWeightMapper.findByConditionId(conditionId).stream()
                .collect(Collectors.toMap(ConditionWeightRow::getCategory, ConditionWeightRow::getImportance));

        return ConditionBundle.builder()
                .conditionId(conditionId)
                .rentalType(condition.getRentalType())
                .guCodes(conditionGuMapper.findGuCodesByConditionId(conditionId))
                .essentialCategories(conditionEssentialMapper.findCategoriesByConditionId(conditionId))
                .preferredHouseTypes(preferredHouseTypeMapper.findHouseTypesByConditionId(conditionId))
                .maxDeposit(condition.getMaxDeposit())
                .maxRent(condition.getMaxRent() == null ? null : Math.toIntExact(condition.getMaxRent()))
                .minArea(condition.getMinArea())
                .destAddress(condition.getDestAddress())
                .commuteType(condition.getCommuteType())
                .maxCommuteTime(condition.getMaxCommuteTime())
                .maxTransferCount(condition.getMaxTransferCount())
                .categoryWeights(categoryWeights)
                .build();
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
     * vehicleType/walkMin/transitMin도 최초 생성 시점에 recommendations에 함께 저장돼(V8)
     * 있는 값을 그대로 돌려준다 - destAddress 없이 생성된 조건이면 애초에 null이었던
     * 값이라 여기서도 null이다.
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
                .latest(row.getLatest())
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

    private UserConditionCreateResponse toResponse(
            Long conditionId,
            List<RecommendationRow> recommendations,
            List<FilterFunnelStage> filterFunnel
    ) {
        List<RecommendedDongResponse> responses = recommendations.stream()
                .map(this::toRecommendedDongResponse)
                .toList();

        return UserConditionCreateResponse.builder()
                .conditionId(conditionId)
                .filterFunnel(filterFunnel)
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

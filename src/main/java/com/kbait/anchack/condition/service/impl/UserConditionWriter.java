package com.kbait.anchack.condition.service.impl;

import com.kbait.anchack.condition.dto.ConditionEssentialRow;
import com.kbait.anchack.condition.dto.ConditionGuRow;
import com.kbait.anchack.condition.dto.ConditionWeightRow;
import com.kbait.anchack.condition.dto.PreferredHouseTypeRow;
import com.kbait.anchack.condition.dto.UserConditionRow;
import com.kbait.anchack.condition.mapper.ConditionEssentialMapper;
import com.kbait.anchack.condition.mapper.ConditionGuMapper;
import com.kbait.anchack.condition.mapper.ConditionWeightMapper;
import com.kbait.anchack.condition.mapper.PreferredHouseTypeMapper;
import com.kbait.anchack.condition.mapper.UserConditionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * user_conditions + 하위 테이블(condition_weights/condition_essentials/
 * preferred_house_types/condition_gus) 저장(insert)과 삭제(delete)를 각각 하나의
 * 트랜잭션으로 묶는다.
 *
 * ConditionServiceImpl.createAndRecommend()가 원래 저장 로직을 직접 갖고 있었는데,
 * 이어서 실행하는 하드필터/스코어링/reason 생성(카카오·OpenAI 외부 호출 포함)과 트랜잭션이
 * 분리돼야 해서 별도 빈으로 뺐다 - 같은 클래스 안에서 this.insert(...)로 호출하면
 * @Transactional이 프록시를 거치지 않아 적용되지 않기 때문이다(self-invocation).
 * ConditionServiceImpl 자체는 더 이상 @Transactional이 아니므로, 이 삽입이 끝난 뒤에
 * 이어지는 외부 API 호출은 DB 커넥션을 붙잡지 않은 채 실행된다.
 */
@Component
@RequiredArgsConstructor
public class UserConditionWriter {

    private final UserConditionMapper userConditionMapper;
    private final ConditionWeightMapper conditionWeightMapper;
    private final ConditionEssentialMapper conditionEssentialMapper;
    private final PreferredHouseTypeMapper preferredHouseTypeMapper;
    private final ConditionGuMapper conditionGuMapper;

    /**
     * essentialCategoriesKorean/houseTypesKorean은 이미 한글 ENUM 값으로 번역된 상태로
     * 받는다 - 영문 코드 -> 한글 번역은 ConditionServiceImpl의 책임으로 남겨두고, 이
     * 클래스는 순수하게 저장만 담당한다.
     */
    @Transactional
    public Long insert(
            UserConditionRow userCondition,
            Map<String, BigDecimal> categoryWeights,
            List<String> essentialCategoriesKorean,
            List<String> houseTypesKorean,
            List<String> guCodes
    ) {
        userConditionMapper.insert(userCondition);
        Long conditionId = userCondition.getConditionId();

        insertWeights(conditionId, categoryWeights);
        insertEssentials(conditionId, essentialCategoriesKorean);
        insertHouseTypes(conditionId, houseTypesKorean);
        insertGus(conditionId, guCodes);

        return conditionId;
    }

    /**
     * insert()가 만든 user_conditions + 하위 테이블을 통째로 지운다. 하드필터/스코어링/
     * reason 생성이나 recommendations 반영이 끝내 실패했을 때, 추천 결과 없는 "고아" 조건이
     * 남지 않도록 ConditionServiceImpl이 보상 삭제(compensating delete)로 호출한다 - insert()가
     * 별도 트랜잭션으로 이미 커밋된 뒤라 그 실패와 하나의 트랜잭션으로 묶을 수 없기 때문이다.
     * FK 제약 때문에 하위 테이블부터 지우고 user_conditions를 마지막에 지운다.
     */
    @Transactional
    public void delete(Long conditionId) {
        conditionWeightMapper.deleteByConditionId(conditionId);
        conditionEssentialMapper.deleteByConditionId(conditionId);
        preferredHouseTypeMapper.deleteByConditionId(conditionId);
        conditionGuMapper.deleteByConditionId(conditionId);
        userConditionMapper.deleteByConditionId(conditionId);
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

    private void insertEssentials(Long conditionId, List<String> essentialCategoriesKorean) {
        if (essentialCategoriesKorean == null || essentialCategoriesKorean.isEmpty()) {
            return;
        }

        List<ConditionEssentialRow> rows = essentialCategoriesKorean.stream()
                .map(category -> ConditionEssentialRow.builder()
                        .conditionId(conditionId)
                        .category(category)
                        .build())
                .toList();

        conditionEssentialMapper.insertBatch(rows);
    }

    private void insertHouseTypes(Long conditionId, List<String> houseTypesKorean) {
        if (houseTypesKorean == null || houseTypesKorean.isEmpty()) {
            return;
        }

        List<PreferredHouseTypeRow> rows = houseTypesKorean.stream()
                .map(houseType -> PreferredHouseTypeRow.builder()
                        .conditionId(conditionId)
                        .houseType(houseType)
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
}

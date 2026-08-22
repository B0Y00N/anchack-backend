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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserConditionWriterTest {

    @Mock
    private UserConditionMapper userConditionMapper;

    @Mock
    private ConditionWeightMapper conditionWeightMapper;

    @Mock
    private ConditionEssentialMapper conditionEssentialMapper;

    @Mock
    private PreferredHouseTypeMapper preferredHouseTypeMapper;

    @Mock
    private ConditionGuMapper conditionGuMapper;

    private UserConditionWriter writer;

    @BeforeEach
    void setUp() {
        writer = new UserConditionWriter(
                userConditionMapper,
                conditionWeightMapper,
                conditionEssentialMapper,
                preferredHouseTypeMapper,
                conditionGuMapper);
    }

    @Test
    void user_condition을_저장하고_생성된_conditionId를_반환한다() {
        mockGeneratedConditionId();
        UserConditionRow userCondition = UserConditionRow.builder().userId(10L).build();

        Long conditionId = writer.insert(
                userCondition, Map.of("SAFETY", BigDecimal.valueOf(1)), List.of(), List.of(), List.of());

        assertThat(conditionId).isEqualTo(1L);
    }

    @Test
    void categoryWeights를_condition_weights_행으로_변환해_저장한다() {
        mockGeneratedConditionId();
        UserConditionRow userCondition = UserConditionRow.builder().userId(10L).build();
        ArgumentCaptor<List<ConditionWeightRow>> captor = weightRowsCaptor();

        writer.insert(
                userCondition,
                Map.of("SAFETY", BigDecimal.valueOf(2), "SPORTS", BigDecimal.valueOf(1)),
                List.of(), List.of(), List.of());

        verify(conditionWeightMapper).insertBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(ConditionWeightRow::getConditionId, ConditionWeightRow::getCategory)
                .contains(tuple(1L, "SAFETY"), tuple(1L, "SPORTS"));
    }

    @Test
    void essentialCategories가_비어있으면_condition_essentials_insert를_호출하지_않는다() {
        mockGeneratedConditionId();
        UserConditionRow userCondition = UserConditionRow.builder().userId(10L).build();

        writer.insert(userCondition, Map.of(), List.of(), List.of(), List.of());

        verifyNoInteractions(conditionEssentialMapper);
    }

    @Test
    void essentialCategories가_있으면_conditionId를_채워_저장한다() {
        mockGeneratedConditionId();
        UserConditionRow userCondition = UserConditionRow.builder().userId(10L).build();
        ArgumentCaptor<List<ConditionEssentialRow>> captor = essentialRowsCaptor();

        writer.insert(userCondition, Map.of(), List.of("편의점", "공원"), List.of(), List.of());

        verify(conditionEssentialMapper).insertBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(ConditionEssentialRow::getConditionId, ConditionEssentialRow::getCategory)
                .containsExactly(tuple(1L, "편의점"), tuple(1L, "공원"));
    }

    @Test
    void houseTypes가_비어있으면_preferred_house_types_insert를_호출하지_않는다() {
        mockGeneratedConditionId();
        UserConditionRow userCondition = UserConditionRow.builder().userId(10L).build();

        writer.insert(userCondition, Map.of(), List.of(), List.of(), List.of());

        verifyNoInteractions(preferredHouseTypeMapper);
    }

    @Test
    void houseTypes가_있으면_conditionId를_채워_저장한다() {
        mockGeneratedConditionId();
        UserConditionRow userCondition = UserConditionRow.builder().userId(10L).build();
        ArgumentCaptor<List<PreferredHouseTypeRow>> captor = houseTypeRowsCaptor();

        writer.insert(userCondition, Map.of(), List.of(), List.of("오피스텔"), List.of());

        verify(preferredHouseTypeMapper).insertBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(PreferredHouseTypeRow::getConditionId, PreferredHouseTypeRow::getHouseType)
                .containsExactly(tuple(1L, "오피스텔"));
    }

    @Test
    void guCodes가_없으면_condition_gus_insert를_호출하지_않는다() {
        mockGeneratedConditionId();
        UserConditionRow userCondition = UserConditionRow.builder().userId(10L).build();

        writer.insert(userCondition, Map.of(), List.of(), List.of(), null);

        verifyNoInteractions(conditionGuMapper);
    }

    @Test
    void guCodes가_있으면_conditionId를_채워_저장한다() {
        mockGeneratedConditionId();
        UserConditionRow userCondition = UserConditionRow.builder().userId(10L).build();
        ArgumentCaptor<List<ConditionGuRow>> captor = guRowsCaptor();

        writer.insert(userCondition, Map.of(), List.of(), List.of(), List.of("11010"));

        verify(conditionGuMapper).insertBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(ConditionGuRow::getConditionId, ConditionGuRow::getGuCode)
                .containsExactly(tuple(1L, "11010"));
    }

    @Test
    void delete는_FK_제약때문에_하위테이블부터_지우고_user_conditions를_마지막에_지운다() {
        writer.delete(1L);

        InOrder inOrder = inOrder(
                conditionWeightMapper, conditionEssentialMapper, preferredHouseTypeMapper,
                conditionGuMapper, userConditionMapper);
        inOrder.verify(conditionWeightMapper).deleteByConditionId(1L);
        inOrder.verify(conditionEssentialMapper).deleteByConditionId(1L);
        inOrder.verify(preferredHouseTypeMapper).deleteByConditionId(1L);
        inOrder.verify(conditionGuMapper).deleteByConditionId(1L);
        inOrder.verify(userConditionMapper).deleteByConditionId(1L);
    }

    private void mockGeneratedConditionId() {
        doAnswer(invocation -> {
            UserConditionRow row = invocation.getArgument(0);
            row.setConditionId(1L);
            return 1;
        }).when(userConditionMapper).insert(any());
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<ConditionWeightRow>> weightRowsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<ConditionEssentialRow>> essentialRowsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<PreferredHouseTypeRow>> houseTypeRowsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<ConditionGuRow>> guRowsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}

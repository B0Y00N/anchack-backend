package com.kbait.anchack.condition.service.impl;

import com.kbait.anchack.condition.dto.ConditionWeightRow;
import com.kbait.anchack.condition.dto.UserConditionCreateRequest;
import com.kbait.anchack.condition.dto.UserConditionRow;
import com.kbait.anchack.condition.mapper.ConditionEssentialMapper;
import com.kbait.anchack.condition.mapper.ConditionGuMapper;
import com.kbait.anchack.condition.mapper.ConditionWeightMapper;
import com.kbait.anchack.condition.mapper.PreferredHouseTypeMapper;
import com.kbait.anchack.condition.mapper.UserConditionMapper;
import com.kbait.anchack.recommendation.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionServiceImplTest {

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

    @Mock
    private RecommendationService recommendationService;

    private ConditionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConditionServiceImpl(
                userConditionMapper,
                conditionWeightMapper,
                conditionEssentialMapper,
                preferredHouseTypeMapper,
                conditionGuMapper,
                recommendationService);
    }

    @Test
    void 우선순위_3개는_3_2_1_가중치로_저장된다() {
        mockGeneratedConditionId();
        when(recommendationService.generate(any())).thenReturn(List.of());
        ArgumentCaptor<List<ConditionWeightRow>> captor = weightRowsCaptor();

        service.createAndRecommend(10L, validRequest(List.of("SAFETY", "SPORTS", "FOOD")));

        verify(conditionWeightMapper).insertBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(ConditionWeightRow::getCategory, row -> row.getImportance().intValue())
                .containsExactly(tuple("SAFETY", 3), tuple("SPORTS", 2), tuple("FOOD", 1));
    }

    @Test
    void 지원하지_않는_우선순위_카테고리는_예외가_발생하고_아무것도_저장하지_않는다() {
        Throwable thrown = catchThrowable(
                () -> service.createAndRecommend(10L, validRequest(List.of("INVALID"))));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userConditionMapper);
    }

    @Test
    void 사용자_조건_저장_후_하위테이블_저장하고_추천을_호출한다() {
        mockGeneratedConditionId();
        when(recommendationService.generate(any())).thenReturn(List.of());

        service.createAndRecommend(10L, validRequest(List.of("SAFETY")));

        InOrder inOrder = inOrder(userConditionMapper, conditionWeightMapper, recommendationService);
        inOrder.verify(userConditionMapper).insert(any());
        inOrder.verify(conditionWeightMapper).insertBatch(anyList());
        inOrder.verify(recommendationService).generate(any());
    }

    @Test
    void guCodes가_없으면_condition_gus_insert를_호출하지_않는다() {
        mockGeneratedConditionId();
        when(recommendationService.generate(any())).thenReturn(List.of());
        UserConditionCreateRequest request = validRequest(List.of("SAFETY"));
        request.setGuCodes(null);

        service.createAndRecommend(10L, request);

        verifyNoInteractions(conditionGuMapper);
    }

    private void mockGeneratedConditionId() {
        doAnswer(invocation -> {
            UserConditionRow row = invocation.getArgument(0);
            row.setConditionId(1L);
            return 1;
        }).when(userConditionMapper).insert(any());
    }

    private UserConditionCreateRequest validRequest(List<String> priorityCategories) {
        UserConditionCreateRequest request = new UserConditionCreateRequest();
        request.setRentalType("MONTHLY");
        request.setCommuteType("PUBLIC_TRANSIT");
        request.setMaxCommuteTime(60);
        request.setMaxTransferCount(2);
        request.setPriorityCategories(priorityCategories);
        request.setMaxDeposit(3000L);
        request.setMaxRent(70);

        return request;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<ConditionWeightRow>> weightRowsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}

package com.kbait.anchack.condition.service.impl;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.common.exception.ForbiddenException;
import com.kbait.anchack.common.exception.NotFoundException;
import com.kbait.anchack.condition.dto.ConditionWeightRow;
import com.kbait.anchack.condition.dto.RecommendedDongResponse;
import com.kbait.anchack.condition.dto.SavedConditionResponse;
import com.kbait.anchack.condition.dto.UserConditionCreateRequest;
import com.kbait.anchack.condition.dto.UserConditionRow;
import com.kbait.anchack.condition.mapper.ConditionEssentialMapper;
import com.kbait.anchack.condition.mapper.ConditionGuMapper;
import com.kbait.anchack.condition.mapper.ConditionWeightMapper;
import com.kbait.anchack.condition.mapper.PreferredHouseTypeMapper;
import com.kbait.anchack.condition.mapper.UserConditionMapper;
import com.kbait.anchack.recommendation.dto.RecommendationRow;
import com.kbait.anchack.recommendation.mapper.RecommendationMapper;
import com.kbait.anchack.recommendation.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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

    @Mock
    private RecommendationMapper recommendationMapper;

    @Mock
    private AdminDongMapper adminDongMapper;

    private ConditionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConditionServiceImpl(
                userConditionMapper,
                conditionWeightMapper,
                conditionEssentialMapper,
                preferredHouseTypeMapper,
                conditionGuMapper,
                recommendationService,
                recommendationMapper,
                adminDongMapper);
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

    @Test
    void 존재하지_않는_조건을_저장하면_예외가_발생한다() {
        when(userConditionMapper.findById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.saveCondition(10L, 1L, "제목"))
                .isInstanceOf(NotFoundException.class);

        verify(userConditionMapper, never()).markSaved(any(), any());
    }

    @Test
    void 다른_사용자의_조건을_저장하면_예외가_발생한다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(999L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        assertThatThrownBy(() -> service.saveCondition(10L, 1L, "제목"))
                .isInstanceOf(ForbiddenException.class);

        verify(userConditionMapper, never()).markSaved(any(), any());
    }

    @Test
    void 본인_조건을_제목과_함께_저장하면_is_saved와_title이_함께_바뀐다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(10L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        service.saveCondition(10L, 1L, "회사 근처 원룸");

        verify(userConditionMapper).markSaved(1L, "회사 근처 원룸");
    }

    @Test
    void 앞뒤_공백만_있는_제목은_트림된다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(10L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        service.saveCondition(10L, 1L, "  회사 근처 원룸  ");

        verify(userConditionMapper).markSaved(1L, "회사 근처 원룸");
    }

    @Test
    void 제목_없이_저장하면_기존_title을_건드리지_않는다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(10L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        service.saveCondition(10L, 1L, null);

        verify(userConditionMapper).markSaved(1L, null);
    }

    @Test
    void 공백만_있는_제목은_없는_것과_동일하게_처리된다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(10L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        service.saveCondition(10L, 1L, "   ");

        verify(userConditionMapper).markSaved(1L, null);
    }

    @Test
    void 본인_조건의_저장을_취소하면_is_saved를_false로_변경한다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(10L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        service.unsaveCondition(10L, 1L);

        verify(userConditionMapper).updateIsSaved(1L, false);
    }

    @Test
    void 저장된_조건_목록은_원_단위를_만원으로_환산하고_영문_코드로_되돌려_반환한다() {
        UserConditionRow row = UserConditionRow.builder()
                .conditionId(1L)
                .title("")
                .rentalType("월세")
                .commuteType("대중교통")
                .maxDeposit(30_000_000L)
                .maxRent(700_000L)
                .minArea(new BigDecimal("20.00"))
                .build();
        when(userConditionMapper.findSavedByUserId(10L)).thenReturn(List.of(row));

        List<SavedConditionResponse> result = service.getSavedConditions(10L);

        assertThat(result).hasSize(1);
        SavedConditionResponse response = result.get(0);
        assertThat(response.getRentalType()).isEqualTo("MONTHLY");
        assertThat(response.getCommuteType()).isEqualTo("PUBLIC_TRANSIT");
        assertThat(response.getMaxDeposit()).isEqualTo(3000L);
        assertThat(response.getMaxRent()).isEqualTo(70);
    }

    @Test
    void 결과_조회는_존재하지_않는_조건이면_예외가_발생하고_recommendation_조회를_하지_않는다() {
        when(userConditionMapper.findById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.getRecommendations(10L, 1L))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(recommendationMapper);
    }

    @Test
    void 결과_조회는_admin_dong_정보를_채워서_반환하고_통근_상세_필드는_null이다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(10L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        RecommendationRow recommendationRow = RecommendationRow.builder()
                .adminDongId(5L)
                .totalScore(new BigDecimal("70.00"))
                .dataCoverageRate(new BigDecimal("100.00"))
                .rank(1)
                .commuteTime(20)
                .transferCount(0)
                .recommendationReason("reason")
                .caution("caution")
                .build();
        when(recommendationMapper.findByConditionId(1L)).thenReturn(List.of(recommendationRow));

        AdminDong adminDong = new AdminDong();
        adminDong.setAdminDongId(5L);
        adminDong.setGuName("은평구");
        adminDong.setName("증산동");
        when(adminDongMapper.findByIds(List.of(5L))).thenReturn(List.of(adminDong));

        List<RecommendedDongResponse> result = service.getRecommendations(10L, 1L);

        assertThat(result).hasSize(1);
        RecommendedDongResponse response = result.get(0);
        assertThat(response.getGuName()).isEqualTo("은평구");
        assertThat(response.getDongName()).isEqualTo("증산동");
        assertThat(response.getRoute()).isNull();
        assertThat(response.getTransitMin()).isNull();
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

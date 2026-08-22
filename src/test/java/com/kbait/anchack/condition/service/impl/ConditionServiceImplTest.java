package com.kbait.anchack.condition.service.impl;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.common.exception.ForbiddenException;
import com.kbait.anchack.common.exception.NotFoundException;
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
import com.kbait.anchack.recommendation.dto.ConditionBundle;
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
import org.springframework.dao.DeadlockLoserDataAccessException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private UserConditionWriter userConditionWriter;

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
                userConditionWriter,
                recommendationService,
                recommendationMapper,
                adminDongMapper);
    }

    @Test
    void 우선순위_3개는_3_2_1_가중치로_저장된다() {
        when(userConditionWriter.insert(any(), any(), any(), any(), any())).thenReturn(1L);
        when(recommendationService.compute(any())).thenReturn(List.of());
        when(recommendationService.persist(any(), any())).thenReturn(List.of());
        ArgumentCaptor<Map<String, BigDecimal>> captor = weightsCaptor();

        service.createAndRecommend(10L, validRequest(List.of("SAFETY", "SPORTS", "FOOD")));

        verify(userConditionWriter).insert(any(), captor.capture(), any(), any(), any());
        assertThat(captor.getValue())
                .containsEntry("SAFETY", BigDecimal.valueOf(3))
                .containsEntry("SPORTS", BigDecimal.valueOf(2))
                .containsEntry("FOOD", BigDecimal.valueOf(1));
    }

    @Test
    void 지원하지_않는_우선순위_카테고리는_예외가_발생하고_아무것도_저장하지_않는다() {
        Throwable thrown = catchThrowable(
                () -> service.createAndRecommend(10L, validRequest(List.of("INVALID"))));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userConditionWriter);
    }

    @Test
    void 사용자_조건_저장_후_계산과_반영을_순서대로_호출한다() {
        when(userConditionWriter.insert(any(), any(), any(), any(), any())).thenReturn(1L);
        when(recommendationService.compute(any())).thenReturn(List.of());
        when(recommendationService.persist(any(), any())).thenReturn(List.of());

        service.createAndRecommend(10L, validRequest(List.of("SAFETY")));

        InOrder inOrder = inOrder(userConditionWriter, recommendationService);
        inOrder.verify(userConditionWriter).insert(any(), any(), any(), any(), any());
        inOrder.verify(recommendationService).compute(any());
        inOrder.verify(recommendationService).persist(any(), any());
    }

    @Test
    void 계산이_실패하면_방금_만든_조건을_정리하고_원래_예외를_그대로_던진다() {
        when(userConditionWriter.insert(any(), any(), any(), any(), any())).thenReturn(1L);
        RuntimeException computeFailure = new RuntimeException("카카오 API 호출 실패");
        when(recommendationService.compute(any())).thenThrow(computeFailure);

        assertThatThrownBy(() -> service.createAndRecommend(10L, validRequest(List.of("SAFETY"))))
                .isSameAs(computeFailure);

        verify(userConditionWriter).delete(1L);
        verifyNoInteractions(recommendationMapper); // persist는 애초에 호출되지 않음(compute가 먼저 터짐)
    }

    @Test
    void 반영이_데드락_재시도를_다_써도_실패하면_방금_만든_조건을_정리하고_원래_예외를_그대로_던진다() {
        when(userConditionWriter.insert(any(), any(), any(), any(), any())).thenReturn(1L);
        when(recommendationService.compute(any())).thenReturn(List.of());
        DeadlockLoserDataAccessException persistFailure =
                new DeadlockLoserDataAccessException("deadlock", null);
        when(recommendationService.persist(any(), any())).thenThrow(persistFailure);

        assertThatThrownBy(() -> service.createAndRecommend(10L, validRequest(List.of("SAFETY"))))
                .isSameAs(persistFailure);

        verify(userConditionWriter).delete(1L);
        // DeadlockRetry가 총 3회 시도한다 - persist()도 그만큼 다시 호출됐어야 한다.
        verify(recommendationService, times(3)).persist(any(), any());
    }

    @Test
    void 존재하지_않는_조건을_재계산하면_예외가_발생하고_추천을_호출하지_않는다() {
        when(userConditionMapper.findById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.recompute(10L, 1L))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(recommendationService);
        verify(userConditionMapper, never()).markLatest(any());
    }

    @Test
    void 다른_사용자의_조건을_재계산하면_예외가_발생한다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(999L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        assertThatThrownBy(() -> service.recompute(10L, 1L))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(recommendationService);
        verify(userConditionMapper, never()).markLatest(any());
    }

    @Test
    void 재계산은_저장된_하위테이블을_그대로_읽어_번들을_구성하고_recommendations를_반환한다() {
        UserConditionRow condition = UserConditionRow.builder()
                .conditionId(1L)
                .userId(10L)
                .rentalType("월세")
                .destAddress("서울 영등포구 여의대로 128")
                .commuteType("대중교통")
                .maxCommuteTime(60)
                .maxTransferCount(2)
                .maxDeposit(3000L)
                .maxRent(70L)
                .minArea(new BigDecimal("20.00"))
                .build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);
        when(conditionWeightMapper.findByConditionId(1L)).thenReturn(List.of(
                ConditionWeightRow.builder().category("SAFETY").importance(new BigDecimal("2")).build()));
        when(conditionGuMapper.findGuCodesByConditionId(1L)).thenReturn(List.of("11120"));
        when(conditionEssentialMapper.findCategoriesByConditionId(1L)).thenReturn(List.of("편의점"));
        when(preferredHouseTypeMapper.findHouseTypesByConditionId(1L)).thenReturn(List.of("원룸"));

        RecommendationRow recommendationRow = RecommendationRow.builder()
                .adminDongId(5L)
                .totalScore(new BigDecimal("70.00"))
                .dataCoverageRate(new BigDecimal("100.00"))
                .rank(1)
                .recommendationReason("reason")
                .caution("caution")
                .build();
        ArgumentCaptor<ConditionBundle> bundleCaptor = ArgumentCaptor.forClass(ConditionBundle.class);
        when(recommendationService.compute(bundleCaptor.capture())).thenReturn(List.of(recommendationRow));
        when(recommendationService.persist(eq(1L), any())).thenReturn(List.of(recommendationRow));

        UserConditionCreateResponse response = service.recompute(10L, 1L);

        assertThat(response.getConditionId()).isEqualTo(1L);
        assertThat(response.getRecommendations()).hasSize(1);

        ConditionBundle bundle = bundleCaptor.getValue();
        assertThat(bundle.getConditionId()).isEqualTo(1L);
        assertThat(bundle.getRentalType()).isEqualTo("월세");
        assertThat(bundle.getDestAddress()).isEqualTo("서울 영등포구 여의대로 128");
        assertThat(bundle.getCommuteType()).isEqualTo("대중교통");
        assertThat(bundle.getGuCodes()).containsExactly("11120");
        assertThat(bundle.getEssentialCategories()).containsExactly("편의점");
        assertThat(bundle.getPreferredHouseTypes()).containsExactly("원룸");
        assertThat(bundle.getMaxDeposit()).isEqualTo(3000L);
        assertThat(bundle.getMaxRent()).isEqualTo(70);
        assertThat(bundle.getCategoryWeights()).containsEntry("SAFETY", new BigDecimal("2"));
        verify(userConditionMapper).markLatest(1L);
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
    void 저장된_조건_목록은_만원_단위를_그대로_돌려주고_영문_코드로_되돌려_반환한다() {
        UserConditionRow row = UserConditionRow.builder()
                .conditionId(1L)
                .title("")
                .rentalType("월세")
                .commuteType("대중교통")
                .maxDeposit(3000L)
                .maxRent(70L)
                .minArea(new BigDecimal("20.00"))
                .latest(false)
                .build();
        when(userConditionMapper.findSavedByUserId(10L)).thenReturn(List.of(row));

        List<SavedConditionResponse> result = service.getSavedConditions(10L);

        assertThat(result).hasSize(1);
        SavedConditionResponse response = result.get(0);
        assertThat(response.getRentalType()).isEqualTo("MONTHLY");
        assertThat(response.getCommuteType()).isEqualTo("PUBLIC_TRANSIT");
        assertThat(response.getMaxDeposit()).isEqualTo(3000L);
        assertThat(response.getMaxRent()).isEqualTo(70);
        assertThat(response.getLatest()).isFalse();
    }

    @Test
    void 결과_조회는_존재하지_않는_조건이면_예외가_발생하고_recommendation_조회를_하지_않는다() {
        when(userConditionMapper.findById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.getRecommendations(10L, 1L))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(recommendationMapper);
    }

    @Test
    void 결과_조회는_admin_dong_정보와_저장된_통근_상세를_함께_채워서_반환한다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(10L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        RecommendationRow recommendationRow = RecommendationRow.builder()
                .adminDongId(5L)
                .totalScore(new BigDecimal("70.00"))
                .dataCoverageRate(new BigDecimal("100.00"))
                .rank(1)
                .commuteTime(20)
                .transferCount(0)
                .route("6호선 증산 → 디지털미디어시티")
                .transportType("SUBWAY")
                .lineNum("6호선")
                .vehicleType("일반")
                .walkMin(5)
                .transitMin(15)
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
        assertThat(response.getRoute()).isEqualTo("6호선 증산 → 디지털미디어시티");
        assertThat(response.getTransportType()).isEqualTo("SUBWAY");
        assertThat(response.getLineNum()).isEqualTo("6호선");
        assertThat(response.getVehicleType()).isEqualTo("일반");
        assertThat(response.getWalkMin()).isEqualTo(5);
        assertThat(response.getTransitMin()).isEqualTo(15);
    }

    @Test
    void 결과_조회는_destAddress_없이_생성된_조건이면_통근_상세가_null인_채로_반환한다() {
        UserConditionRow condition = UserConditionRow.builder().conditionId(1L).userId(10L).build();
        when(userConditionMapper.findById(1L)).thenReturn(condition);

        RecommendationRow recommendationRow = RecommendationRow.builder()
                .adminDongId(5L)
                .totalScore(new BigDecimal("70.00"))
                .dataCoverageRate(new BigDecimal("100.00"))
                .rank(1)
                .recommendationReason("reason")
                .caution("caution")
                .build();
        when(recommendationMapper.findByConditionId(1L)).thenReturn(List.of(recommendationRow));
        when(adminDongMapper.findByIds(List.of(5L))).thenReturn(List.of());

        List<RecommendedDongResponse> result = service.getRecommendations(10L, 1L);

        assertThat(result).hasSize(1);
        RecommendedDongResponse response = result.get(0);
        assertThat(response.getRoute()).isNull();
        assertThat(response.getTransitMin()).isNull();
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
    private ArgumentCaptor<Map<String, BigDecimal>> weightsCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}

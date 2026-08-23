package com.kbait.anchack.recommendation.service.impl;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.recommendation.client.RecommendationReasonClient;
import com.kbait.anchack.recommendation.dto.CategoryScoreBreakdown;
import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.FilterFunnelStage;
import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.HardFilterResult;
import com.kbait.anchack.recommendation.dto.RankedRecommendation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.dto.RecommendationComputation;
import com.kbait.anchack.recommendation.dto.RecommendationReasonContext;
import com.kbait.anchack.recommendation.dto.RecommendationRow;
import com.kbait.anchack.recommendation.dto.RecommendationScoreRow;
import com.kbait.anchack.recommendation.mapper.RecommendationMapper;
import com.kbait.anchack.recommendation.mapper.RecommendationScoreMapper;
import com.kbait.anchack.recommendation.service.HardFilterService;
import com.kbait.anchack.recommendation.service.RecommendationScoreCalculator;
import com.kbait.anchack.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * 하드필터 → 소프트 스코어링 → 상위 5개 추출 → reason/caution 생성(compute) →
 * recommendations/recommendation_scores DELETE 후 INSERT(persist)까지 이어붙이는 진입점.
 * compute와 persist를 나눈 이유는 클래스 상단 RecommendationService 인터페이스 참고 -
 * 요약하면 데드락 재시도가 persist만 다시 실행하고, compute(카카오/OpenAI 호출)는
 * 재시도 때마다 중복 실행되지 않게 하기 위해서다.
 *
 * reason/caution 생성(OpenAI 프록시 호출)은 후보별로 순차 호출하면 read-timeout이 걸릴 때마다
 * 누적되어(최악의 경우 TOP_N * read-timeout) 사용자가 오래 기다리게 되므로 병렬로 호출한다.
 * admin_dong 이름 조회(DB) 등은 병렬화 대상에서 제외하고 메인 스레드에서 미리 끝내둔다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int TOP_N = 5;

    private final HardFilterService hardFilterService;
    private final RecommendationScoreCalculator recommendationScoreCalculator;
    private final RecommendationReasonClient recommendationReasonClient;
    private final RecommendationMapper recommendationMapper;
    private final RecommendationScoreMapper recommendationScoreMapper;
    private final AdminDongMapper adminDongMapper;

    /**
     * 의도적으로 @Transactional을 안 붙인다 - hardFilterService.filter()가 destAddress가
     * 있으면 카카오 API를 호출하는데(CommuteFilter), 트랜잭션을 걸면 그 네트워크 호출이
     * 끝날 때까지 DB 커넥션을 붙잡아두게 된다. 여기서 나가는 개별 매퍼 조회들은 각자
     * 자기 완결적인 SELECT라 공유 트랜잭션 없이 실행돼도 문제없다.
     */
    @Override
    public RecommendationComputation compute(ConditionBundle condition) {
        HardFilterResult hardFilterResult = hardFilterService.filter(condition);
        List<RecommendationCandidate> candidates = hardFilterResult.getCandidates();
        List<RankedRecommendation> ranked = recommendationScoreCalculator.calculate(candidates, condition);
        List<RankedRecommendation> topRanked = ranked.stream().limit(TOP_N).toList();

        List<AdminDong> adminDongs = topRanked.stream()
                .map(recommendation -> adminDongMapper.findById(recommendation.getAdminDongId()))
                .toList();

        List<RecommendationReasonContext> contexts = IntStream.range(0, topRanked.size())
                .mapToObj(i -> toReasonContext(topRanked.get(i), adminDongs.get(i), condition.getConditionId()))
                .toList();
        List<GeneratedReason> reasons = generateReasonsInParallel(contexts);

        List<RecommendationRow> rows = IntStream.range(0, topRanked.size())
                .mapToObj(i -> toRow(topRanked.get(i), adminDongs.get(i), reasons.get(i), condition.getConditionId()))
                .toList();

        return RecommendationComputation.builder()
                .rows(rows)
                .filterFunnel(toFinalFunnel(hardFilterResult, rows))
                .build();
    }

    /**
     * 하드필터 단계 퍼널에 FINAL(최종 5개 추출) 단계를 이어붙인다. 후보가 하드필터 단계에서
     * 이미 0개가 됐으면(candidates 비어있음) FINAL 단계도 실행된 적 없는 셈이라 추가하지
     * 않는다 - "실행되지 않은 단계"와 "실행됐지만 0개인 단계"를 구분하기 위함이다.
     */
    private List<FilterFunnelStage> toFinalFunnel(HardFilterResult hardFilterResult, List<RecommendationRow> rows) {
        List<FilterFunnelStage> funnel = new ArrayList<>(hardFilterResult.getFilterFunnel());

        if (!hardFilterResult.getCandidates().isEmpty()) {
            funnel.add(FilterFunnelStage.builder().stage("FINAL").label("BEST").count(rows.size()).build());
        }

        return funnel;
    }

    @Override
    @Transactional
    public List<RecommendationRow> persist(Long conditionId, List<RecommendationRow> rows) {
        replaceRecommendations(conditionId, rows);
        replaceRecommendationScores(rows);

        return rows;
    }

    private List<GeneratedReason> generateReasonsInParallel(List<RecommendationReasonContext> contexts) {
        if (contexts.isEmpty()) {
            return List.of();
        }

        ExecutorService executor = Executors.newFixedThreadPool(contexts.size());

        try {
            List<CompletableFuture<GeneratedReason>> futures = contexts.stream()
                    .map(context -> CompletableFuture.supplyAsync(
                            () -> recommendationReasonClient.generate(context), executor))
                    .toList();

            return futures.stream().map(CompletableFuture::join).toList();
        } finally {
            executor.shutdown();
        }
    }

    private RecommendationRow toRow(
            RankedRecommendation recommendation,
            AdminDong adminDong,
            GeneratedReason reason,
            Long conditionId
    ) {
        return RecommendationRow.builder()
                .conditionId(conditionId)
                .adminDongId(recommendation.getAdminDongId())
                .totalScore(recommendation.getTotalScore())
                .dataCoverageRate(recommendation.getDataCoverageRate())
                .commuteTime(recommendation.getCommuteTime())
                .transferCount(recommendation.getTransferCount())
                .route(recommendation.getRoute())
                .transportType(recommendation.getTransportType())
                .lineNum(recommendation.getLineNum())
                .vehicleType(recommendation.getVehicleType())
                .walkMin(recommendation.getWalkMin())
                .transitMin(recommendation.getTransitMin())
                .rank(recommendation.getRank())
                .recommendationReason(reason.getRecommendationReason())
                .caution(reason.getCaution())
                .categoryBreakdowns(recommendation.getCategoryBreakdowns())
                .guName(adminDong.getGuName())
                .dongName(adminDong.getName())
                .latitude(adminDong.getLatitude())
                .longitude(adminDong.getLongitude())
                .build();
    }

    private RecommendationReasonContext toReasonContext(
            RankedRecommendation recommendation,
            AdminDong adminDong,
            Long conditionId
    ) {
        return RecommendationReasonContext.builder()
                .adminDongId(recommendation.getAdminDongId())
                .adminDongName(adminDong.getGuName() + " " + adminDong.getName())
                .conditionId(conditionId)
                .totalScore(recommendation.getTotalScore())
                .commuteTime(recommendation.getCommuteTime())
                .transferCount(recommendation.getTransferCount())
                .categoryBreakdowns(recommendation.getCategoryBreakdowns())
                .build();
    }

    /**
     * recommendation_scores -> recommendations 순으로 삭제한다.
     * FK(recommendation_scores.recommendation_id)에 ON DELETE CASCADE가 없어서
     * 순서를 바꾸면 FK 제약 위반이 난다.
     *
     * rows가 비어있으면(하드필터를 통과한 후보가 하나도 없는 경우) insertBatch를 호출하지
     * 않는다 - MyBatis <foreach>가 빈 리스트에 대해 "VALUES" 뒤에 아무것도 없는 SQL을
     * 그대로 렌더링해 문법 오류가 나기 때문이다.
     */
    private void replaceRecommendations(Long conditionId, List<RecommendationRow> rows) {
        recommendationScoreMapper.deleteByConditionId(conditionId);
        recommendationMapper.deleteByConditionId(conditionId);

        if (!rows.isEmpty()) {
            recommendationMapper.insertBatch(rows);
        }
    }

    private void replaceRecommendationScores(List<RecommendationRow> rows) {
        List<RecommendationScoreRow> scoreRows = rows.stream()
                .flatMap(row -> toScoreRows(row).stream())
                .toList();

        if (!scoreRows.isEmpty()) {
            recommendationScoreMapper.insertBatch(scoreRows);
        }
    }

    private List<RecommendationScoreRow> toScoreRows(RecommendationRow row) {
        return row.getCategoryBreakdowns().stream()
                .map(breakdown -> toScoreRow(row.getRecommendationId(), breakdown))
                .toList();
    }

    private RecommendationScoreRow toScoreRow(Long recommendationId, CategoryScoreBreakdown breakdown) {
        return RecommendationScoreRow.builder()
                .recommendationId(recommendationId)
                .category(breakdown.getCategory())
                .rawScore(breakdown.getRawScore())
                .weight(breakdown.getWeight())
                .weightedScore(breakdown.getWeightedScore())
                .build();
    }
}

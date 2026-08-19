package com.kbait.anchack.recommendation.service.impl;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.recommendation.client.RecommendationReasonClient;
import com.kbait.anchack.recommendation.dto.CategoryScoreBreakdown;
import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.RankedRecommendation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * 하드필터 → 소프트 스코어링 → 상위 5개 추출 → reason/caution 생성 →
 * recommendations/recommendation_scores DELETE 후 INSERT까지 이어붙이는 진입점.
 *
 * reason/caution 생성(OpenAI 프록시 호출)은 후보별로 순차 호출하면 read-timeout이 걸릴 때마다
 * 누적되어(최악의 경우 TOP_N * read-timeout) 사용자가 오래 기다리게 되므로 병렬로 호출한다.
 * admin_dong 이름 조회(DB) 등 트랜잭션에 묶인 작업은 병렬화 대상에서 제외하고 메인 스레드에서
 * 미리 끝내둔다 - @Transactional이 스레드 하나에 바인딩되므로 다른 스레드에서 매퍼를 호출하면
 * 같은 트랜잭션/커넥션을 타지 않는다.
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

    @Override
    @Transactional
    public List<RecommendationRow> generate(ConditionBundle condition) {
        List<RecommendationCandidate> candidates = hardFilterService.filter(condition);
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

        replaceRecommendations(condition.getConditionId(), rows);
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
     */
    private void replaceRecommendations(Long conditionId, List<RecommendationRow> rows) {
        recommendationScoreMapper.deleteByConditionId(conditionId);
        recommendationMapper.deleteByConditionId(conditionId);
        recommendationMapper.insertBatch(rows);
    }

    private void replaceRecommendationScores(List<RecommendationRow> rows) {
        List<RecommendationScoreRow> scoreRows = rows.stream()
                .flatMap(row -> toScoreRows(row).stream())
                .toList();

        recommendationScoreMapper.insertBatch(scoreRows);
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

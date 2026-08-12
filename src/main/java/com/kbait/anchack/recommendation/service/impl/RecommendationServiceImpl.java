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

/**
 * 하드필터 → 소프트 스코어링 → 상위 5개 추출 → reason/caution 생성 →
 * recommendations/recommendation_scores DELETE 후 INSERT까지 이어붙이는 진입점.
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

        List<RecommendationRow> rows = ranked.stream()
                .limit(TOP_N)
                .map(recommendation -> toRow(recommendation, condition.getConditionId()))
                .toList();

        replaceRecommendations(condition.getConditionId(), rows);
        replaceRecommendationScores(rows);

        return rows;
    }

    private RecommendationRow toRow(RankedRecommendation recommendation, Long conditionId) {
        GeneratedReason reason = recommendationReasonClient.generate(toReasonContext(recommendation, conditionId));

        return RecommendationRow.builder()
                .conditionId(conditionId)
                .adminDongId(recommendation.getAdminDongId())
                .totalScore(recommendation.getTotalScore())
                .dataCoverageRate(recommendation.getDataCoverageRate())
                .commuteTime(recommendation.getCommuteTime())
                .transferCount(recommendation.getTransferCount())
                .rank(recommendation.getRank())
                .recommendationReason(reason.getRecommendationReason())
                .caution(reason.getCaution())
                .categoryBreakdowns(recommendation.getCategoryBreakdowns())
                .build();
    }

    private RecommendationReasonContext toReasonContext(RankedRecommendation recommendation, Long conditionId) {
        return RecommendationReasonContext.builder()
                .adminDongId(recommendation.getAdminDongId())
                .adminDongName(resolveAdminDongName(recommendation.getAdminDongId()))
                .conditionId(conditionId)
                .totalScore(recommendation.getTotalScore())
                .commuteTime(recommendation.getCommuteTime())
                .transferCount(recommendation.getTransferCount())
                .categoryBreakdowns(recommendation.getCategoryBreakdowns())
                .build();
    }

    private String resolveAdminDongName(Long adminDongId) {
        AdminDong adminDong = adminDongMapper.findById(adminDongId);

        return adminDong.getGuName() + " " + adminDong.getName();
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

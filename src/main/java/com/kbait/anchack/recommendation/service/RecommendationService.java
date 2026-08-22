package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.RecommendationRow;

import java.util.List;

public interface RecommendationService {

    /**
     * 하드필터 -> 소프트 스코어링 -> 상위 5개 추출 -> reason/caution 생성까지, DB 쓰기 없이
     * 순수 계산만 한다. 카카오 통근 API·OpenAI reason 생성 같은 외부 호출이 여기서
     * 일어나는데, 이 메서드 자체는 트랜잭션을 열지 않는다 - DB 커넥션을 네트워크 호출
     * 동안 붙잡아두지 않기 위해서다(호출자도 이 메서드를 재시도해서는 안 된다 - 외부
     * 호출이 중복 실행된다).
     */
    List<RecommendationRow> compute(ConditionBundle condition);

    /**
     * compute()가 만든 결과를 recommendations/recommendation_scores에 DELETE 후 INSERT로
     * 반영한다. 이 메서드만 트랜잭션으로 묶여 있고, 데드락 재시도(DeadlockRetry)의 대상도
     * 이 메서드다 - compute()의 외부 호출을 재시도 때마다 반복하지 않기 위해 compute와
     * 분리해뒀다.
     */
    List<RecommendationRow> persist(Long conditionId, List<RecommendationRow> rows);
}

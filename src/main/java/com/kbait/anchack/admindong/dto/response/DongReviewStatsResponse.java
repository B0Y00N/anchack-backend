package com.kbait.anchack.admindong.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 동네 둘러보기(explore) - 구 선택 시 동 목록에 보여줄 리뷰 요약 1건.
 * 프론트엔드 region/api/neighborhood.js의 getReviewStatsByGu() 응답.
 */
@Getter
@Builder
public class DongReviewStatsResponse {

    private String dongName;
    private Long reviewCount;
    private Double avgRating;
}

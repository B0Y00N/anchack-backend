package com.kbait.anchack.admindong.mapper;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.dto.response.DongReviewStatsResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminDongMapper {

    /**
     * 구 이름과 행정동 이름으로 행정동 정보를 조회한다.
     * (동 이름 프론트엔드 문자열 -> 실제 admin_dong_id 매핑용)
     */
    AdminDong findByGuNameAndDongName(
        @Param("guName") String guName,
        @Param("dongName") String dongName
    );

    AdminDong findById(
        @Param("adminDongId") Long adminDongId
    );

    List<AdminDong> findAllCodeMappings();

    List<AdminDong> findByIds(
        @Param("adminDongIds") List<Long> adminDongIds
    );

    /**
     * 구에 속한 모든 행정동의 활성 리뷰 개수/평균 별점을 한 번에 조회한다.
     * (동네 둘러보기 - 구 선택 시 동 목록 리뷰 요약용)
     * 리뷰가 없는 동도 count=0, avgRating=null로 함께 내려온다.
     */
    List<DongReviewStatsResponse> findReviewStatsByGuName(
        @Param("guName") String guName
    );
}

package com.kbait.anchack.admindong.mapper;

import com.kbait.anchack.admindong.dto.AdminDongMetricsRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 행정동 상세 배치 조회(P1-b) 전용. safety/healthcare/life_convenience/sports/nature_metrics와
 * gu_crime_stats, places(편의점)를 한 번의 조인 쿼리로 묶어서, admin_dong_id 목록당 지표
 * 테이블 수만큼 별도 왕복하지 않도록 한다.
 */
public interface AdminDongDetailMapper {

    List<AdminDongMetricsRow> findMetricsByIds(@Param("adminDongIds") List<Long> adminDongIds);
}

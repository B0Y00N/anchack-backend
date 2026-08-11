package com.kbait.anchack.recommendation.mapper;

import com.kbait.anchack.recommendation.dto.AdminDongLocation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** admindong.mapper.AdminDongMapper와 Bean 이름이 충돌해 recommendation 전용으로 이름 구분. */
public interface RecommendationAdminDongMapper {

    List<Long> findAllIds();

    List<Long> findIdsByGuCodes(@Param("guCodes") List<String> guCodes);

    List<AdminDongLocation> findLocationsByIds(@Param("adminDongIds") List<Long> adminDongIds);
}

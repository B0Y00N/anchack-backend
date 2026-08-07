package com.kbait.anchack.recommendation.mapper;

import com.kbait.anchack.recommendation.dto.AdminDongLocation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminDongMapper {

    List<Long> findAllIds();

    List<Long> findIdsByGuCodes(@Param("guCodes") List<String> guCodes);

    List<AdminDongLocation> findLocationsByIds(@Param("adminDongIds") List<Long> adminDongIds);
}

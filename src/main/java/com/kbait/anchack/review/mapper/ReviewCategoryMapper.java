package com.kbait.anchack.review.mapper;

import com.kbait.anchack.review.dto.response.ReviewCategoryResponse;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReviewCategoryMapper {

    List<ReviewCategoryResponse> findAll();
}

package com.kbait.anchack.review.mapper;

import com.kbait.anchack.review.domain.ReviewCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReviewCategoryMapper {

    List<ReviewCategory> findAll();
}

package com.kbait.anchack.review.service;

import com.kbait.anchack.review.dto.response.ReviewCategoryResponse;

import java.util.List;

/**
 * 리뷰 평가 항목(소음, 청결, 안전 등) 조회를 담당한다.
 */
public interface ReviewCategoryService {

    List<ReviewCategoryResponse> getCategories();
}

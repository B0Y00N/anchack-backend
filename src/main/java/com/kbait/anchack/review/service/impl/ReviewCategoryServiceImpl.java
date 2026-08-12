package com.kbait.anchack.review.service.impl;

import com.kbait.anchack.review.dto.response.ReviewCategoryResponse;
import com.kbait.anchack.review.mapper.ReviewCategoryMapper;
import com.kbait.anchack.review.service.ReviewCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewCategoryServiceImpl implements ReviewCategoryService {

    private final ReviewCategoryMapper reviewCategoryMapper;

    public ReviewCategoryServiceImpl(ReviewCategoryMapper reviewCategoryMapper) {
        this.reviewCategoryMapper = reviewCategoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewCategoryResponse> getCategories() {
        return reviewCategoryMapper.findAll().stream()
            .map(ReviewCategoryResponse::from)
            .collect(Collectors.toList());
    }
}

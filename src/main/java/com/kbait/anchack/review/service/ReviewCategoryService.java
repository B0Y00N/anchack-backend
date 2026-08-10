package com.kbait.anchack.review.service;

import com.kbait.anchack.review.dto.response.ReviewCategoryResponse;
import com.kbait.anchack.review.mapper.ReviewCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewCategoryService {

    private final ReviewCategoryMapper reviewCategoryMapper;

    @Transactional(readOnly = true)
    public List<ReviewCategoryResponse> getCategories() {
        return reviewCategoryMapper.findAll();
    }
}

package com.kbait.anchack.review.controller;

import com.kbait.anchack.review.dto.response.ReviewCategoryResponse;
import com.kbait.anchack.review.service.ReviewCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/review-categories")
@RequiredArgsConstructor
public class ReviewCategoryController {

    private final ReviewCategoryService reviewCategoryService;

    @GetMapping
    public ResponseEntity<List<ReviewCategoryResponse>> getCategories() {
        return ResponseEntity.ok(
            reviewCategoryService.getCategories()
        );
    }
}

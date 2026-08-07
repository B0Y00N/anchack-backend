package com.kbait.anchack.review.controller;

import com.kbait.anchack.common.security.JwtAuthenticationFilter;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
import com.kbait.anchack.review.dto.response.ReviewResponse;
import com.kbait.anchack.review.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(
        ReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    // 특정 행정동의 공개 리뷰 목록
    @GetMapping
    public ResponseEntity<?> getReviews(
        @RequestParam Long adminDongId
    ) {
        try {
            List<ReviewResponse> reviews =
                reviewService
                    .getReviewsByAdminDong(
                        adminDongId
                    );

            return ResponseEntity.ok(reviews);
        } catch (IllegalArgumentException exception) {
            return badRequest(
                exception.getMessage()
            );
        }
    }

    // 리뷰 상세 조회
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReview(
        @PathVariable Long reviewId
    ) {
        try {
            return ResponseEntity.ok(
                reviewService.getReview(
                    reviewId
                )
            );
        } catch (IllegalArgumentException exception) {
            return notFound(
                exception.getMessage()
            );
        } catch (IllegalStateException exception) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                    createErrorResponse(
                        "REVIEW_NOT_AVAILABLE",
                        exception.getMessage()
                    )
                );
        }
    }

    // 로그인 사용자가 작성한 리뷰 목록
    @GetMapping("/me")
    public ResponseEntity<?> getMyReviews(
        HttpServletRequest request
    ) {
        Long userId =
            resolveUserId(request);

        if (userId == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(
            reviewService.getMyReviews(userId)
        );
    }

    // 리뷰 등록
    @PostMapping
    public ResponseEntity<?> createReview(
        HttpServletRequest request,
        @RequestBody ReviewCreateRequest createRequest
    ) {
        Long userId =
            resolveUserId(request);

        if (userId == null) {
            return unauthorized();
        }

        try {
            ReviewResponse response =
                reviewService.createReview(
                    userId,
                    createRequest
                );

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
        } catch (IllegalArgumentException exception) {
            return badRequest(
                exception.getMessage()
            );
        }
    }

    // 본인 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
        HttpServletRequest request,
        @PathVariable Long reviewId,
        @RequestBody ReviewUpdateRequest updateRequest
    ) {
        Long userId =
            resolveUserId(request);

        if (userId == null) {
            return unauthorized();
        }

        try {
            return ResponseEntity.ok(
                reviewService.updateReview(
                    userId,
                    reviewId,
                    updateRequest
                )
            );
        } catch (SecurityException exception) {
            return forbidden(
                exception.getMessage()
            );
        } catch (IllegalArgumentException exception) {
            return badRequest(
                exception.getMessage()
            );
        } catch (IllegalStateException exception) {
            return badRequest(
                exception.getMessage()
            );
        }
    }

    // 본인 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
        HttpServletRequest request,
        @PathVariable Long reviewId
    ) {
        Long userId =
            resolveUserId(request);

        if (userId == null) {
            return unauthorized();
        }

        try {
            reviewService.deleteReview(
                userId,
                reviewId
            );

            return ResponseEntity.noContent().build();
        } catch (SecurityException exception) {
            return forbidden(
                exception.getMessage()
            );
        } catch (IllegalArgumentException exception) {
            return notFound(
                exception.getMessage()
            );
        }
    }

    private Long resolveUserId(
        HttpServletRequest request
    ) {
        Object userIdAttribute =
            request.getAttribute(
                JwtAuthenticationFilter
                    .USER_ID_ATTRIBUTE
            );

        if (userIdAttribute instanceof Long) {
            return (Long) userIdAttribute;
        }

        if (userIdAttribute instanceof Number) {
            return ((Number) userIdAttribute)
                .longValue();
        }

        return null;
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                createErrorResponse(
                    "UNAUTHORIZED",
                    "로그인이 필요합니다."
                )
            );
    }

    private ResponseEntity<?> forbidden(
        String message
    ) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                createErrorResponse(
                    "FORBIDDEN",
                    message
                )
            );
    }

    private ResponseEntity<?> badRequest(
        String message
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                createErrorResponse(
                    "INVALID_REVIEW",
                    message
                )
            );
    }

    private ResponseEntity<?> notFound(
        String message
    ) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                createErrorResponse(
                    "REVIEW_NOT_FOUND",
                    message
                )
            );
    }

    private Map<String, Object> createErrorResponse(
        String code,
        String message
    ) {
        Map<String, Object> error =
            new LinkedHashMap<>();

        error.put("code", code);
        error.put("message", message);

        return error;
    }
}

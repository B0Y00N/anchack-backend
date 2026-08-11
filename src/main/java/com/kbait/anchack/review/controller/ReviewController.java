package com.kbait.anchack.review.controller;

import com.kbait.anchack.common.security.AuthenticatedUserResolver;
import com.kbait.anchack.review.dto.request.ReviewCreateRequest;
import com.kbait.anchack.review.dto.request.ReviewReactionRequest;
import com.kbait.anchack.review.dto.request.ReviewUpdateRequest;
import com.kbait.anchack.review.dto.response.ReviewReactionResponse;
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

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 특정 행정동의 공개 리뷰 목록 조회. 로그인하지 않아도 조회할 수 있지만,
     * 로그인한 사용자라면 내가 남긴 반응(myReaction)을 함께 내려준다.
     *
     * GET /api/reviews?adminDongId=1
     */
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviewsByAdminDong(
        HttpServletRequest httpRequest,
        @RequestParam Long adminDongId
    ) {
        Long viewerId = AuthenticatedUserResolver.resolveUserId(httpRequest);

        return ResponseEntity.ok(
            reviewService.getReviewsByAdminDong(adminDongId, viewerId)
        );
    }

    /**
     * 리뷰 상세 조회
     *
     * GET /api/reviews/1
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
        HttpServletRequest httpRequest,
        @PathVariable Long reviewId
    ) {
        Long viewerId = AuthenticatedUserResolver.resolveUserId(httpRequest);

        return ResponseEntity.ok(reviewService.getReview(reviewId, viewerId));
    }

    /**
     * 로그인 사용자가 작성한 리뷰 조회
     *
     * GET /api/reviews/me
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(HttpServletRequest httpRequest) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        return ResponseEntity.ok(reviewService.getMyReviews(userId));
    }

    /**
     * 리뷰 등록
     *
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
        HttpServletRequest httpRequest,
        @RequestBody ReviewCreateRequest request
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(reviewService.createReview(userId, request));
    }

    /**
     * 리뷰 수정
     *
     * PUT /api/reviews/1
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
        HttpServletRequest httpRequest,
        @PathVariable Long reviewId,
        @RequestBody ReviewUpdateRequest request
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        return ResponseEntity.ok(reviewService.updateReview(userId, reviewId, request));
    }

    /**
     * 리뷰 삭제. 실제 데이터 삭제가 아니라 상태를 DELETED로 변경한다.
     *
     * DELETE /api/reviews/1
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, Object>> deleteReview(
        HttpServletRequest httpRequest,
        @PathVariable Long reviewId
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        reviewService.deleteReview(userId, reviewId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "리뷰가 삭제되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 리뷰 좋아요 / 싫어요. 같은 반응을 다시 누르면 취소되고, 반대 반응을 누르면 바뀐다.
     *
     * POST /api/reviews/1/reactions
     * body: { "reactionType": "LIKE" | "DISLIKE" }
     */
    @PostMapping("/{reviewId}/reactions")
    public ResponseEntity<ReviewReactionResponse> reactToReview(
        HttpServletRequest httpRequest,
        @PathVariable Long reviewId,
        @RequestBody ReviewReactionRequest request
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);
        String reactionType = request == null ? null : request.getReactionType();

        return ResponseEntity.ok(reviewService.reactToReview(userId, reviewId, reactionType));
    }
}

package com.kbait.anchack.review.controller.admin;

import com.kbait.anchack.common.security.AuthenticatedUserResolver;
import com.kbait.anchack.review.dto.request.AdminReviewStatusRequest;
import com.kbait.anchack.review.dto.response.ReviewResponse;
import com.kbait.anchack.review.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 관리자용 리뷰 조회 및 상태(숨김/삭제/복구) 변경을 처리한다.
 * 권한 검증은 ReviewService가 담당하며, 실패 시 예외는 GlobalExceptionHandler가 처리한다.
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(
        HttpServletRequest request,
        @RequestParam(required = false) String status
    ) {
        Long adminId = AuthenticatedUserResolver.requireUserId(request);

        return ResponseEntity.ok(reviewService.getReviewsForAdmin(adminId, status));
    }

    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<ReviewResponse> updateReviewStatus(
        HttpServletRequest request,
        @PathVariable Long reviewId,
        @RequestBody AdminReviewStatusRequest statusRequest
    ) {
        Long adminId = AuthenticatedUserResolver.requireUserId(request);

        return ResponseEntity.ok(
            reviewService.updateReviewStatusByAdmin(adminId, reviewId, statusRequest)
        );
    }
}

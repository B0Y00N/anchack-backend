package com.kbait.anchack.review.controller.admin;

import com.kbait.anchack.common.security.JwtAuthenticationFilter;
import com.kbait.anchack.review.dto.request.AdminReviewStatusRequest;
import com.kbait.anchack.review.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(
        ReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    // 관리자용 전체 리뷰 조회
    @GetMapping
    public ResponseEntity<?> getReviews(
        HttpServletRequest request,
        @RequestParam(
            required = false
        ) String status
    ) {
        Long adminId =
            resolveUserId(request);

        if (adminId == null) {
            return unauthorized();
        }

        try {
            return ResponseEntity.ok(
                reviewService
                    .getReviewsForAdmin(
                        adminId,
                        status
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
        }
    }

    // 리뷰 숨김, 삭제 또는 복구
    @PatchMapping("/{reviewId}/status")
    public ResponseEntity<?> updateReviewStatus(
        HttpServletRequest request,
        @PathVariable Long reviewId,
        @RequestBody AdminReviewStatusRequest statusRequest
    ) {
        Long adminId =
            resolveUserId(request);

        if (adminId == null) {
            return unauthorized();
        }

        try {
            return ResponseEntity.ok(
                reviewService
                    .updateReviewStatusByAdmin(
                        adminId,
                        reviewId,
                        statusRequest
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
                    "INVALID_REQUEST",
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

package com.kbait.anchack.condition.controller;

import com.kbait.anchack.common.exception.ErrorCode;
import com.kbait.anchack.common.response.ApiResponse;
import com.kbait.anchack.common.security.AuthenticatedUserResolver;
import com.kbait.anchack.common.security.JwtAuthenticationFilter;
import com.kbait.anchack.condition.dto.RecommendedDongResponse;
import com.kbait.anchack.condition.dto.SaveConditionRequest;
import com.kbait.anchack.condition.dto.SavedConditionResponse;
import com.kbait.anchack.condition.dto.UserConditionCreateRequest;
import com.kbait.anchack.condition.dto.UserConditionCreateResponse;
import com.kbait.anchack.condition.service.ConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/user-conditions")
@RequiredArgsConstructor
public class ConditionController {

    private final ConditionService conditionService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserConditionCreateResponse>> create(
            HttpServletRequest httpRequest,
            @Valid @RequestBody UserConditionCreateRequest request
    ) {
        Long userId = resolveUserId(httpRequest);

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.error(ErrorCode.AUTH_UNAUTHORIZED.name(), ErrorCode.AUTH_UNAUTHORIZED.getMessage()));
        }

        UserConditionCreateResponse response = conditionService.createAndRecommend(userId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 조건 저장. 이미 저장돼 있어도 에러 없이 그대로 성공 처리한다.
     * body의 title은 선택이다 - 안 주거나 공백만 주면 기존 title을 그대로 둔다.
     *
     * PUT /api/user-conditions/1/save
     * body: { "title": "회사 근처 원룸" }
     */
    @PutMapping("/{conditionId}/save")
    public ResponseEntity<Void> save(
            HttpServletRequest httpRequest,
            @PathVariable Long conditionId,
            @Valid @RequestBody(required = false) SaveConditionRequest request
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);
        String title = request == null ? null : request.getTitle();

        conditionService.saveCondition(userId, conditionId, title);

        return ResponseEntity.noContent().build();
    }

    /**
     * 조건 저장 취소. 저장돼 있지 않아도 에러 없이 그대로 성공 처리한다.
     *
     * DELETE /api/user-conditions/1/save
     */
    @DeleteMapping("/{conditionId}/save")
    public ResponseEntity<Void> unsave(
            HttpServletRequest httpRequest,
            @PathVariable Long conditionId
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        conditionService.unsaveCondition(userId, conditionId);

        return ResponseEntity.noContent().build();
    }

    /**
     * 로그인 사용자가 저장한 조건 목록 조회.
     *
     * GET /api/user-conditions/saved
     */
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<List<SavedConditionResponse>>> getSaved(
            HttpServletRequest httpRequest
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        return ResponseEntity.ok(ApiResponse.success(conditionService.getSavedConditions(userId)));
    }

    /**
     * 조건의 추천 결과 조회. 재계산하지 않고 최초 생성 시점에 저장된 결과를 그대로 반환한다.
     *
     * GET /api/user-conditions/1/recommendations
     */
    @GetMapping("/{conditionId}/recommendations")
    public ResponseEntity<ApiResponse<List<RecommendedDongResponse>>> getRecommendations(
            HttpServletRequest httpRequest,
            @PathVariable Long conditionId
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        return ResponseEntity.ok(ApiResponse.success(conditionService.getRecommendations(userId, conditionId)));
    }

    /**
     * 저장된 조건의 파라미터를 그대로 다시 계산한다(재요청 시점 기준 최신 지표/카카오
     * 경로로 새로 계산 - 통근 상세도 이번엔 다시 채워진다). 재계산 결과가 기존
     * recommendations를 그대로 덮어쓴다.
     *
     * POST /api/user-conditions/1/recompute
     */
    @PostMapping("/{conditionId}/recompute")
    public ResponseEntity<ApiResponse<UserConditionCreateResponse>> recompute(
            HttpServletRequest httpRequest,
            @PathVariable Long conditionId
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        UserConditionCreateResponse response = conditionService.recompute(userId, conditionId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** JWT 필터가 request attribute에 저장한 사용자 ID를 조회한다. */
    private Long resolveUserId(HttpServletRequest request) {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        if (attribute instanceof Number number) {
            return number.longValue();
        }

        return null;
    }
}

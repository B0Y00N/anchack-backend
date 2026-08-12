package com.kbait.anchack.condition.controller;

import com.kbait.anchack.common.exception.ErrorCode;
import com.kbait.anchack.common.response.ApiResponse;
import com.kbait.anchack.common.security.JwtAuthenticationFilter;
import com.kbait.anchack.condition.dto.UserConditionCreateRequest;
import com.kbait.anchack.condition.dto.UserConditionCreateResponse;
import com.kbait.anchack.condition.service.ConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

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

    /** JWT 필터가 request attribute에 저장한 사용자 ID를 조회한다. */
    private Long resolveUserId(HttpServletRequest request) {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        if (attribute instanceof Number number) {
            return number.longValue();
        }

        return null;
    }
}

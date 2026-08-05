package com.kbait.anchack.user.controller;

import com.kbait.anchack.common.security.JwtAuthenticationFilter;
import com.kbait.anchack.user.domain.User;
import com.kbait.anchack.user.dto.UserUpdateRequest;
import com.kbait.anchack.user.dto.response.UserResponse;
import com.kbait.anchack.user.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

// 마이페이지의 사용자 프로필 조회와 수정 API를 처리한다.
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserMapper userMapper;

    public UserController(
            UserMapper userMapper
    ) {
        this.userMapper = userMapper;
    }

    // 현재 로그인한 사용자의 프로필을 조회한다.
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(
            HttpServletRequest request
    ) {
        Long userId = resolveUserId(request);

        if (userId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            createErrorResponse(
                                    "UNAUTHORIZED",
                                    "인증 정보가 없습니다."
                            )
                    );
        }

        User user =
                userMapper.findById(userId);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(
                            createErrorResponse(
                                    "USER_NOT_FOUND",
                                    "사용자 정보를 찾을 수 없습니다."
                            )
                    );
        }

        return ResponseEntity.ok(
                UserResponse.from(user)
        );
    }

    // 현재 로그인한 사용자의 프로필을 수정한다.
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            HttpServletRequest request,
            @RequestBody UserUpdateRequest updateRequest
    ) {
        Long userId = resolveUserId(request);

        if (userId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            createErrorResponse(
                                    "UNAUTHORIZED",
                                    "인증 정보가 없습니다."
                            )
                    );
        }

        if (updateRequest == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                            createErrorResponse(
                                    "INVALID_REQUEST",
                                    "수정할 사용자 정보가 없습니다."
                            )
                    );
        }

        User existingUser =
                userMapper.findById(userId);

        if (existingUser == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(
                            createErrorResponse(
                                    "USER_NOT_FOUND",
                                    "사용자 정보를 찾을 수 없습니다."
                            )
                    );
        }

        updateProfileFields(
                existingUser,
                updateRequest
        );

        int updatedCount =
                userMapper.updateProfile(existingUser);

        if (updatedCount != 1) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            createErrorResponse(
                                    "PROFILE_UPDATE_FAILED",
                                    "프로필 수정에 실패했습니다."
                            )
                    );
        }

        User updatedUser =
                userMapper.findById(userId);

        if (updatedUser == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(
                            createErrorResponse(
                                    "USER_NOT_FOUND",
                                    "수정된 사용자 정보를 조회할 수 없습니다."
                            )
                    );
        }

        return ResponseEntity.ok(
                UserResponse.from(updatedUser)
        );
    }

    // 요청에 포함된 프로필 항목만 사용자 객체에 반영한다.
    private void updateProfileFields(
            User existingUser,
            UserUpdateRequest updateRequest
    ) {
        if (updateRequest.getNickname() != null) {
            existingUser.setNickname(
                    updateRequest.getNickname().trim()
            );
        }

        if (updateRequest.getBirthDate() != null) {
            existingUser.setBirthDate(
                    updateRequest.getBirthDate()
            );
        }

        if (updateRequest.getProfileImageUrl() != null) {
            existingUser.setProfileImageUrl(
                    updateRequest.getProfileImageUrl().trim()
            );
        }
    }

    // JWT 필터가 request attribute에 저장한 사용자 ID를 조회한다.
    private Long resolveUserId(
            HttpServletRequest request
    ) {
        Object userIdAttribute =
                request.getAttribute(
                        JwtAuthenticationFilter.USER_ID_ATTRIBUTE
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

    // API 오류 응답 객체를 생성한다.
    private Map<String, Object> createErrorResponse(
            String code,
            String message
    ) {
        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("code", code);
        response.put("message", message);

        return response;
    }
}

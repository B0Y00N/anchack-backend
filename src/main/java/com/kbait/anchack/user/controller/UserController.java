package com.kbait.anchack.user.controller;

import com.kbait.anchack.common.security.AuthenticatedUserResolver;
import com.kbait.anchack.user.dto.request.UserUpdateRequest;
import com.kbait.anchack.user.dto.response.UserResponse;
import com.kbait.anchack.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 마이페이지의 사용자 프로필 조회와 수정 API를 처리한다.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(HttpServletRequest request) {
        Long userId = AuthenticatedUserResolver.requireUserId(request);

        return ResponseEntity.ok(userService.getMyProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
        HttpServletRequest request,
        @RequestBody UserUpdateRequest updateRequest
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(request);

        return ResponseEntity.ok(userService.updateMyProfile(userId, updateRequest));
    }
}

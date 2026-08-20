package com.kbait.anchack.favorite.controller;

import com.kbait.anchack.common.response.ApiResponse;
import com.kbait.anchack.common.security.AuthenticatedUserResolver;
import com.kbait.anchack.favorite.dto.request.FavoriteDongRequest;
import com.kbait.anchack.favorite.dto.response.FavoriteDongResponse;
import com.kbait.anchack.favorite.service.FavoriteDongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/favorite-dongs")
@RequiredArgsConstructor
public class FavoriteDongController {

    private final FavoriteDongService favoriteDongService;

    /**
     * 로그인 사용자의 관심 동네 목록 조회. 최근 등록한 순으로 반환한다.
     *
     * GET /api/favorite-dongs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteDongResponse>>> getFavorites(
        HttpServletRequest httpRequest
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        return ResponseEntity.ok(ApiResponse.success(favoriteDongService.getFavorites(userId)));
    }

    /**
     * 즐겨찾기 추가. 이미 즐겨찾기한 행정동이어도 에러 없이 그대로 성공 처리한다.
     *
     * POST /api/favorite-dongs
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addFavorite(
        HttpServletRequest httpRequest,
        @Valid @RequestBody FavoriteDongRequest request
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        favoriteDongService.addFavorite(userId, request.getAdminDongId());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 즐겨찾기 삭제. 즐겨찾기하지 않은 상태여도 에러 없이 그대로 성공 처리한다.
     *
     * DELETE /api/favorite-dongs/1
     */
    @DeleteMapping("/{adminDongId}")
    public ResponseEntity<Void> removeFavorite(
        HttpServletRequest httpRequest,
        @PathVariable Long adminDongId
    ) {
        Long userId = AuthenticatedUserResolver.requireUserId(httpRequest);

        favoriteDongService.removeFavorite(userId, adminDongId);

        return ResponseEntity.noContent().build();
    }
}

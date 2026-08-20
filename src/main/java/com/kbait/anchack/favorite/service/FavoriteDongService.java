package com.kbait.anchack.favorite.service;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.favorite.domain.FavoriteDong;
import com.kbait.anchack.favorite.dto.response.FavoriteDongResponse;
import com.kbait.anchack.favorite.mapper.FavoriteDongMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 즐겨찾기 추가/삭제. (user_id, admin_dong_id) 복합 PK 하나짜리 단순 토글이라
 * 다른 구현체가 나올 여지가 없어 인터페이스 분리 없이 구현체만 둔다.
 */
@Service
@RequiredArgsConstructor
public class FavoriteDongService {

    private final FavoriteDongMapper favoriteDongMapper;
    private final AdminDongMapper adminDongMapper;

    /**
     * admin_dong_id가 실제로 존재하는지 먼저 확인한다 - INSERT IGNORE에 맡기면
     * FK 위반도 조용히 무시돼서, 없는 admin_dong_id를 보내도 성공한 것처럼 보인다.
     * 이미 즐겨찾기한 상태에서 다시 추가해도(중복 PK) 에러 없이 그대로 유지된다.
     */
    @Transactional
    public void addFavorite(Long userId, Long adminDongId) {
        AdminDong adminDong = adminDongMapper.findById(adminDongId);

        if (adminDong == null) {
            throw new IllegalArgumentException("존재하지 않는 행정동입니다: " + adminDongId);
        }

        favoriteDongMapper.insertIgnore(userId, adminDongId);
    }

    /** 즐겨찾기하지 않은 상태에서 삭제를 호출해도 에러를 던지지 않는다. */
    @Transactional
    public void removeFavorite(Long userId, Long adminDongId) {
        favoriteDongMapper.delete(userId, adminDongId);
    }

    @Transactional(readOnly = true)
    public List<FavoriteDongResponse> getFavorites(Long userId) {
        return favoriteDongMapper.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private FavoriteDongResponse toResponse(FavoriteDong favoriteDong) {
        return FavoriteDongResponse.builder()
                .adminDongId(favoriteDong.getAdminDongId())
                .createdAt(favoriteDong.getCreatedAt())
                .build();
    }
}

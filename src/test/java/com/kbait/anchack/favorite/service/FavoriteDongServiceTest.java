package com.kbait.anchack.favorite.service;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.favorite.domain.FavoriteDong;
import com.kbait.anchack.favorite.dto.response.FavoriteDongResponse;
import com.kbait.anchack.favorite.mapper.FavoriteDongMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteDongServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_DONG_ID = 2L;

    @Mock
    private FavoriteDongMapper favoriteDongMapper;

    @Mock
    private AdminDongMapper adminDongMapper;

    private FavoriteDongService service;

    @BeforeEach
    void setUp() {
        service = new FavoriteDongService(favoriteDongMapper, adminDongMapper);
    }

    @Test
    void 존재하지_않는_행정동을_즐겨찾기하면_예외를_던지고_INSERT하지_않는다() {
        when(adminDongMapper.findById(ADMIN_DONG_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.addFavorite(USER_ID, ADMIN_DONG_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verify(favoriteDongMapper, never()).insertIgnore(USER_ID, ADMIN_DONG_ID);
    }

    @Test
    void 존재하는_행정동을_즐겨찾기하면_INSERT_IGNORE로_등록한다() {
        when(adminDongMapper.findById(ADMIN_DONG_ID)).thenReturn(new AdminDong());

        service.addFavorite(USER_ID, ADMIN_DONG_ID);

        verify(favoriteDongMapper).insertIgnore(USER_ID, ADMIN_DONG_ID);
    }

    @Test
    void 즐겨찾기_삭제는_존재_여부와_무관하게_그대로_DELETE를_호출한다() {
        service.removeFavorite(USER_ID, ADMIN_DONG_ID);

        verify(favoriteDongMapper).delete(USER_ID, ADMIN_DONG_ID);
    }

    @Test
    void 관심_동네_목록을_조회하면_adminDongId와_createdAt을_그대로_응답에_채운다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 20, 10, 0);
        FavoriteDong favoriteDong = new FavoriteDong();
        favoriteDong.setAdminDongId(ADMIN_DONG_ID);
        favoriteDong.setCreatedAt(createdAt);
        when(favoriteDongMapper.findByUserId(USER_ID)).thenReturn(List.of(favoriteDong));

        List<FavoriteDongResponse> result = service.getFavorites(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAdminDongId()).isEqualTo(ADMIN_DONG_ID);
        assertThat(result.get(0).getCreatedAt()).isEqualTo(createdAt);
    }
}

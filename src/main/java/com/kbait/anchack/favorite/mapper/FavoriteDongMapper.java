package com.kbait.anchack.favorite.mapper;

import com.kbait.anchack.favorite.domain.FavoriteDong;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FavoriteDongMapper {

    /**
     * 로그인 사용자의 관심 동네 목록을 최근 등록한 순으로 조회한다.
     */
    List<FavoriteDong> findByUserId(
        @Param("userId") Long userId
    );

    /**
     * 즐겨찾기를 등록한다. (user_id, admin_dong_id) 복합 PK라 이미 등록돼 있으면
     * INSERT IGNORE로 조용히 아무 일도 하지 않는다 - 즐겨찾기 버튼은 두 번 눌러도
     * 에러 없이 같은 상태를 유지해야 한다.
     */
    int insertIgnore(
        @Param("userId") Long userId,
        @Param("adminDongId") Long adminDongId
    );

    /**
     * 즐겨찾기를 삭제한다. 등록돼 있지 않아도(0행 삭제) 에러를 던지지 않는다 -
     * insertIgnore와 동일한 이유로 삭제도 멱등하게 둔다.
     */
    int delete(
        @Param("userId") Long userId,
        @Param("adminDongId") Long adminDongId
    );
}

package com.kbait.anchack.admindong.mapper;

import com.kbait.anchack.admindong.domain.AdminDong;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AdminDongMapper {

    /**
     * 구 이름과 행정동 이름으로 행정동 정보를 조회한다.
     * (동 이름 프론트엔드 문자열 -> 실제 admin_dong_id 매핑용)
     */
    AdminDong findByGuNameAndDongName(
        @Param("guName") String guName,
        @Param("dongName") String dongName
    );

    AdminDong findById(
        @Param("adminDongId") Long adminDongId
    );

    List<AdminDong> findAllCodeMappings();

    List<AdminDong> findByIds(
        @Param("adminDongIds") List<Long> adminDongIds
    );
}

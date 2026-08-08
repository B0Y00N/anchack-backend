package com.kbait.anchack.admindong.service;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.dto.response.AdminDongResponse;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDongService {

    private final AdminDongMapper adminDongMapper;

    public AdminDongService(
        AdminDongMapper adminDongMapper
    ) {
        this.adminDongMapper = adminDongMapper;
    }

    /**
     * 프론트엔드에서 다루는 "구 이름 + 행정동 이름" 문자열을
     * 실제 DB의 admin_dong_id로 변환한다.
     * 리뷰 작성/조회는 admin_dong_id를 기준으로 동작하기 때문에
     * 화면에서 리뷰 기능을 쓰기 전에 이 조회가 선행되어야 한다.
     */
    @Transactional(readOnly = true)
    public AdminDongResponse getAdminDong(
        String guName,
        String dongName
    ) {
        if (
            guName == null ||
                guName.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "구 이름이 필요합니다."
            );
        }

        if (
            dongName == null ||
                dongName.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "행정동 이름이 필요합니다."
            );
        }

        AdminDong adminDong =
            adminDongMapper
                .findByGuNameAndDongName(
                    guName.trim(),
                    dongName.trim()
                );

        if (adminDong == null) {
            throw new IllegalArgumentException(
                "해당 행정동 정보를 찾을 수 없습니다: "
                    + guName + " " + dongName
            );
        }

        return AdminDongResponse.from(adminDong);
    }
}

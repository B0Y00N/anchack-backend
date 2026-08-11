package com.kbait.anchack.admindong.dto.response;

import com.kbait.anchack.admindong.domain.AdminDong;
import lombok.Getter;

@Getter
public class AdminDongResponse {

    private Long adminDongId;
    private String name;
    private String guName;

    public static AdminDongResponse from(
        AdminDong adminDong
    ) {
        AdminDongResponse response =
            new AdminDongResponse();

        response.adminDongId =
            adminDong.getAdminDongId();
        response.name =
            adminDong.getName();
        response.guName =
            adminDong.getGuName();

        return response;
    }
}

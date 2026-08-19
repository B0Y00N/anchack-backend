package com.kbait.anchack.admindong.controller;

import com.kbait.anchack.admindong.dto.response.AdminDongDetailResponse;
import com.kbait.anchack.admindong.dto.response.AdminDongResponse;
import com.kbait.anchack.admindong.service.AdminDongService;
import com.kbait.anchack.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin-dongs")
public class AdminDongController {

    private final AdminDongService adminDongService;

    public AdminDongController(
        AdminDongService adminDongService
    ) {
        this.adminDongService = adminDongService;
    }

    /**
     * 구/행정동 이름으로 admin_dong_id를 조회한다.
     *
     * GET /api/admin-dongs?guName=은평구&dongName=증산동
     */
    @GetMapping
    public ResponseEntity<AdminDongResponse> getAdminDong(
        @RequestParam String guName,
        @RequestParam String dongName
    ) {
        AdminDongResponse response =
            adminDongService.getAdminDong(
                guName,
                dongName
            );

        return ResponseEntity.ok(response);
    }

    /**
     * 상세보기/비교 화면(P1-b)용 행정동 고정 정보 배치 조회.
     *
     * GET /api/admin-dongs/batch?ids=1,2,3,4,5
     */
    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<AdminDongDetailResponse>>> getAdminDongDetails(
        @RequestParam List<Long> ids
    ) {
        List<AdminDongDetailResponse> response = adminDongService.getAdminDongDetails(ids);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package com.kbait.anchack.admindong.controller;

import com.kbait.anchack.admindong.dto.response.AdminDongResponse;
import com.kbait.anchack.admindong.service.AdminDongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}

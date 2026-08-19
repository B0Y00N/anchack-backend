package com.kbait.anchack.rental.resolver;

import java.util.Objects;

public final class RentalAdminDongResolution {

    public enum Status {
        MAPPED,
        JIBUN_MISSING,
        ADDRESS_NOT_FOUND,
        BOUNDARY_NOT_FOUND,
        ADMIN_DONG_NOT_FOUND
    }

    private final Long adminDongId;
    private final Status status;

    private RentalAdminDongResolution(Long adminDongId, Status status) {
        this.adminDongId = adminDongId;
        this.status = Objects.requireNonNull(status, "status는 null일 수 없습니다.");
    }

    public static RentalAdminDongResolution mapped(Long adminDongId) {
        return new RentalAdminDongResolution(
                Objects.requireNonNull(adminDongId, "adminDongId는 null일 수 없습니다."),
                Status.MAPPED
        );
    }

    public static RentalAdminDongResolution unmapped(Status status) {
        if (status == Status.MAPPED) {
            throw new IllegalArgumentException("MAPPED 상태에는 adminDongId가 필요합니다.");
        }
        return new RentalAdminDongResolution(null, status);
    }

    public Long getAdminDongId() {
        return adminDongId;
    }

    public Status getStatus() {
        return status;
    }
}

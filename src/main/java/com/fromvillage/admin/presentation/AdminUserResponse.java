package com.fromvillage.admin.presentation;

import com.fromvillage.admin.application.AdminUserSummary;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long userId,
        String email,
        String nickname,
        String role,
        LocalDateTime sellerApprovedAt,
        LocalDateTime createdAt
) {

    public static AdminUserResponse from(AdminUserSummary summary) {
        return new AdminUserResponse(
                summary.userId(),
                summary.email(),
                summary.nickname(),
                summary.role(),
                summary.sellerApprovedAt(),
                summary.createdAt()
        );
    }
}

package com.fromvillage.admin.application;

import com.fromvillage.user.domain.User;

import java.time.LocalDateTime;

public record AdminUserSummary(
        Long userId,
        String email,
        String nickname,
        String role,
        LocalDateTime sellerApprovedAt,
        LocalDateTime createdAt
) {

    public static AdminUserSummary from(User user) {
        return new AdminUserSummary(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole().name(),
                user.getSellerApprovedAt(),
                user.getCreatedAt()
        );
    }
}

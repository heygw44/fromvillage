package com.fromvillage.admin.domain;

import java.time.LocalDateTime;

public record AdminUserSummary(
        Long userId,
        String email,
        String nickname,
        String role,
        LocalDateTime sellerApprovedAt,
        LocalDateTime createdAt
) {
}

package com.fromvillage.admin.presentation;

import com.fromvillage.admin.application.AdminUserPage;

import java.util.List;

public record AdminUserPageResponse(
        List<AdminUserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static AdminUserPageResponse from(AdminUserPage page) {
        return new AdminUserPageResponse(
                page.content().stream()
                        .map(AdminUserResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }
}

package com.fromvillage.admin.application;

import com.fromvillage.admin.domain.AdminUserSummary;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserPage(
        List<AdminUserSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static AdminUserPage from(Page<AdminUserSummary> page) {
        return new AdminUserPage(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}

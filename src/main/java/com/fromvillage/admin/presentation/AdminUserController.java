package com.fromvillage.admin.presentation;

import com.fromvillage.admin.application.AdminSellerRoleService;
import com.fromvillage.admin.application.AdminUserPage;
import com.fromvillage.admin.application.AdminUserQueryService;
import com.fromvillage.admin.application.AdminUserSummary;
import com.fromvillage.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserQueryService adminUserQueryService;
    private final AdminSellerRoleService adminSellerRoleService;

    @GetMapping
    public ApiResponse<AdminUserPageResponse> getUsers(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        AdminUserPage page = adminUserQueryService.getUsers(pageable);
        return ApiResponse.success(AdminUserPageResponse.from(page));
    }

    @PostMapping("/{userId}/seller-role")
    public ApiResponse<AdminUserResponse> approveSellerRole(@PathVariable Long userId) {
        AdminUserSummary summary = adminSellerRoleService.approveSellerRole(userId);
        return ApiResponse.success(AdminUserResponse.from(summary));
    }
}

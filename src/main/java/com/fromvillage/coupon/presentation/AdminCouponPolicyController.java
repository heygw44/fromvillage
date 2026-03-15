package com.fromvillage.coupon.presentation;

import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.common.security.AuthenticatedUser;
import com.fromvillage.coupon.application.AdminCouponPolicyResult;
import com.fromvillage.coupon.application.AdminCouponPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/coupon-policies")
public class AdminCouponPolicyController {

    private final AdminCouponPolicyService adminCouponPolicyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminCouponPolicyResponse> createCouponPolicy(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody AdminCouponPolicyCreateRequest request
    ) {
        AdminCouponPolicyResult result = adminCouponPolicyService.createCouponPolicy(
                authenticatedUser.getUserId(),
                request.toCommand()
        );
        return ApiResponse.success(AdminCouponPolicyResponse.from(result));
    }

    @PostMapping("/{couponPolicyId}/open")
    public ApiResponse<AdminCouponPolicyResponse> openCouponPolicy(@PathVariable Long couponPolicyId) {
        return ApiResponse.success(AdminCouponPolicyResponse.from(
                adminCouponPolicyService.openCouponPolicy(couponPolicyId)
        ));
    }

    @PostMapping("/{couponPolicyId}/close")
    public ApiResponse<AdminCouponPolicyResponse> closeCouponPolicy(@PathVariable Long couponPolicyId) {
        return ApiResponse.success(AdminCouponPolicyResponse.from(
                adminCouponPolicyService.closeCouponPolicy(couponPolicyId)
        ));
    }
}

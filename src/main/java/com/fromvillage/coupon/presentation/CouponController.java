package com.fromvillage.coupon.presentation;

import com.fromvillage.common.response.ApiResponse;
import com.fromvillage.common.security.AuthenticatedUser;
import com.fromvillage.coupon.application.CouponIssueResult;
import com.fromvillage.coupon.application.CouponIssueService;
import com.fromvillage.coupon.application.CouponQueryResult;
import com.fromvillage.coupon.application.CouponQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponIssueService couponIssueService;
    private final CouponQueryService couponQueryService;

    @PostMapping("/{couponPolicyId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponIssueResponse> issueCoupon(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long couponPolicyId
    ) {
        CouponIssueResult result = couponIssueService.issue(authenticatedUser.getUserId(), couponPolicyId);
        return ApiResponse.success(CouponIssueResponse.from(result));
    }

    @GetMapping("/me")
    public ApiResponse<CouponQueryResponse> getMyCoupons(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        CouponQueryResult result = couponQueryService.getMyCoupons(authenticatedUser.getUserId());
        return ApiResponse.success(CouponQueryResponse.from(result));
    }
}

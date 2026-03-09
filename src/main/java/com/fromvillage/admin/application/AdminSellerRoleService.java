package com.fromvillage.admin.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminSellerRoleService {

    private final UserStore userStore;
    private final Clock clock;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdminUserSummary approveSellerRole(Long userId) {
        User user = userStore.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.approveSeller(LocalDateTime.now(clock));
        User savedUser = userStore.save(user);
        return AdminUserSummary.from(savedUser);
    }
}

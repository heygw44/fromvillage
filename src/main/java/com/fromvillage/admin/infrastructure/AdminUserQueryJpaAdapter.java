package com.fromvillage.admin.infrastructure;

import com.fromvillage.admin.application.AdminUserSummary;
import com.fromvillage.admin.domain.AdminUserQueryPort;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserQueryJpaAdapter implements AdminUserQueryPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Page<AdminUserSummary> findUsers(Pageable pageable) {
        return userJpaRepository.findAll(pageable)
                .map(AdminUserSummary::from);
    }
}

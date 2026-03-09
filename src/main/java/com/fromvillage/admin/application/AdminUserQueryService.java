package com.fromvillage.admin.application;

import com.fromvillage.admin.domain.AdminUserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserQueryService {

    private final AdminUserQueryPort adminUserQueryPort;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AdminUserPage getUsers(Pageable pageable) {
        return AdminUserPage.from(adminUserQueryPort.findUsers(pageable));
    }
}

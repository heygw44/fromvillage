package com.fromvillage.admin.domain;

import com.fromvillage.admin.application.AdminUserSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserQueryPort {

    Page<AdminUserSummary> findUsers(Pageable pageable);
}

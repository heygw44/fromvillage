package com.fromvillage.admin.infrastructure;

import com.fromvillage.common.config.JpaAuditingConfig;
import com.fromvillage.support.TestContainersConfig;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.infrastructure.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, JpaAuditingConfig.class, AdminUserQueryJpaAdapter.class})
class AdminUserQueryJpaAdapterTest {

    @Autowired
    private AdminUserQueryJpaAdapter adminUserQueryJpaAdapter;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    @DisplayName("관리자 회원 조회 어댑터는 페이지와 기본 필드를 매핑한다")
    void findUsersReturnsPagedSummaries() {
        User admin = userJpaRepository.saveAndFlush(User.createAdmin(
                "admin@example.com",
                "encoded-password",
                "운영자"
        ));
        User user = userJpaRepository.saveAndFlush(User.createUser(
                "user@example.com",
                "encoded-password",
                "일반회원"
        ));

        var result = adminUserQueryJpaAdapter.findUsers(PageRequest.of(0, 10, Sort.by("createdAt").ascending()));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).userId()).isEqualTo(admin.getId());
        assertThat(result.getContent().get(0).email()).isEqualTo("admin@example.com");
        assertThat(result.getContent().get(0).role()).isEqualTo("ADMIN");
        assertThat(result.getContent().get(1).userId()).isEqualTo(user.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}

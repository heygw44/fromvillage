package com.fromvillage.admin.application;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.admin.domain.AdminUserSummary;
import com.fromvillage.user.domain.User;
import com.fromvillage.user.domain.UserStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminSellerRoleServiceTest {

    @Mock
    private UserStore userStore;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-09T00:00:00Z"), ZoneOffset.UTC);

    private AdminSellerRoleService adminSellerRoleService;

    @BeforeEach
    void setUp() {
        adminSellerRoleService = new AdminSellerRoleService(userStore, clock);
    }

    @Test
    @DisplayName("관리자가 USER를 SELLER로 승인하면 승인 시각을 기록한 응답을 반환한다")
    void approveSellerRole() {
        User user = User.createUser("user@example.com", "encoded-password", "일반회원");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userStore.findById(1L)).willReturn(Optional.of(user));
        given(userStore.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        AdminUserSummary result = adminSellerRoleService.approveSellerRole(1L);

        verify(userStore).save(user);
        assertThat(user.getRole().name()).isEqualTo("SELLER");
        assertThat(user.getSellerApprovedAt()).isEqualTo(LocalDateTime.of(2026, 3, 9, 0, 0));
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.role()).isEqualTo("SELLER");
        assertThat(result.sellerApprovedAt()).isEqualTo(LocalDateTime.of(2026, 3, 9, 0, 0));
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 USER_NOT_FOUND 예외를 반환한다")
    void approveSellerRoleRejectsMissingUser() {
        given(userStore.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminSellerRoleService.approveSellerRole(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}

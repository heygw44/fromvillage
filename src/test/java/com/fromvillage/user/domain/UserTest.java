package com.fromvillage.user.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("일반 회원 생성 시 기본 역할은 USER다")
    void createUserUsesUserRole() {
        User user = User.createUser("user@example.com", "encoded-password", "nickname");

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getSellerApprovedAt()).isNull();
    }

    @Test
    @DisplayName("일반 회원 생성 시 null 인자를 허용하지 않는다")
    void createUserRejectsNullArguments() {
        assertThatThrownBy(() -> User.createUser(null, "encoded-password", "nickname"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> User.createUser("user@example.com", null, "nickname"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> User.createUser("user@example.com", "encoded-password", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("USER만 SELLER로 전환할 수 있다")
    void approveSellerOnlyAllowsUserRole() {
        User user = User.createUser("user@example.com", "encoded-password", "nickname");
        LocalDateTime approvedAt = LocalDateTime.of(2026, 3, 8, 12, 0);

        user.approveSeller(approvedAt);

        assertThat(user.getRole()).isEqualTo(UserRole.SELLER);
        assertThat(user.getSellerApprovedAt()).isEqualTo(approvedAt);
    }

    @Test
    @DisplayName("이미 SELLER인 사용자는 다시 승인할 수 없다")
    void approveSellerRejectsAlreadySeller() {
        User user = User.createUser("user@example.com", "encoded-password", "nickname");
        user.approveSeller(LocalDateTime.of(2026, 3, 8, 12, 0));

        assertThatThrownBy(() -> user.approveSeller(LocalDateTime.of(2026, 3, 8, 13, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_SELLER);
    }

    @Test
    @DisplayName("ADMIN은 SELLER로 전환할 수 없다")
    void approveSellerRejectsAdmin() {
        User user = User.createAdmin("admin@example.com", "encoded-password", "admin");

        assertThatThrownBy(() -> user.approveSeller(LocalDateTime.of(2026, 3, 8, 12, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SELLER_APPROVAL_NOT_ALLOWED);
    }
}

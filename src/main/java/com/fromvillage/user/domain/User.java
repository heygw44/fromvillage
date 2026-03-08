package com.fromvillage.user.domain;

import com.fromvillage.common.exception.BusinessException;
import com.fromvillage.common.exception.ErrorCode;
import com.fromvillage.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Getter
    @Column(nullable = false, length = 50)
    private String nickname;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Getter
    @Column(name = "seller_approved_at")
    private LocalDateTime sellerApprovedAt;

    private User(String email, String password, String nickname, UserRole role) {
        this.email = Objects.requireNonNull(email);
        this.password = Objects.requireNonNull(password);
        this.nickname = Objects.requireNonNull(nickname);
        this.role = Objects.requireNonNull(role);
    }

    public static User createUser(String email, String password, String nickname) {
        return new User(email, password, nickname, UserRole.USER);
    }

    public static User createAdmin(String email, String password, String nickname) {
        return new User(email, password, nickname, UserRole.ADMIN);
    }

    public void approveSeller(LocalDateTime approvedAt) {
        if (this.role == UserRole.SELLER) {
            throw new BusinessException(ErrorCode.USER_ALREADY_SELLER);
        }
        if (this.role != UserRole.USER) {
            throw new BusinessException(ErrorCode.SELLER_APPROVAL_NOT_ALLOWED);
        }
        this.role = UserRole.SELLER;
        this.sellerApprovedAt = Objects.requireNonNull(approvedAt);
    }

    public String getPasswordHash() {
        return password;
    }
}

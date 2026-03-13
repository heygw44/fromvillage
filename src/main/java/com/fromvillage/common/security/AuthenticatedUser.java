package com.fromvillage.common.security;

import com.fromvillage.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String nickname;
    private final String role;
    private final List<GrantedAuthority> authorities;

    private AuthenticatedUser(
            Long userId,
            String username,
            String password,
            String nickname,
            String role,
            List<GrantedAuthority> authorities
    ) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.authorities = List.copyOf(authorities);
    }

    public static AuthenticatedUser from(User user) {
        String role = user.getRole().name();
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getNickname(),
                role,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        // 현재 범위에서는 계정 만료 주기를 따로 두지 않는다.
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 로그인 잠금은 별도 마일스톤에서 다루므로, 지금은 인증 주체에 잠금 상태를 싣지 않는다.
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호 만료 정책은 현재 범위에 포함하지 않는다.
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 현재 범위에서는 회원 비활성화나 탈퇴를 구현하지 않아 저장된 회원을 모두 활성 상태로 본다.
        return true;
    }
}

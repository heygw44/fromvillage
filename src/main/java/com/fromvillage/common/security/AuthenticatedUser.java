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
        // MVP currently has no account expiration lifecycle in the User aggregate.
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Login lock is introduced in a later milestone, so principal-level lock state is not modeled yet.
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Password expiration is outside the current MVP scope.
        return true;
    }

    @Override
    public boolean isEnabled() {
        // User deactivation/withdrawal is not implemented in the current MVP, so all persisted users are enabled.
        return true;
    }
}

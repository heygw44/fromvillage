package com.fromvillage.common.security;

import org.springframework.security.core.AuthenticationException;

public class LoginTemporarilyLockedAuthenticationException extends AuthenticationException {

    public LoginTemporarilyLockedAuthenticationException() {
        super("로그인 시도가 잠시 제한되었습니다.");
    }
}

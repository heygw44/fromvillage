package com.fromvillage.user.domain;

public interface UserStore {

    boolean existsByEmail(String email);

    User save(User user);
}

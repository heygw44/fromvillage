package com.fromvillage.user.domain;

import java.util.Optional;

public interface UserStore {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    User save(User user);
}

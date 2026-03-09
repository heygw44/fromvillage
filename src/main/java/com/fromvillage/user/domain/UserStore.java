package com.fromvillage.user.domain;

import java.util.Optional;

public interface UserStore {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    User save(User user);
}

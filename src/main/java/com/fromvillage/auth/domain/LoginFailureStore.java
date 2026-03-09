package com.fromvillage.auth.domain;

import java.time.Duration;
import java.util.Optional;

public interface LoginFailureStore {

    Optional<LoginFailureState> find(String normalizedEmail);

    void save(String normalizedEmail, LoginFailureState state, Duration ttl);

    void delete(String normalizedEmail);
}

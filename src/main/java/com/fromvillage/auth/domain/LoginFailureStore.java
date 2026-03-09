package com.fromvillage.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface LoginFailureStore {

    Optional<LoginFailureState> find(String normalizedEmail);

    LoginFailureState recordFailure(String normalizedEmail, Instant now, int maxFailures, Duration lockDuration);

    void delete(String normalizedEmail);
}

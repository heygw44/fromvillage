package com.fromvillage.auth.application;

import com.fromvillage.auth.domain.LoginFailureState;
import com.fromvillage.auth.domain.LoginFailureStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LoginFailurePolicyService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final LoginFailureStore loginFailureStore;
    private final Clock clock;

    public boolean isLocked(String email) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail == null) {
            return false;
        }

        Instant now = Instant.now(clock);
        return loginFailureStore.find(normalizedEmail)
                .filter(state -> state.isLocked(now))
                .isPresent();
    }

    public boolean recordFailure(String email) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail == null) {
            return false;
        }

        Instant now = Instant.now(clock);
        LoginFailureState updatedState = loginFailureStore.recordFailure(normalizedEmail, now, MAX_FAILURES, LOCK_DURATION);
        return updatedState.isLocked(now);
    }

    public void clear(String email) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail == null) {
            return;
        }
        loginFailureStore.delete(normalizedEmail);
    }

    private String normalize(String email) {
        if (email == null) {
            return null;
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}

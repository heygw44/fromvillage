package com.fromvillage.auth.application;

import com.fromvillage.auth.domain.LoginFailureState;
import com.fromvillage.auth.domain.LoginFailureStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LoginFailurePolicyServiceTest {

    @Test
    @DisplayName("잠금 시간이 지난 뒤의 첫 실패는 다시 1회부터 시작한다")
    void expiredLockStartsFailureCountFromOneAgain() {
        InMemoryLoginFailureStore store = new InMemoryLoginFailureStore();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T00:00:00Z"));
        LoginFailurePolicyService service = new LoginFailurePolicyService(store, clock);

        for (int attempt = 1; attempt <= 5; attempt++) {
            service.recordFailure("user@example.com");
        }

        clock.advance(Duration.ofMinutes(10).plusSeconds(1));

        boolean locked = service.recordFailure("user@example.com");

        assertThat(locked).isFalse();
        assertThat(store.find("user@example.com"))
                .contains(new LoginFailureState(1, null));
    }

    private static final class InMemoryLoginFailureStore implements LoginFailureStore {

        private final Map<String, LoginFailureState> states = new HashMap<>();

        @Override
        public Optional<LoginFailureState> find(String normalizedEmail) {
            return Optional.ofNullable(states.get(normalizedEmail));
        }

        @Override
        public void save(String normalizedEmail, LoginFailureState state, Duration ttl) {
            states.put(normalizedEmail, state);
        }

        @Override
        public void delete(String normalizedEmail) {
            states.remove(normalizedEmail);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant currentInstant;

        private MutableClock(Instant currentInstant) {
            this.currentInstant = currentInstant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }

        private void advance(Duration duration) {
            currentInstant = currentInstant.plus(duration);
        }
    }
}

package com.fromvillage.auth.application;

import com.fromvillage.auth.domain.LoginFailureState;
import com.fromvillage.auth.domain.LoginFailureStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LoginFailurePolicyServiceTest {

    @Test
    @DisplayName("1회부터 4회까지는 잠금되지 않고 5회째에 잠금된다")
    void locksOnFifthFailure() {
        InMemoryLoginFailureStore store = new InMemoryLoginFailureStore();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T00:00:00Z"));
        LoginFailurePolicyService service = new LoginFailurePolicyService(store, clock);

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThat(service.recordFailure("user@example.com")).isFalse();
            assertThat(service.isLocked("user@example.com")).isFalse();
        }

        assertThat(service.recordFailure("user@example.com")).isTrue();
        assertThat(service.isLocked("user@example.com")).isTrue();
        assertThat(store.find("user@example.com")).hasValueSatisfying(state -> {
            assertThat(state.failedCount()).isEqualTo(5);
            assertThat(state.lockedUntil()).isEqualTo(Instant.parse("2026-03-09T00:10:00Z"));
        });
    }

    @Test
    @DisplayName("로그인 성공 시 실패 상태를 초기화한다")
    void clearRemovesFailureState() {
        InMemoryLoginFailureStore store = new InMemoryLoginFailureStore();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T00:00:00Z"));
        LoginFailurePolicyService service = new LoginFailurePolicyService(store, clock);

        service.recordFailure("user@example.com");
        service.recordFailure("user@example.com");

        service.clear("user@example.com");

        assertThat(store.find("user@example.com")).isEmpty();
        assertThat(service.isLocked("user@example.com")).isFalse();
    }

    @Test
    @DisplayName("이메일이 null 이거나 비어 있으면 안전하게 무시한다")
    void ignoresNullOrBlankEmail() {
        InMemoryLoginFailureStore store = new InMemoryLoginFailureStore();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T00:00:00Z"));
        LoginFailurePolicyService service = new LoginFailurePolicyService(store, clock);

        assertThat(service.recordFailure(null)).isFalse();
        assertThat(service.recordFailure("   ")).isFalse();
        assertThat(service.isLocked(null)).isFalse();
        assertThat(service.isLocked("   ")).isFalse();

        service.clear(null);
        service.clear("   ");

        assertThat(store.states()).isEmpty();
    }

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

    @Test
    @DisplayName("실패 기록은 순차 호출마다 누적된다")
    void sequentialFailuresAccumulate() {
        InMemoryLoginFailureStore store = new InMemoryLoginFailureStore();
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T00:00:00Z"));
        LoginFailurePolicyService service = new LoginFailurePolicyService(store, clock);

        assertThat(service.recordFailure("user@example.com")).isFalse();
        assertThat(service.recordFailure("user@example.com")).isFalse();

        assertThat(store.find("user@example.com"))
                .hasValue(new LoginFailureState(2, null));
    }

    @Test
    @DisplayName("MutableClock.withZone 은 전달된 zone 을 반영한 새 Clock 을 반환한다")
    void mutableClockWithZoneReturnsClockForRequestedZone() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T00:00:00Z"));

        Clock seoulClock = clock.withZone(ZoneId.of("Asia/Seoul"));

        assertThat(seoulClock).isNotSameAs(clock);
        assertThat(seoulClock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
        assertThat(seoulClock.instant()).isEqualTo(clock.instant());
    }

    private static final class InMemoryLoginFailureStore implements LoginFailureStore {

        private final Map<String, LoginFailureState> states = new HashMap<>();

        @Override
        public Optional<LoginFailureState> find(String normalizedEmail) {
            return Optional.ofNullable(states.get(normalizedEmail));
        }

        @Override
        public LoginFailureState recordFailure(String normalizedEmail, Instant now, int maxFailures, Duration lockDuration) {
            LoginFailureState currentState = states.get(normalizedEmail);
            if (currentState != null && !currentState.isLocked(now) && currentState.lockedUntil() != null) {
                currentState = null;
            }

            int nextFailureCount = currentState == null ? 1 : currentState.failedCount() + 1;
            Instant lockedUntil = nextFailureCount >= maxFailures ? now.plus(lockDuration) : null;
            LoginFailureState nextState = new LoginFailureState(nextFailureCount, lockedUntil);
            states.put(normalizedEmail, nextState);
            return nextState;
        }

        @Override
        public void delete(String normalizedEmail) {
            states.remove(normalizedEmail);
        }

        private Map<String, LoginFailureState> states() {
            return states;
        }
    }

    private static final class MutableClock extends Clock {

        private Instant currentInstant;
        private final ZoneId zone;

        private MutableClock(Instant currentInstant) {
            this(currentInstant, ZoneOffset.UTC);
        }

        private MutableClock(Instant currentInstant, ZoneId zone) {
            this.currentInstant = currentInstant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
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

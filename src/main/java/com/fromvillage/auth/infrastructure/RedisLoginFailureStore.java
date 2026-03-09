package com.fromvillage.auth.infrastructure;

import com.fromvillage.auth.domain.LoginFailureState;
import com.fromvillage.auth.domain.LoginFailureStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisLoginFailureStore implements LoginFailureStore {

    private static final String KEY_PREFIX = "fromvillage:auth:login-failure:";
    private static final String FAILED_COUNT_FIELD = "failedCount";
    private static final String LOCKED_UNTIL_FIELD = "lockedUntilEpochMilli";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Optional<LoginFailureState> find(String normalizedEmail) {
        String key = key(normalizedEmail);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        int failedCount = Integer.parseInt(String.valueOf(entries.getOrDefault(FAILED_COUNT_FIELD, "0")));
        Object lockedUntilValue = entries.get(LOCKED_UNTIL_FIELD);
        Instant lockedUntil = lockedUntilValue == null || String.valueOf(lockedUntilValue).isBlank()
                ? null
                : Instant.ofEpochMilli(Long.parseLong(String.valueOf(lockedUntilValue)));

        return Optional.of(new LoginFailureState(failedCount, lockedUntil));
    }

    @Override
    public void save(String normalizedEmail, LoginFailureState state, Duration ttl) {
        String key = key(normalizedEmail);
        stringRedisTemplate.opsForHash().putAll(key, Map.of(
                FAILED_COUNT_FIELD, Integer.toString(state.failedCount()),
                LOCKED_UNTIL_FIELD, state.lockedUntil() == null ? "" : Long.toString(state.lockedUntil().toEpochMilli())
        ));

        if (ttl == null) {
            stringRedisTemplate.persist(key);
            return;
        }

        stringRedisTemplate.expire(key, ttl);
    }

    @Override
    public void delete(String normalizedEmail) {
        stringRedisTemplate.delete(key(normalizedEmail));
    }

    private String key(String normalizedEmail) {
        return KEY_PREFIX + normalizedEmail;
    }
}

package com.fromvillage.auth.infrastructure;

import com.fromvillage.auth.domain.LoginFailureState;
import com.fromvillage.auth.domain.LoginFailureStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisLoginFailureStore implements LoginFailureStore {

    private static final String KEY_PREFIX = "fromvillage:auth:login-failure:";
    private static final String FAILED_COUNT_FIELD = "failedCount";
    private static final String LOCKED_UNTIL_FIELD = "lockedUntilEpochMilli";
    private static final DefaultRedisScript<List> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>(
            """
                    local key = KEYS[1]
                    local failedCountField = ARGV[1]
                    local lockedUntilField = ARGV[2]
                    local nowEpochMilli = tonumber(ARGV[3])
                    local maxFailures = tonumber(ARGV[4])
                    local lockDurationMillis = tonumber(ARGV[5])

                    local failedCount = redis.call('HINCRBY', key, failedCountField, 1)

                    if failedCount >= maxFailures then
                        local lockedUntil = nowEpochMilli + lockDurationMillis
                        redis.call('HSET', key, lockedUntilField, lockedUntil)
                        redis.call('PEXPIRE', key, lockDurationMillis)
                        return { tostring(failedCount), tostring(lockedUntil) }
                    end

                    redis.call('HDEL', key, lockedUntilField)
                    redis.call('PERSIST', key)
                    return { tostring(failedCount), '' }
                    """,
            List.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Optional<LoginFailureState> find(String normalizedEmail) {
        String key = key(normalizedEmail);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        int failedCount = parseFailedCount(entries.get(FAILED_COUNT_FIELD));
        Instant lockedUntil = parseLockedUntil(entries.get(LOCKED_UNTIL_FIELD));

        return Optional.of(new LoginFailureState(failedCount, lockedUntil));
    }

    @Override
    public LoginFailureState recordFailure(String normalizedEmail, Instant now, int maxFailures, Duration lockDuration) {
        String key = key(normalizedEmail);
        List<?> result = stringRedisTemplate.execute(
                RECORD_FAILURE_SCRIPT,
                Collections.singletonList(key),
                FAILED_COUNT_FIELD,
                LOCKED_UNTIL_FIELD,
                Long.toString(now.toEpochMilli()),
                Integer.toString(maxFailures),
                Long.toString(lockDuration.toMillis())
        );
        if (result == null || result.size() < 2) {
            throw new IllegalStateException("로그인 실패 상태를 Redis에 기록하지 못했습니다.");
        }

        int failedCount = parseFailedCount(result.get(0));
        Instant lockedUntil = parseLockedUntil(result.get(1));
        return new LoginFailureState(failedCount, lockedUntil);
    }

    @Override
    public void delete(String normalizedEmail) {
        stringRedisTemplate.delete(key(normalizedEmail));
    }

    private String key(String normalizedEmail) {
        return KEY_PREFIX + normalizedEmail;
    }

    private int parseFailedCount(Object rawValue) {
        if (rawValue == null) {
            return 0;
        }

        try {
            return Integer.parseInt(String.valueOf(rawValue));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private Instant parseLockedUntil(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        String value = String.valueOf(rawValue);
        if (value.isBlank()) {
            return null;
        }

        try {
            return Instant.ofEpochMilli(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

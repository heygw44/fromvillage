package com.fromvillage.auth.infrastructure;

import com.fromvillage.auth.domain.LoginFailureState;
import com.fromvillage.support.TestContainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class RedisLoginFailureStoreIntegrationTest {

    private static final String KEY_PREFIX = "fromvillage:auth:login-failure:";

    @Autowired
    private RedisLoginFailureStore redisLoginFailureStore;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("손상된 failedCount 값이 있어도 실패 기록을 안전하게 복구한다")
    void recordFailureRepairsMalformedFailedCountValue() {
        String key = KEY_PREFIX + "user@example.com";
        stringRedisTemplate.opsForHash().put(key, "failedCount", "broken");
        stringRedisTemplate.opsForHash().put(key, "lockedUntilEpochMilli", "broken");

        LoginFailureState state = redisLoginFailureStore.recordFailure(
                "user@example.com",
                Instant.parse("2026-03-09T00:00:00Z"),
                5,
                Duration.ofMinutes(10)
        );

        assertThat(state).isEqualTo(new LoginFailureState(1, null));
        assertThat(redisLoginFailureStore.find("user@example.com"))
                .hasValue(new LoginFailureState(1, null));
    }

    @Test
    @DisplayName("실제 Redis 에서 동시에 실패를 기록해도 누락 없이 누적된다")
    void concurrentFailuresAreCountedWithoutLossInRedis() throws Exception {
        int concurrency = 8;
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(concurrency)) {
            List<Future<LoginFailureState>> futures = new ArrayList<>();
            for (int index = 0; index < concurrency; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return redisLoginFailureStore.recordFailure(
                            "user@example.com",
                            Instant.parse("2026-03-09T00:00:00Z"),
                            5,
                            Duration.ofMinutes(10)
                    );
                }));
            }

            ready.await();
            start.countDown();

            for (Future<LoginFailureState> future : futures) {
                future.get();
            }
        }

        assertThat(redisLoginFailureStore.find("user@example.com"))
                .hasValueSatisfying(state -> {
                    assertThat(state.failedCount()).isEqualTo(concurrency);
                    assertThat(state.lockedUntil()).isEqualTo(Instant.parse("2026-03-09T00:10:00Z"));
                });
    }
}

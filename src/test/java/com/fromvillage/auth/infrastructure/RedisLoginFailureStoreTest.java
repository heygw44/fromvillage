package com.fromvillage.auth.infrastructure;

import com.fromvillage.auth.domain.LoginFailureState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RedisLoginFailureStoreTest {

    @Test
    @DisplayName("Redis 값이 손상되어도 안전한 기본값으로 읽는다")
    void findFallsBackToSafeDefaultsWhenStoredValuesAreMalformed() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries("fromvillage:auth:login-failure:user@example.com"))
                .willReturn(Map.of(
                        "failedCount", "not-a-number",
                        "lockedUntilEpochMilli", "invalid-epoch"
                ));

        RedisLoginFailureStore store = new RedisLoginFailureStore(redisTemplate);

        assertThat(store.find("user@example.com"))
                .hasValue(new LoginFailureState(0, null));
    }
}

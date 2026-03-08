package com.fromvillage.common.config;

import com.fromvillage.support.TestContainersConfig;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class RedisSessionIntegrationTest {

    private static final String SESSION_VALUE_ATTRIBUTE = "sessionValue";
    private static final String SESSION_KEY_PREFIX = "fromvillage:session:sessions:";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SessionRepository<?> sessionRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private Filter springSessionRepositoryFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Set<String> sessionKeys = stringRedisTemplate.keys("fromvillage:session:*");
        if (sessionKeys != null && !sessionKeys.isEmpty()) {
            stringRedisTemplate.delete(sessionKeys);
        }

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSessionRepositoryFilter)
                .build();
    }

    @Test
    @DisplayName("세션 데이터는 Redis에 저장되고 같은 세션 쿠키로 다시 조회할 수 있다")
    void sessionIsStoredInRedisAndCanBeReadBackWithSameCookie() throws Exception {
        var setResult = mockMvc.perform(get("/test/session/value/fromvillage"))
                .andExpect(status().isOk())
                .andExpect(content().string("fromvillage"))
                .andReturn();

        var sessionCookie = setResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        mockMvc.perform(get("/test/session/value").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().string("fromvillage"));

        assertThat(sessionRepository).isInstanceOf(RedisIndexedSessionRepository.class);
        assertThat(sessionRepository).isInstanceOf(FindByIndexNameSessionRepository.class);

        Set<String> keys = stringRedisTemplate.keys(SESSION_KEY_PREFIX + "*");
        assertThat(keys).isNotNull();
        String sessionKey = keys.stream()
                .filter(key -> !key.contains(":expires:"))
                .findFirst()
                .orElseThrow();

        Long ttlSeconds = stringRedisTemplate.getExpire(sessionKey, TimeUnit.SECONDS);
        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isPositive();
        assertThat(ttlSeconds).isGreaterThanOrEqualTo(30 * 60);
        assertThat(ttlSeconds).isLessThanOrEqualTo(35 * 60);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SessionTestControllerConfig {

        @Bean
        SessionTestController sessionTestController() {
            return new SessionTestController();
        }
    }

    @RestController
    static class SessionTestController {

        @GetMapping("/test/session/value/{value}")
        ResponseEntity<String> setValue(@PathVariable String value, HttpSession session) {
            session.setAttribute(SESSION_VALUE_ATTRIBUTE, value);
            return ResponseEntity.ok(value);
        }

        @GetMapping("/test/session/value")
        ResponseEntity<String> getValue(HttpSession session) {
            Object value = session.getAttribute(SESSION_VALUE_ATTRIBUTE);
            return ResponseEntity.ok(value == null ? "missing" : value.toString());
        }
    }
}

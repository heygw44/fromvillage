package com.fromvillage.common.config;

import com.fromvillage.support.TestContainersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(TestContainersConfig.class)
class LocalSessionCookieIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("local 프로필에서는 세션 쿠키의 Secure가 false이고 나머지 기본 속성은 유지된다")
    void localProfileUsesInsecureSessionCookieOnlyForSecureFlag() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        var sessionCookie = result.getResponse().getCookie("SESSION");

        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.isHttpOnly()).isTrue();
        assertThat(sessionCookie.getSecure()).isFalse();
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(cookieHeader -> assertThat(cookieHeader).contains("SameSite=Lax"));
    }
}

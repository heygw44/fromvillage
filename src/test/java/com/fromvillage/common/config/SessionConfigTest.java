package com.fromvillage.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigTest {

    private final SessionConfig sessionConfig = new SessionConfig();

    @Test
    @DisplayName("sameSite가 비어 있으면 기본값으로 Lax를 사용한다")
    void cookieSerializerDefaultsBlankSameSiteToLax() throws Exception {
        DefaultCookieSerializer serializer =
                (DefaultCookieSerializer) sessionConfig.cookieSerializer(true, true, "   ");

        assertThat(readSameSite(serializer)).isEqualTo("Lax");
    }

    @Test
    @DisplayName("sameSite는 허용값만 대소문자 보정 후 허용한다")
    void cookieSerializerNormalizesAllowedSameSiteValues() throws Exception {
        DefaultCookieSerializer serializer =
                (DefaultCookieSerializer) sessionConfig.cookieSerializer(true, true, "strict");

        assertThat(readSameSite(serializer)).isEqualTo("Strict");
    }

    @Test
    @DisplayName("sameSite가 허용되지 않은 값이면 즉시 예외를 던진다")
    void cookieSerializerRejectsUnsupportedSameSiteValue() {
        assertThatThrownBy(() -> sessionConfig.cookieSerializer(true, true, "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same-site")
                .hasMessageContaining("invalid");
    }

    private String readSameSite(DefaultCookieSerializer serializer) throws Exception {
        Field sameSiteField = DefaultCookieSerializer.class.getDeclaredField("sameSite");
        sameSiteField.setAccessible(true);
        return (String) sameSiteField.get(serializer);
    }
}

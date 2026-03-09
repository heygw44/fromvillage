package com.fromvillage.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromvillage.auth.application.LoginFailurePolicyService;
import com.fromvillage.common.security.JsonAccessDeniedHandler;
import com.fromvillage.common.security.JsonAuthenticationEntryPoint;
import com.fromvillage.common.security.JsonInvalidSessionStrategy;
import com.fromvillage.common.security.JsonLoginAuthenticationFilter;
import com.fromvillage.common.security.JsonLoginFailureHandler;
import com.fromvillage.common.security.JsonLoginSuccessHandler;
import com.fromvillage.common.security.JsonLogoutSuccessHandler;
import com.fromvillage.common.security.JsonSessionInformationExpiredStrategy;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final int MAXIMUM_SESSIONS = 1;

    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final JsonInvalidSessionStrategy invalidSessionStrategy;
    private final JsonLoginSuccessHandler loginSuccessHandler;
    private final JsonLoginFailureHandler loginFailureHandler;
    private final JsonLogoutSuccessHandler logoutSuccessHandler;
    private final JsonSessionInformationExpiredStrategy sessionInformationExpiredStrategy;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final LoginFailurePolicyService loginFailurePolicyService;

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JsonLoginAuthenticationFilter jsonLoginAuthenticationFilter,
            SecurityContextRepository securityContextRepository,
            SessionRegistry sessionRegistry
    ) throws Exception {
        http
                .csrf(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/signup", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session
                        .invalidSessionStrategy(invalidSessionStrategy)
                        .maximumSessions(MAXIMUM_SESSIONS)
                        .maxSessionsPreventsLogin(false)
                        .expiredSessionStrategy(sessionInformationExpiredStrategy)
                        .sessionRegistry(sessionRegistry)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutSuccessHandler(logoutSuccessHandler)
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository)
                )
                .addFilterAt(jsonLoginAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    JsonLoginAuthenticationFilter jsonLoginAuthenticationFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy
    ) {
        JsonLoginAuthenticationFilter filter = new JsonLoginAuthenticationFilter(
                objectMapper,
                validator,
                loginFailurePolicyService
        );
        filter.setAuthenticationManager(authenticationManager);
        filter.setAuthenticationSuccessHandler(loginSuccessHandler);
        filter.setAuthenticationFailureHandler(loginFailureHandler);
        filter.setSecurityContextRepository(securityContextRepository);
        filter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
        return filter;
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
        ConcurrentSessionControlAuthenticationStrategy concurrentSessionControl =
                new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry);
        concurrentSessionControl.setMaximumSessions(MAXIMUM_SESSIONS);
        concurrentSessionControl.setExceptionIfMaximumExceeded(false);

        return new CompositeSessionAuthenticationStrategy(List.of(
                concurrentSessionControl,
                new ChangeSessionIdAuthenticationStrategy(),
                new RegisterSessionAuthenticationStrategy(sessionRegistry)
        ));
    }
}

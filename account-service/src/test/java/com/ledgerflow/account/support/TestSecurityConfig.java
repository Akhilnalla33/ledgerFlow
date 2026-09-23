package com.ledgerflow.account.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Supplies a {@link JwtDecoder} bean that satisfies {@code SecurityConfig}'s dependency
 * without Spring Boot's auto-configuration reaching out over the network to an issuer's
 * {@code .well-known/openid-configuration} endpoint at context startup (there is none in
 * tests). It is never actually invoked: tests that exercise services directly never go
 * through the security filter chain, and tests that do go through MockMvc authenticate via
 * {@code SecurityMockMvcRequestPostProcessors.jwt()} instead of a real bearer token.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            throw new UnsupportedOperationException("Tests authenticate via mock JWT post-processors, not real tokens");
        };
    }
}

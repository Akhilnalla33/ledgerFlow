package com.ledgerflow.account.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Active only under the {@code loadtest} Spring profile, used exclusively by
 * {@code scripts/loadtest/run-load-test.sh} to run k6 against a real, locally-running instance
 * of account-service without needing a live Keycloak/OAuth2 issuer just to mint load-test
 * tokens. Every request is treated as an already-authenticated {@code ROLE_ADMIN} principal
 * (note: plain {@code .anonymous()} is not enough — Spring Security's {@code isAuthenticated()}
 * SpEL expression is defined to be false for the anonymous principal even though its own
 * {@code isAuthenticated()} flag is true, so {@code @PreAuthorize("isAuthenticated()")} would
 * still reject it). This profile is never used in {@code docker-compose.yml} or any deployed
 * environment — the real {@link SecurityConfig} (JWT-required) is what runs everywhere else.
 */
@Configuration
@EnableMethodSecurity
@Profile("loadtest")
public class LoadTestSecurityConfig {

    @Bean
    public SecurityFilterChain loadTestFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new AlwaysAuthenticatedFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    private static final class AlwaysAuthenticatedFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            var authentication = new UsernamePasswordAuthenticationToken("loadtest-user", null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        }
    }
}

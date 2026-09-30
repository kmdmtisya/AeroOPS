package com.aeroops.config;

import com.aeroops.tenancy.TenantFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every API request must carry a valid OIDC-issued JWT (Keycloak). There is no
 * anonymous or session-based access, and no endpoint accepts a client-supplied
 * tenant id: see {@link TenantFilter}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${aeroops.tenant.claim}")
    private String tenantClaim;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
            .addFilterAfter(new TenantFilter(tenantClaim), BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}

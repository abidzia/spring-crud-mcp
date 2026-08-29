package com.example.productmcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Phase 1 (sub-PR #1): makes product-mcp an OAuth2 Resource Server.
 *
 * <p>One filter chain guards <b>both</b> surfaces — the REST API ({@code /api/**}) and the MCP
 * endpoint ({@code /mcp}) — because MCP Streamable HTTP is just HTTP. Every request must carry a
 * valid Bearer JWT; Spring Security's default converter maps the token's {@code scope} claim to
 * {@code SCOPE_*} authorities.
 *
 * <p>This is a <b>coarse, resource-level</b> gate: any request to this server needs one of the
 * catalog scopes (so a token scoped only for orders is rejected with 403). Fine-grained
 * read-vs-write enforcement lives in the service layer via {@code @PreAuthorize} (sub-PR #2), so a
 * single rule covers the REST controller and the MCP tools together and the model's tool choice
 * can never exceed the caller's scopes.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless bearer-token API: no server-side session, no CSRF tokens.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**", "/mcp", "/mcp/**")
                        .hasAnyAuthority("SCOPE_catalog:read", "SCOPE_catalog:write")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}

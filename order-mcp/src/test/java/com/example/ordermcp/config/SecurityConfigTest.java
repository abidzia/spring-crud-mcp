package com.example.ordermcp.config;

import com.example.ordermcp.controller.OrderController;
import com.example.ordermcp.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sub-PR #1 security matrix — the same coarse gate protects both surfaces of order-mcp:
 * no token → 401, valid token with the wrong scope → 403, right scope → 200.
 *
 * <p>The {@code jwt()} post-processor injects a pre-authenticated token, so the (mocked)
 * {@link JwtDecoder} is never exercised — these tests assert authorization, not token decoding.
 */
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService service;

    // Satisfies the resource-server filter's dependency without a live IdP; never invoked.
    @MockitoBean
    JwtDecoder jwtDecoder;

    private static SimpleGrantedAuthority scope(String scope) {
        return new SimpleGrantedAuthority("SCOPE_" + scope);
    }

    // ---- REST surface (/api/**) ----

    @Test
    void restWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restWithWrongScopeIsForbidden() throws Exception {
        mvc.perform(get("/api/orders").with(jwt().authorities(scope("catalog:read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void restWithOrdersScopeIsOk() throws Exception {
        mvc.perform(get("/api/orders").with(jwt().authorities(scope("orders:read"))))
                .andExpect(status().isOk());
    }

    // ---- MCP surface (/mcp) — guarded by the same filter chain ----

    @Test
    void mcpWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/mcp"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mcpWithWrongScopeIsForbidden() throws Exception {
        mvc.perform(post("/mcp").with(jwt().authorities(scope("catalog:write"))))
                .andExpect(status().isForbidden());
    }
}

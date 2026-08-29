package com.example.productmcp.config;

import com.example.productmcp.controller.ProductController;
import com.example.productmcp.service.ProductService;
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
 * Sub-PR #1 security matrix — the same coarse gate protects both surfaces of product-mcp:
 * no token → 401, valid token with the wrong scope → 403, right scope → 200.
 *
 * <p>The {@code jwt()} post-processor injects a pre-authenticated token, so the (mocked)
 * {@link JwtDecoder} is never exercised — these tests assert authorization, not token decoding.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService service;

    // Satisfies the resource-server filter's dependency without a live IdP; never invoked.
    @MockitoBean
    JwtDecoder jwtDecoder;

    private static SimpleGrantedAuthority scope(String scope) {
        return new SimpleGrantedAuthority("SCOPE_" + scope);
    }

    // ---- REST surface (/api/**) ----

    @Test
    void restWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restWithWrongScopeIsForbidden() throws Exception {
        mvc.perform(get("/api/products").with(jwt().authorities(scope("orders:read"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void restWithCatalogScopeIsOk() throws Exception {
        mvc.perform(get("/api/products").with(jwt().authorities(scope("catalog:read"))))
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
        mvc.perform(post("/mcp").with(jwt().authorities(scope("orders:write"))))
                .andExpect(status().isForbidden());
    }
}

package com.example.productmcp.controller;

import com.example.productmcp.config.SecurityConfig;
import com.example.productmcp.exception.ProductNotFoundException;
import com.example.productmcp.model.Product;
import com.example.productmcp.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService service;

    // Security is active on the slice now; satisfies the resource-server filter (never invoked).
    @MockitoBean
    JwtDecoder jwtDecoder;

    // These tests cover controller behaviour, not authorization — authenticate with the write
    // scope so every request clears the coarse catalog gate. The 401/403 matrix lives in
    // SecurityConfigTest.
    private static JwtRequestPostProcessor catalogWrite() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_catalog:write"));
    }

    private Product product(Long id, String name) {
        Product p = new Product(name, "desc", new BigDecimal("9.99"), 5);
        p.setId(id);
        return p;
    }

    @Test
    void listReturnsAllProducts() throws Exception {
        given(service.findAll()).willReturn(List.of(product(1L, "Mouse"), product(2L, "Keyboard")));

        mvc.perform(get("/api/products").with(catalogWrite()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Mouse"));
    }

    @Test
    void listWithNameParamSearchesByName() throws Exception {
        given(service.searchByName("mou")).willReturn(List.of(product(1L, "Mouse")));

        mvc.perform(get("/api/products").param("name", "mou").with(catalogWrite()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Mouse"));

        verify(service).searchByName("mou");
    }

    @Test
    void getByIdReturnsProduct() throws Exception {
        given(service.findById(1L)).willReturn(product(1L, "Mouse"));

        mvc.perform(get("/api/products/1").with(catalogWrite()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mouse"));
    }

    @Test
    void getByIdUnknownReturns404() throws Exception {
        given(service.findById(99L)).willThrow(new ProductNotFoundException(99L));

        mvc.perform(get("/api/products/99").with(catalogWrite()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        given(service.create(any(Product.class))).willReturn(product(1L, "Mouse"));

        mvc.perform(post("/api/products")
                        .with(catalogWrite())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mouse\",\"description\":\"desc\",\"price\":9.99,\"quantity\":5}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/products/1")))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturnsUpdatedProduct() throws Exception {
        given(service.update(eq(1L), any(Product.class))).willReturn(product(1L, "Updated"));

        mvc.perform(put("/api/products/1")
                        .with(catalogWrite())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"description\":\"desc\",\"price\":9.99,\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void createWithInvalidBodyReturns400() throws Exception {
        // blank name, negative price, negative quantity all violate validation
        mvc.perform(post("/api/products")
                        .with(catalogWrite())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1,\"quantity\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/products/1").with(catalogWrite()))
                .andExpect(status().isNoContent());

        verify(service).delete(1L);
    }
}

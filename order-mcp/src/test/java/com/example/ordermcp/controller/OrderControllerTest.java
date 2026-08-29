package com.example.ordermcp.controller;

import com.example.ordermcp.config.SecurityConfig;
import com.example.ordermcp.exception.OrderNotFoundException;
import com.example.ordermcp.model.Order;
import com.example.ordermcp.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService service;

    // Security is active on the slice now; satisfies the resource-server filter (never invoked).
    @MockitoBean
    JwtDecoder jwtDecoder;

    // The REST surface is read-only, so orders:read clears the coarse gate. The 401/403 matrix
    // lives in SecurityConfigTest.
    private static JwtRequestPostProcessor ordersRead() {
        return jwt().authorities(new SimpleGrantedAuthority("SCOPE_orders:read"));
    }

    private Order order(Long id, String customer) {
        Order o = new Order(customer, "Widget", 2, "NEW");
        o.setId(id);
        return o;
    }

    @Test
    void listReturnsAllOrders() throws Exception {
        given(service.findAll()).willReturn(List.of(order(1L, "Alice"), order(2L, "Bob")));

        mvc.perform(get("/api/orders").with(ordersRead()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customer").value("Alice"));
    }

    @Test
    void listWithCustomerParamSearchesByCustomer() throws Exception {
        given(service.searchByCustomer("ali")).willReturn(List.of(order(1L, "Alice")));

        mvc.perform(get("/api/orders").param("customer", "ali").with(ordersRead()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customer").value("Alice"));

        verify(service).searchByCustomer("ali");
    }

    @Test
    void getByIdReturnsOrder() throws Exception {
        given(service.findById(1L)).willReturn(order(1L, "Alice"));

        mvc.perform(get("/api/orders/1").with(ordersRead()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customer").value("Alice"));
    }

    @Test
    void getByIdUnknownReturns404() throws Exception {
        given(service.findById(99L)).willThrow(new OrderNotFoundException(99L));

        mvc.perform(get("/api/orders/99").with(ordersRead()))
                .andExpect(status().isNotFound());
    }
}

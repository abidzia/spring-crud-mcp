package com.example.ordermcp.controller;

import com.example.ordermcp.model.Order;
import com.example.ordermcp.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// NOTE: order-mcp has no GlobalExceptionHandler yet, so a not-found id currently
// surfaces as 500 rather than 404 — a Phase-0 hardening gap, deliberately not
// asserted here. Add the handler in the hardening branch, then a 404 test.
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService service;

    private Order order(Long id, String customer) {
        Order o = new Order(customer, "Widget", 2, "NEW");
        o.setId(id);
        return o;
    }

    @Test
    void listReturnsAllOrders() throws Exception {
        given(service.findAll()).willReturn(List.of(order(1L, "Alice"), order(2L, "Bob")));

        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customer").value("Alice"));
    }

    @Test
    void listWithCustomerParamSearchesByCustomer() throws Exception {
        given(service.searchByCustomer("ali")).willReturn(List.of(order(1L, "Alice")));

        mvc.perform(get("/api/orders").param("customer", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customer").value("Alice"));

        verify(service).searchByCustomer("ali");
    }

    @Test
    void getByIdReturnsOrder() throws Exception {
        given(service.findById(1L)).willReturn(order(1L, "Alice"));

        mvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customer").value("Alice"));
    }
}

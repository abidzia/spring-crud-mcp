package com.example.ordermcp.controller;

import com.example.ordermcp.model.Order;
import com.example.ordermcp.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Minimal REST surface — handy for verifying seed data with curl, and mirrors
 * the dual REST + MCP shape of product-crud-mcp.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<Order> list(@RequestParam(required = false) String customer) {
        if (customer != null && !customer.isBlank()) {
            return orderService.searchByCustomer(customer);
        }
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.findById(id);
    }
}

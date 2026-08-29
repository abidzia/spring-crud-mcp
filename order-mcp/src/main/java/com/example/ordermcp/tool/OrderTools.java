package com.example.ordermcp.tool;

import com.example.ordermcp.dto.OrderRequest;
import com.example.ordermcp.model.Order;
import com.example.ordermcp.service.OrderService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * MCP surface for orders. Tool names are domain-prefixed with "order" so they
 * never collide with another service's tools (e.g. product-mcp's list_products)
 * when both servers are connected to the same agent.
 */
@Component
public class OrderTools {

    private static final String ALLOWED_STATUS = "NEW|SHIPPED|CANCELLED";

    private final OrderService orderService;
    private final Validator validator;

    public OrderTools(OrderService orderService, Validator validator) {
        this.orderService = orderService;
        this.validator = validator;
    }

    private OrderRequest validated(OrderRequest request) {
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return request;
    }

    @McpTool(name = "list_orders",
            description = "List all customer orders. Returns each order's id, customer, product, quantity, and status.")
    public List<Order> listOrders() {
        return orderService.findAll();
    }

    @McpTool(name = "get_order",
            description = "Fetch a single order by its numeric id. Fails if no order with that id exists.")
    public Order getOrder(
            @McpToolParam(description = "The numeric id of the order to fetch", required = true)
            Long id) {
        return orderService.findById(id);
    }

    @McpTool(name = "search_orders_by_customer",
            description = "Search orders by customer name. Returns all orders whose customer contains the given text, case-insensitive.")
    public List<Order> searchOrdersByCustomer(
            @McpToolParam(description = "Text to match against the customer name", required = true)
            String customer) {
        return orderService.searchByCustomer(customer);
    }

    @McpTool(name = "create_order",
            description = "Create a new order. Status defaults to NEW if omitted. Returns the created order including its newly assigned id.")
    public Order createOrder(
            @McpToolParam(description = "Customer name", required = true) String customer,
            @McpToolParam(description = "Product being ordered", required = true) String product,
            @McpToolParam(description = "Quantity ordered", required = true) Integer quantity,
            @McpToolParam(description = "Order status, e.g. NEW, SHIPPED, CANCELLED", required = false) String status) {
        return orderService.create(validated(new OrderRequest(customer, product, quantity, status)).toEntity());
    }

    @McpTool(name = "update_order_status",
            description = "Update the status of an existing order by id, e.g. to SHIPPED or CANCELLED. Fails if no order with that id exists.")
    public Order updateOrderStatus(
            @McpToolParam(description = "The numeric id of the order to update", required = true) Long id,
            @McpToolParam(description = "New status, e.g. NEW, SHIPPED, CANCELLED", required = true) String status) {
        if (status == null || !status.matches(ALLOWED_STATUS)) {
            throw new IllegalArgumentException("status must be NEW, SHIPPED or CANCELLED");
        }
        return orderService.updateStatus(id, status);
    }

    @McpTool(name = "delete_order",
            description = "Delete an order by its numeric id. Fails if no order with that id exists.")
    public String deleteOrder(
            @McpToolParam(description = "The numeric id of the order to delete", required = true)
            Long id) {
        orderService.delete(id);
        return "Deleted order with id: " + id;
    }
}

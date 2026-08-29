package com.example.ordermcp.dto;

import com.example.ordermcp.model.Order;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Validated request payload for creating an order. Kept separate from the JPA
 * entity so validation lives at the API/tool boundary, not on the entity.
 */
public record OrderRequest(

        @NotBlank(message = "customer is required")
        @Size(max = 200, message = "customer must be at most 200 characters")
        String customer,

        @NotBlank(message = "product is required")
        @Size(max = 200, message = "product must be at most 200 characters")
        String product,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        Integer quantity,

        // Optional; when provided must be one of the known statuses (null is allowed).
        @Pattern(regexp = "NEW|SHIPPED|CANCELLED", message = "status must be NEW, SHIPPED or CANCELLED")
        String status
) {
    /** Maps this request to a new (unpersisted) entity. */
    public Order toEntity() {
        return new Order(customer, product, quantity, status);
    }
}

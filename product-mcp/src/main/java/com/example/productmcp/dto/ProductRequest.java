package com.example.productmcp.dto;

import com.example.productmcp.model.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Validated request payload for creating/updating a product. Kept separate from
 * the JPA entity so validation lives at the API boundary, not on the entity.
 */
public record ProductRequest(

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "price must not be negative")
        BigDecimal price,

        @NotNull(message = "quantity is required")
        @PositiveOrZero(message = "quantity must not be negative")
        Integer quantity
) {
    /** Maps this request to a new (unpersisted) entity. */
    public Product toEntity() {
        return new Product(name, description, price, quantity);
    }
}

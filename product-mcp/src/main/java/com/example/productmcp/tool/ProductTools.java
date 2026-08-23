package com.example.productmcp.tool;

import com.example.productmcp.model.Product;
import com.example.productmcp.service.ProductService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProductTools {

    private final ProductService productService;

    public ProductTools(ProductService productService) {
        this.productService = productService;
    }

    @McpTool(name = "list_products",
            description = "List all products in the catalog. Returns each product's id, name, description, price, and quantity.")
    public List<Product> listProducts() {
        return productService.findAll();
    }

    @McpTool(name = "get_product",
            description = "Fetch a single product by its numeric id. Fails if no product with that id exists.")
    public Product getProduct(
            @McpToolParam(description = "The numeric id of the product to fetch", required = true)
            Long id) {
        return productService.findById(id);
    }

    @McpTool(name = "search_products",
            description = "Search products by name. Returns all products whose name contains the given text, case-insensitive.")
    public List<Product> searchProducts(
            @McpToolParam(description = "Text to match against product names", required = true)
            String name) {
        return productService.searchByName(name);
    }

    @McpTool(name = "create_product",
            description = "Create a new product in the catalog. Returns the created product including its newly assigned id.")
    public Product createProduct(
            @McpToolParam(description = "Product name", required = true) String name,
            @McpToolParam(description = "Product description", required = false) String description,
            @McpToolParam(description = "Unit price", required = true) BigDecimal price,
            @McpToolParam(description = "Quantity in stock", required = true) Integer quantity) {
        return productService.create(new Product(name, description, price, quantity));
    }

    @McpTool(name = "update_product",
            description = "Update an existing product by id. Replaces all fields, so supply the full set of values. Fails if no product with that id exists.")
    public Product updateProduct(
            @McpToolParam(description = "The numeric id of the product to update", required = true) Long id,
            @McpToolParam(description = "Product name", required = true) String name,
            @McpToolParam(description = "Product description", required = false) String description,
            @McpToolParam(description = "Unit price", required = true) BigDecimal price,
            @McpToolParam(description = "Quantity in stock", required = true) Integer quantity) {
        return productService.update(id, new Product(name, description, price, quantity));
    }

    @McpTool(name = "delete_product",
            description = "Delete a product by its numeric id. Fails if no product with that id exists.")
    public String deleteProduct(
            @McpToolParam(description = "The numeric id of the product to delete", required = true)
            Long id) {
        productService.delete(id);
        return "Deleted product with id: " + id;
    }
}

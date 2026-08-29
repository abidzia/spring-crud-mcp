package com.example.productmcp.tool;

import com.example.productmcp.model.Product;
import com.example.productmcp.service.ProductService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The MCP tool class is a thin delegator over {@link ProductService}; these
 * unit tests confirm each @McpTool method calls the service and returns its result.
 */
class ProductToolsTest {

    private final ProductService service = mock(ProductService.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ProductTools tools = new ProductTools(service, validator);

    private Product sample() {
        return new Product("Mouse", "desc", new BigDecimal("9.99"), 5);
    }

    @Test
    void listProductsDelegatesToService() {
        Product p = sample();
        given(service.findAll()).willReturn(List.of(p));

        assertThat(tools.listProducts()).containsExactly(p);
    }

    @Test
    void getProductDelegatesToService() {
        Product p = sample();
        given(service.findById(1L)).willReturn(p);

        assertThat(tools.getProduct(1L)).isSameAs(p);
    }

    @Test
    void searchProductsDelegatesToService() {
        Product p = sample();
        given(service.searchByName("mo")).willReturn(List.of(p));

        assertThat(tools.searchProducts("mo")).containsExactly(p);
    }

    @Test
    void createProductBuildsProductAndDelegates() {
        given(service.create(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        Product result = tools.createProduct("Mouse", "desc", new BigDecimal("9.99"), 5);

        assertThat(result.getName()).isEqualTo("Mouse");
        assertThat(result.getPrice()).isEqualByComparingTo("9.99");
        verify(service).create(any(Product.class));
    }

    @Test
    void createProductWithInvalidDataIsRejected() {
        // blank name + negative price violate the request constraints
        assertThatThrownBy(() ->
                tools.createProduct("", "desc", new BigDecimal("-1"), 5))
                .isInstanceOf(ConstraintViolationException.class);
        verify(service, never()).create(any(Product.class));
    }

    @Test
    void deleteProductReturnsConfirmationMessage() {
        String message = tools.deleteProduct(7L);

        assertThat(message).contains("7");
        verify(service).delete(7L);
    }
}

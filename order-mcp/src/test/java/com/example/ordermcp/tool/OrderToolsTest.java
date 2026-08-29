package com.example.ordermcp.tool;

import com.example.ordermcp.model.Order;
import com.example.ordermcp.service.OrderService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the MCP tool class: each @McpTool method should delegate to
 * {@link OrderService} and return its result.
 */
class OrderToolsTest {

    private final OrderService service = mock(OrderService.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final OrderTools tools = new OrderTools(service, validator);

    private Order sample() {
        return new Order("Alice", "Widget", 2, "NEW");
    }

    @Test
    void listOrdersDelegatesToService() {
        Order o = sample();
        given(service.findAll()).willReturn(List.of(o));

        assertThat(tools.listOrders()).containsExactly(o);
    }

    @Test
    void getOrderDelegatesToService() {
        Order o = sample();
        given(service.findById(1L)).willReturn(o);

        assertThat(tools.getOrder(1L)).isSameAs(o);
    }

    @Test
    void searchOrdersByCustomerDelegatesToService() {
        Order o = sample();
        given(service.searchByCustomer("ali")).willReturn(List.of(o));

        assertThat(tools.searchOrdersByCustomer("ali")).containsExactly(o);
    }

    @Test
    void createOrderBuildsOrderAndDelegates() {
        given(service.create(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        Order result = tools.createOrder("Alice", "Widget", 2, "NEW");

        assertThat(result.getCustomer()).isEqualTo("Alice");
        assertThat(result.getProduct()).isEqualTo("Widget");
        verify(service).create(any(Order.class));
    }

    @Test
    void updateOrderStatusDelegatesToService() {
        Order shipped = sample();
        shipped.setStatus("SHIPPED");
        given(service.updateStatus(1L, "SHIPPED")).willReturn(shipped);

        assertThat(tools.updateOrderStatus(1L, "SHIPPED").getStatus()).isEqualTo("SHIPPED");
        verify(service).updateStatus(1L, "SHIPPED");
    }

    @Test
    void createOrderWithInvalidDataIsRejected() {
        // blank customer + zero quantity violate the request constraints
        assertThatThrownBy(() ->
                tools.createOrder("", "Widget", 0, "NEW"))
                .isInstanceOf(ConstraintViolationException.class);
        verify(service, never()).create(any(Order.class));
    }

    @Test
    void updateOrderStatusWithUnknownStatusIsRejected() {
        assertThatThrownBy(() ->
                tools.updateOrderStatus(1L, "BOGUS"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(service, never()).updateStatus(any(), any());
    }

    @Test
    void deleteOrderReturnsConfirmationMessage() {
        String message = tools.deleteOrder(7L);

        assertThat(message).contains("7");
        verify(service).delete(7L);
    }
}

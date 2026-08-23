package com.example.ordermcp.service;

import com.example.ordermcp.exception.OrderNotFoundException;
import com.example.ordermcp.model.Order;
import com.example.ordermcp.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> searchByCustomer(String customer) {
        return orderRepository.findByCustomerContainingIgnoreCase(customer);
    }

    @Transactional
    public Order create(Order order) {
        order.setId(null);
        if (order.getStatus() == null || order.getStatus().isBlank()) {
            order.setStatus("NEW");
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateStatus(Long id, String status) {
        Order existing = findById(id);
        existing.setStatus(status);
        return orderRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteById(id);
    }
}

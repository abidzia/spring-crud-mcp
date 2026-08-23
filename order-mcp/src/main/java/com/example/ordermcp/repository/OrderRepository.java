package com.example.ordermcp.repository;

import com.example.ordermcp.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerContainingIgnoreCase(String customer);
}

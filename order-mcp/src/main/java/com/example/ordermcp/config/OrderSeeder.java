package com.example.ordermcp.config;

import com.example.ordermcp.model.Order;
import com.example.ordermcp.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Seeds a few sample orders on startup so the MCP tools have data to return.
 * Customers repeat (Alice, Bob) so search_orders_by_customer returns multiple
 * hits, and statuses vary (NEW, SHIPPED, CANCELLED) for status-update testing.
 */
@Configuration
public class OrderSeeder {

    private static final Logger log = LoggerFactory.getLogger(OrderSeeder.class);

    @Bean
    CommandLineRunner seedOrders(OrderRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            List<Order> orders = List.of(
                    new Order("Alice Johnson", "Wireless Mouse", 2, "SHIPPED"),
                    new Order("Bob Smith", "Mechanical Keyboard Pro", 1, "NEW"),
                    new Order("Alice Johnson", "27-inch 4K Monitor", 1, "NEW"),
                    new Order("Carol White", "Portable SSD 1TB", 3, "SHIPPED"),
                    new Order("Bob Smith", "Noise-Cancelling Headphones", 1, "CANCELLED"),
                    new Order("Dan Brown", "Electric Standing Desk", 1, "NEW")
            );
            repository.saveAll(orders);
            log.info("Seeded {} sample orders.", orders.size());
        };
    }
}

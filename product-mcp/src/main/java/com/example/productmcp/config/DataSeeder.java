package com.example.productmcp.config;

import com.example.productmcp.model.Product;
import com.example.productmcp.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the in-memory catalog with sample products on startup so REST and MCP
 * surfaces have data to exercise without manual inserts.
 *
 * <p>The dataset is deliberately varied for testing:
 * <ul>
 *   <li>overlapping names ("Wireless", "Keyboard", "Pro") so
 *       {@code findByNameContainingIgnoreCase} search returns multiple hits;</li>
 *   <li>two zero-quantity items to exercise out-of-stock handling;</li>
 *   <li>a wide price range (12.99 – 449.00) for range/formatting checks.</li>
 * </ul>
 *
 * <p>Guarded by a count check so it never double-seeds (harmless with the default
 * in-memory DB, which resets each restart, but correct if persistence is added).
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedProducts(ProductRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                log.info("Product catalog already populated ({} rows); skipping seed.",
                        repository.count());
                return;
            }

            List<Product> products = List.of(
                    new Product("Wireless Mouse",
                            "Ergonomic 2.4GHz wireless mouse with silent clicks",
                            new BigDecimal("24.99"), 150),
                    new Product("Wireless Keyboard",
                            "Compact wireless keyboard with backlit keys",
                            new BigDecimal("45.50"), 80),
                    new Product("Mechanical Keyboard Pro",
                            "Hot-swappable mechanical keyboard with RGB backlighting",
                            new BigDecimal("129.00"), 25),
                    new Product("USB-C Hub",
                            "7-in-1 USB-C hub with HDMI, Ethernet, and power delivery",
                            new BigDecimal("39.99"), 60),
                    new Product("Laptop Stand",
                            "Aluminium adjustable laptop stand with cable management",
                            new BigDecimal("34.95"), 0),
                    new Product("27-inch 4K Monitor",
                            "27-inch 4K UHD IPS monitor with 99% sRGB",
                            new BigDecimal("299.99"), 15),
                    new Product("Noise-Cancelling Headphones",
                            "Over-ear Bluetooth headphones with active noise cancellation",
                            new BigDecimal("199.99"), 40),
                    new Product("1080p Webcam",
                            "Full HD 1080p webcam with autofocus and privacy shutter",
                            new BigDecimal("49.99"), 100),
                    new Product("Portable SSD 1TB",
                            "1TB USB 3.2 Gen 2 portable solid-state drive",
                            new BigDecimal("89.99"), 55),
                    new Product("LED Desk Lamp",
                            "Dimmable LED desk lamp with adjustable colour temperature",
                            new BigDecimal("27.49"), 0),
                    new Product("Electric Standing Desk",
                            "Height-adjustable electric standing desk with memory presets",
                            new BigDecimal("449.00"), 8),
                    new Product("Cable Organizer Pro",
                            "Magnetic cable organizer set for desk and wall",
                            new BigDecimal("12.99"), 300)
            );

            repository.saveAll(products);
            log.info("Seeded {} sample products into the catalog.", products.size());
        };
    }
}

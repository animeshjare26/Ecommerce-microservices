package com.ecommerce.productservice.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Product (The Warehouse Item Box)
 * 
 * WHY THIS EXISTS:
 * This class directly maps to the `products` table in the PostgreSQL database for this specific microservice.
 * It contains exactly what a product needs: name, description, price, and current stock quantity.
 * 
 * ARCHITECTURE NOTE:
 * This is saved in the `product_db` PostgreSQL database. It is completely physically 
 * isolated from the `user_db` and `order_db`.
 */
@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    // In real systems, prices are often stored as BigDecimal to avoid 
    // floating-point precision errors (e.g. 0.1 + 0.2 = 0.30000000000000004)
    private Double price;
    
    private Integer quantity; // The current available stock
}

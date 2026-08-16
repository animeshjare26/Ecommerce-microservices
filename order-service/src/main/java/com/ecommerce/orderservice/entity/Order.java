package com.ecommerce.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "orders") 
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private Long productId;
    
    /**
 * Order (The Digital Receipt)
 * 
 * WHY THIS EXISTS:
 * This class directly maps to the `orders` table in the PostgreSQL database for this specific microservice.
 * Notice that it only stores the `userId` and `productId`, not the full User or Product objects. 
 * This is how microservices stay decoupled!
 */
    private Integer quantity;
    private Double totalPrice;
    private String status; // PENDING, COMPLETED, FAILED
    private LocalDateTime orderDate;
}

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
    
    // E-commerce specific fields
    private Integer quantity;
    private Double totalPrice;
    private String status; // PENDING, COMPLETED, FAILED
    private LocalDateTime orderDate;
}

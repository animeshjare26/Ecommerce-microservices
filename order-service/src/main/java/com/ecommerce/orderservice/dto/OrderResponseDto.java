package com.ecommerce.orderservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderResponseDto {
    private Long id;
    private Long userId;
    private Long productId;
    
    private Integer quantity;
    private Double totalPrice;
    private String status;
    private LocalDateTime orderDate;
    
    private String message;
}

package com.ecommerce.orderservice.dto;

import lombok.Data;

@Data
public class OrderRequestDto {
    private Long userId;
    private Long productId;
    private Integer quantity; // How many items is the user buying?
}

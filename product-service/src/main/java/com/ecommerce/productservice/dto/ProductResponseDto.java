package com.ecommerce.productservice.dto;

import lombok.Data;

@Data
public class ProductResponseDto {
    private Long id;
    private String name;
    private Double price;
    private Integer quantity;
}

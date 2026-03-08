package com.ecommerce.productservice.dto;

import lombok.Data;

@Data
public class ProductRequestDto {
    private String name;
    private Double price;
    private Integer quantity;
}

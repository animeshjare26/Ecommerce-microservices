package com.ecommerce.orderservice.feign;

import lombok.Data;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private Double price;
    private Integer quantity;
}

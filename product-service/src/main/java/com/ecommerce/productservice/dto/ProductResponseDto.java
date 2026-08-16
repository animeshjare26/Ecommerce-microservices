package com.ecommerce.productservice.dto;

import lombok.Data;

/**
 * ProductResponseDto (The Censored Warehouse File)
 * 
 * WHY THIS EXISTS:
 * When the frontend asks to see a product, we send back this DTO instead of the raw database `Product` object. 
 * This ensures we never accidentally leak internal database IDs or metadata that the frontend shouldn't see.
 *
 * DTO for outgoing Product data.
 * Sent back to clients so they can render the product catalog on the frontend.
 */
@Data
public class ProductResponseDto {
    private Long id;
    private String name;
    private Double price;
    private Integer quantity;
}

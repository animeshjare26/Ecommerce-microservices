package com.ecommerce.productservice.dto;

import lombok.Data;

/**
 * ProductRequestDto (The Warehouse Intake Form)
 * 
 * WHY THIS EXISTS:
 * When an admin wants to add a new product to the catalog, they submit this specific "form". 
 * It ensures we only accept exactly the data we need (name, price, stock) and nothing malicious.
 *
 * DTO for incoming Product payloads.
 * Controls what an Admin is allowed to submit when creating/updating a product.
 */
@Data
public class ProductRequestDto {
    private String name;
    private Double price;
    private Integer quantity;
}

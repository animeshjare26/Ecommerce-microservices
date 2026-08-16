package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.ProductRequestDto;
import com.ecommerce.productservice.dto.ProductResponseDto;
import com.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ProductController (The Warehouse Reception Desk)
 * 
 * WHY THIS EXISTS:
 * This controller handles incoming HTTP requests related to the product catalog. 
 * Just like a front desk at a warehouse, it takes requests (like "show me all laptops"), 
 * checks if the request is valid, hands it off to the Warehouse Manager (ProductService) 
 * to fetch the boxes, and then gives a standardized response back to the caller.
 * 
 * ARCHITECTURE ROLE:
 * This microservice owns everything related to Products, Pricing, and Inventory.
 * When users browse the website, their requests flow through the API Gateway to here.
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;

    /**
     * Adds a new product to the catalog.
     * 
     * SECURITY NOTE: 
     * Adding products should only be done by Administrators! 
     * We use @PreAuthorize (enabled in SecurityConfig) to ensure the JWT has "ROLE_ADMIN".
     * If a regular user tries this, Spring returns a 403 Forbidden.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDto addProduct(@RequestBody ProductRequestDto requestDto) {
        return productService.addProduct(requestDto);
    }

    /**
     * Retrieves details for a single product. 
     * (Accessible by both USER and ADMIN)
     */
    @GetMapping("/{id}")
    public ProductResponseDto getProductDetails(@PathVariable Long id) {
        return productService.getProductDetails(id);
    }

    /**
     * Retrieves products that are currently in stock.
     * E-COMMERCE USE CASE: When rendering the homepage, we only want to show products 
     * that users can actually buy right now.
     */
    @GetMapping("/available")
    public List<ProductResponseDto> getAvailableProducts() {
        return productService.getAllAvailableProducts();
    }

    /**
     * Updates product details (price, name, total quantity).
     * Only admins can do this.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponseDto updateProduct(@PathVariable Long id, @RequestBody ProductRequestDto requestDto) {
        return productService.updateProduct(id, requestDto);
    }

    /**
     * Reduces the inventory of a product.
     * 
     * MICROSERVICES INTERACTION:
     * This endpoint is actually called internally by the `order-service` via a Feign Client 
     * during the checkout process. When an order is placed, the order-service tells the 
     * product-service "Hey, reduce the stock of Product 5 by 2 units."
     */
    @PutMapping("/{id}/reduce-inventory")
    @ResponseStatus(HttpStatus.OK)
    public void reduceInventory(@PathVariable Long id, @RequestParam Integer quantity) {
        productService.reduceInventory(id, quantity);
    }

    /**
     * Removes a product from the catalog.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}

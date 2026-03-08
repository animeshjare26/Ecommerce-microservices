package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.dto.ProductRequestDto;
import com.ecommerce.productservice.dto.ProductResponseDto;
import com.ecommerce.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDto addProduct(@RequestBody ProductRequestDto requestDto) {
        return productService.addProduct(requestDto);
    }

    @GetMapping("/{id}")
    public ProductResponseDto getProductDetails(@PathVariable Long id) {
        return productService.getProductDetails(id);
    }

    // E-commerce: users want to see only available stock
    @GetMapping("/available")
    public List<ProductResponseDto> getAvailableProducts() {
        return productService.getAllAvailableProducts();
    }

    @PutMapping("/{id}")
    public ProductResponseDto updateProduct(@PathVariable Long id, @RequestBody ProductRequestDto requestDto) {
        return productService.updateProduct(id, requestDto);
    }

    // E-commerce specific API for the Order Service to trigger
    @PutMapping("/{id}/reduce-inventory")
    @ResponseStatus(HttpStatus.OK)
    public void reduceInventory(@PathVariable Long id, @RequestParam Integer quantity) {
        productService.reduceInventory(id, quantity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}

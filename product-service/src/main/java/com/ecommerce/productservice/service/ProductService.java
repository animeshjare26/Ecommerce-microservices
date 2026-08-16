package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductRequestDto;
import com.ecommerce.productservice.dto.ProductResponseDto;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductService (The Warehouse Foreman)
 * 
 * WHY THIS EXISTS:
 * This service contains the actual business logic for managing products. 
 * While the ProductController (Receptionist) handles HTTP stuff, this class handles the real work: 
 * checking if there is enough stock in the database (Filing Cabinet), reserving items, 
 * and throwing errors if something is out of stock.
 * 
 * WHY THIS EXISTS:
 * This layer handles data mapping, ensuring inventory doesn't drop below zero, 
 * and querying the database via the Repository.
 */
@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;

    public ProductResponseDto addProduct(ProductRequestDto requestDto) {
        Product product = new Product();
        product.setName(requestDto.getName());
        product.setPrice(requestDto.getPrice());
        product.setQuantity(requestDto.getQuantity());

        Product savedProduct = productRepository.save(product);
        return mapToResponseDto(savedProduct);
    }

    public ProductResponseDto getProductDetails(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return mapToResponseDto(product);
    }

    /**
     * E-COMMERCE USE CASE: 
     * Filtering out products that are out of stock. We do this at the database query level 
     * (using findByQuantityGreaterThan) rather than fetching all products and filtering them in Java. 
     * This is much more memory efficient.
     */
    public List<ProductResponseDto> getAllAvailableProducts() {
        return productRepository.findByQuantityGreaterThan(0)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public ProductResponseDto updateProduct(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setName(requestDto.getName());
        product.setPrice(requestDto.getPrice());
        product.setQuantity(requestDto.getQuantity());

        Product updatedProduct = productRepository.save(product);
        return mapToResponseDto(updatedProduct);
    }

    /**
     * E-COMMERCE SPECIFIC: Safely reducing inventory.
     * 
     * RACE CONDITIONS WARNING:
     * In a high-traffic production system, doing this simply via Java logic can lead to 
     * negative inventory if two users buy the last item at the exact same millisecond.
     * To fix that, we would usually use Database Row Locking (@Lock) or update queries:
     * `UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?`
     */
    public void reduceInventory(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Prevent negative inventory
        if (product.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient inventory for product. Available: " + product.getQuantity());
        }

        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponseDto mapToResponseDto(Product product) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setQuantity(product.getQuantity());
        return dto;
    }
}

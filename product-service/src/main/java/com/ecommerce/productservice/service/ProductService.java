package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductRequestDto;
import com.ecommerce.productservice.dto.ProductResponseDto;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    public List<ProductResponseDto> getAllAvailableProducts() {
        // E-commerce: Only show products in stock
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

    // E-commerce specific: reduce inventory gracefully
    public void reduceInventory(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

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

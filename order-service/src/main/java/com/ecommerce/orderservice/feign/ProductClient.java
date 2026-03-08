package com.ecommerce.orderservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {
    
    @GetMapping("/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);

    // Communicate with Product Service to dynamically reduce inventory
    @PutMapping("/products/{id}/reduce-inventory")
    void reduceInventory(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}

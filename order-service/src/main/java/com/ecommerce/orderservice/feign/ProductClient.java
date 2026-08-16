package com.ecommerce.orderservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OpenFeign REST Client for the Product Service.
 * 
 * HOW IT WORKS:
 * Instead of writing boilerplate code using `RestTemplate` or `WebClient` to manually craft 
 * HTTP requests, we simply define an interface. Spring Cloud Feign reads these annotations 
 * and generates a proxy implementation at runtime.
 * 
 * WHY "PRODUCT-SERVICE"?
 * Because of Eureka Service Discovery, we don't hardcode "http://localhost:8082". 
 * We just tell Feign to look up "PRODUCT-SERVICE" in Eureka, and Eureka will return the correct IP.
 */
@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {
    
    /**
     * Executes a `GET http://PRODUCT-SERVICE/products/{id}` 
     * and maps the JSON response into a local `ProductDto` object.
     */
    @GetMapping("/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);

    /**
     * Executes a `PUT http://PRODUCT-SERVICE/products/{id}/reduce-inventory?quantity={quantity}`
     * to safely update the inventory in the remote database.
     */
    @PutMapping("/products/{id}/reduce-inventory")
    void reduceInventory(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}

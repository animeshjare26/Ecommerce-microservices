package com.ecommerce.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ProductServiceApplication (The Warehouse Manager's Office)
 * 
 * WHY THIS EXISTS:
 * This microservice manages the entire catalog of products. Think of it like the 
 * manager of a giant warehouse: it knows what items exist, their prices, and how much 
 * stock is left, but it doesn't care who is buying them or how they are paying.
 * 
 * ARCHITECTURE ROLE:
 * Manages the product catalog and inventory. It does not know who the users are, 
 * nor does it care about orders. It only cares about: "Here is a product, here is its price, 
 * and I have 5 in stock."
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
        System.out.println("Product Service started !");
    }
}

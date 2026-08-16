package com.ecommerce.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * OrderServiceApplication (The Checkout Counter)
 * 
 * WHY THIS EXISTS:
 * This microservice handles customer orders. Think of it like a cashier at a checkout counter. 
 * The cashier needs to check your ID (User Service) and scan your items (Product Service) 
 * to complete the transaction. This service orchestrates all of that using FeignClients.
 */
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("Order Service started !");
    }
}

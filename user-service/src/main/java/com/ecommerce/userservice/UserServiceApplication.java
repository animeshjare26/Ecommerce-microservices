package com.ecommerce.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * UserServiceApplication (The HR Department)
 * 
 * WHY THIS EXISTS:
 * This microservice manages user profiles, addresses, and account details. It does NOT handle 
 * passwords or login (that's the Auth Service's job). Think of it like the HR department: 
 * they know where you live and your employee details, but they don't issue your ID badge.
 * 
 * ARCHITECTURE ROLE:
 * This microservice is responsible for managing User Profiles, specifically 
 * for e-commerce purposes (like addresses, contact info). 
 * 
 * IMPORTANT DISTINCTION:
 * This service does NOT handle passwords or authentication. That is the job of the `auth-service`.
 * The `auth-service` manages the login process and issues JWTs. 
 * This `user-service` trusts those JWTs and manages the actual business data related to the user.
 * 
 * WHY ISOLATE THIS?
 * If we have a massive spike in users updating their profile pictures or addresses, we can 
 * scale up this `user-service` independently of the `order-service` or `auth-service`.
 */
@EnableDiscoveryClient // Registers this service with Eureka so the Gateway and Order Service can find it.
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("User Service started !");
    }
}

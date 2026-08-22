package com.ecommerce.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 16. AuthServiceApplication (The Engine Starter)
 *
 * INTERN GUIDE: THE ENTRY POINT
 * -----------------------------
 * This is the very first file that runs when we start the microservice.
 *
 * WHY THIS EXISTS:
 * `@SpringBootApplication` tells Spring Boot to look through this entire package
 * and automatically configure all our Controllers, Services, and Repositories.
 *
 * `@EnableDiscoveryClient` tells this service to announce itself to the Eureka Server
 * (the API Gateway uses Eureka to find us).
 *
 * `@EnableFeignClients` tells Spring Cloud to scan this package for interfaces annotated
 * with `@FeignClient` (we have one: UserProfileClient) and generate a working HTTP-client
 * implementation for each at startup. Without this annotation, our UserProfileClient
 * interface would just be an empty contract that nobody implements, and the profile-sync
 * call to the user-service would fail with a "no such bean" error.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("Auth Service Started ");
    }
}

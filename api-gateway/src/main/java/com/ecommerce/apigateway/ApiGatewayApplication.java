package com.ecommerce.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ApiGatewayApplication (The Front Desk Receptionist of the Entire Building)
 *
 * Imagine our microservices architecture as a massive corporate building with many different departments 
 * (Auth Service, User Service, Product Service, etc.).
 * 
 * WHY THIS EXISTS:
 * The outside world (the frontend app) doesn't know where each department is located inside the building.
 * Instead of letting people wander the halls looking for the Product department, everyone must talk to 
 * this API Gateway (the Receptionist) first. The Gateway looks at what they want (e.g., "/products"), 
 * checks the directory (Eureka), and immediately transfers them to the correct internal department.
 * 
 * <h2>What this service does</h2>
 * At startup, this application:
 * <ol>
 *   <li>Connects to Eureka to build a local cache of available service instances.</li>
 *   <li>Parses routing predicates and filters defined in {@code application.yml}.</li>
 *   <li>Intercepts all incoming HTTP traffic from the outside world.</li>
 *   <li>Load-balances and proxies traffic to the correct downstream microservice based on the URI path.</li>
 * </ol>
 *
 * <h2>Routing Resolution</h2>
 * <pre>
 *   Incoming HTTP GET /users/1/profile
 *     ↓ (Matched by Route ID: user-service `Path=/users/**`)
 *   Gateway resolves `lb://USER-SERVICE` via Eureka cache
 *     ↓
 *   Gateway proxies request to http://{dynamic-user-service-ip}:{dynamic-port}/users/1/profile
 * </pre>
 *
 * <h2>Under the Hood (Spring Cloud Gateway)</h2>
 * <p>
 * Unlike traditional Spring WebMVC apps, the Gateway is built on <b>Spring WebFlux and Netty</b>. 
 * This makes it fully non-blocking and reactive, allowing it to handle thousands of concurrent 
 * connections with very few threads. This is crucial for a Gateway that sits in front of the entire cluster.
 * </p>
 */
@SpringBootApplication
/*
 * @EnableDiscoveryClient
 * ---------------------
 * WHY: Connects this Spring Boot app to the Service Registry (Eureka).
 *
 * Internally, it imports DiscoveryClientAutoConfiguration, which:
 *  - Registers this Gateway as an instance in Eureka.
 *  - Fetches the registry mapping (App IDs to IPs) and keeps it updated in the background.
 *  - Provides the `DiscoveryClient` bean used by the Gateway's LoadBalancer (`lb://`) to 
 *    round-robin traffic across multiple instances of a downstream service.
 */
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        /*
         * SpringApplication.run() bootstraps the Spring context:
         *  1. Connects to the Config Server (if available) to pull gateway routes.
         *  2. Connects to Eureka to pull downstream service IPs.
         *  3. Starts the Reactive Netty server (not Tomcat) to handle high-throughput routing.
         */
        SpringApplication.run(ApiGatewayApplication.class, args);
        System.out.println("Api Gateway started !");
    }
}

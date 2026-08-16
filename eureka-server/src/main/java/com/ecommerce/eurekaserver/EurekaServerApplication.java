package com.ecommerce.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * EurekaServerApplication (The Corporate Phonebook / Operator)
 *
 * Imagine a large company where employees are constantly switching desks and getting new extensions.
 * 
 * WHY THIS EXISTS:
 * If the "Order Service" needs to talk to the "User Service", it doesn't know the User Service's 
 * exact IP address (which changes constantly in the cloud). So, it calls this Eureka Server 
 * (the Operator) and asks, "What's the current number for USER-SERVICE?" 
 * Eureka looks it up in its dynamic phonebook and connects them. Every service must call Eureka 
 * every 30 seconds (a heartbeat) to say "I'm still alive at this IP!"
 * 
 * <h2>What this service does</h2>
 * At startup, this application:
 * <ol>
 *   <li>Initializes an in-memory registry of available microservices.</li>
 *   <li>Listens for registration requests (heartbeats) from Eureka Clients.</li>
 *   <li>Serves the current registry list to clients requesting peer addresses (e.g., API Gateway).</li>
 *   <li>Evicts instances that fail to send heartbeats within the expiration threshold.</li>
 * </ol>
 *
 * <h2>Why Dynamic Discovery?</h2>
 * <p>
 * In containerized environments (Docker/Kubernetes), microservices get assigned random IP addresses 
 * and ports. Hardcoding `http://192.168.1.5:8081` in the Order Service will break as soon as the User 
 * Service container restarts. Eureka acts as a dynamic "phonebook" allowing services to route traffic 
 * using logical names (e.g. `lb://USER-SERVICE`).
 * </p>
 */
@SpringBootApplication
/*
 * @EnableEurekaServer
 * ------------------
 * WHY: Activates the Netflix Eureka Server components within this Spring Boot app.
 *
 * Internally, it imports EurekaServerAutoConfiguration, which:
 *  - Configures the PeerAwareInstanceRegistry (the core storage for service metadata).
 *  - Sets up the InstanceRegistry dashboard (available at root /).
 *  - Registers the REST controllers that clients use to register (/eureka/apps/{appId}).
 *  - Starts the eviction timer task to clear out dead services.
 *
 * WITHOUT @EnableEurekaServer: This app would just be a generic Tomcat web server with no registry capabilities.
 */
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        /*
         * SpringApplication.run() bootstraps the Spring context:
         *  1. Loads application.yml to configure server port (8761).
         *  2. Starts the Eureka instance registry.
         *  3. Disables self-registration via `eureka.client.register-with-eureka=false`.
         *  4. Starts embedded Tomcat on port 8761 to accept heartbeats.
         */
        SpringApplication.run(EurekaServerApplication.class, args);
        System.out.println("Eureka Server started !");
    }
}

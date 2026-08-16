package com.ecommerce.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * ConfigServerApplication (The Central Filing Cabinet)
 *
 * Imagine a large company where every department needs a copy of the company policies, but the policies 
 * change frequently.
 * 
 * WHY THIS EXISTS:
 * Instead of going into each department (User Service, Order Service, etc.) and updating their 
 * individual `application.yml` files manually every time a database password or a timeout setting changes, 
 * we store ALL the configurations centrally in this Config Server (the Filing Cabinet). 
 * When a microservice starts up, it immediately asks the Config Server for its specific settings.
 * 
 * <h2>What this service does</h2>
 * At startup, this application:
 * <ol>
 *   <li>Reads property files from {@code classpath:/config/} (the 'native' profile).</li>
 *   <li>Exposes an HTTP API at {@code /{application}/{profile}[/{label}]}.</li>
 *   <li>Client services call this API at bootstrap time to receive their full configuration.</li>
 *   <li>Registers itself with Eureka so clients can discover it by name
 *       ({@code spring.cloud.config.discovery.service-id=config-server}).</li>
 * </ol>
 *
 * <h2>Config resolution order (most-specific wins)</h2>
 * <pre>
 *   {application}-{profile}.yml  (e.g. auth-service-docker.yml)
 *     ↑ overrides
 *   {application}.yml             (e.g. auth-service.yml)
 *     ↑ overrides
 *   application.yml               (shared defaults — served to ALL clients)
 * </pre>
 *
 * <h2>Why this service is STANDALONE (no common-lib dependency)</h2>
 * config-server must start before any business service. If it depended on
 * common-lib and common-lib had transitive issues, the entire system would fail
 * to boot. Keeping it dependency-free eliminates this circular bootstrap risk.
 */
@SpringBootApplication
/*
 * @EnableConfigServer
 * ------------------
 * WHY: This single annotation activates Spring Cloud Config Server autoconfiguration.
 *
 * Internally it imports ConfigServerConfiguration, which:
 *  - Creates an EnvironmentRepository bean (NativeEnvironmentRepository when
 *    spring.profiles.active=native, JGitEnvironmentRepository for 'git' profile).
 *  - Registers EnvironmentController — the REST controller that handles
 *    GET /{application}/{profile} and GET /{application}/{profile}/{label} requests.
 *  - Sets up encryption/decryption support for {cipher} placeholders (if a
 *    symmetric or RSA key is configured).
 *
 * WITHOUT @EnableConfigServer: the app starts as a plain Spring Boot web
 * application. The /config endpoint does not exist and clients get HTTP 404.
 * Adding this annotation is the ONLY supported way to enable config-server behavior.
 */
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        /*
         * SpringApplication.run() bootstraps the Spring context:
         *  1. Loads application.yml from src/main/resources/
         *  2. Because spring.profiles.active=native, activates NativeEnvironmentRepository
         *  3. Registers with Eureka (spring-cloud-starter-netflix-eureka-client)
         *  4. Starts embedded Tomcat on port 8888
         *
         * No special configuration is needed here — all behavior is driven by
         * application.yml and the @EnableConfigServer annotation above.
         */
        SpringApplication.run(ConfigServerApplication.class, args);
        System.out.println("Config server started !");
    }

}

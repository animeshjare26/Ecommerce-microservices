# E-Commerce Microservices - Infrastructure Services

Welcome to the step-by-step implementation guide for your industry-standard microservices architecture using Spring Boot 3.x!

As a Senior Architect, I'll guide you through each piece. We are dividing the work into Infrastructure (Config, Discovery, Gateway) and Business services.

---

## STEP 1 — Config Server

**Why this service exists:**
In microservices, managing configurations (like DB credentials, ports) in every individual service is a nightmare. The Config Server acts as a central repository for all configuration properties across all environments.
**What problem it solves:**
When you need to change a property (like a database password), you change it in one central place rather than updating and rebuilding multiple microservices.

### 1. Folder Structure

```text
config-server/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/ecommerce/configserver/
        │       └── ConfigServerApplication.java
        └── resources/
            ├── application.yml
            └── config/           <-- (For native profile, config files go here)
                ├── gateway-service.yml
                ├── user-service.yml
                ├── product-service.yml
                └── order-service.yml
```

### 2. pom.xml dependencies

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.3</version> <!-- Spring Boot 3.x -->
        <relativePath/>
    </parent>
    <groupId>com.ecommerce</groupId>
    <artifactId>config-server</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version> <!-- Compatible Release Train -->
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-config-server</artifactId>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 3. application.yml configuration

```yaml
server:
  port: 8888

spring:
  application:
    name: config-server
  profiles:
    active: native # For simplicity, using filesystem instead of Git repository
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config/ # Where other services configs are stored
```

_(Note: As a beginner, we use the `native` profile which loads configurations from the local `resources/config` folder instead of requiring a Git repository)._

### 4. Main Spring Boot Application class

```java
package com.ecommerce.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer // This annotation is crucial! It turns this app into a Config Server
@SpringBootApplication
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

---

## STEP 2 — Eureka Server (Service Registry)

**Why this service exists:**
In a cloud environment, instances of services can start and stop dynamically. Their IP addresses change. A Service Registry is like a telephone directory for microservices.
**What problem it solves:**
Instead of hardcoding IP addresses (like `http://192.168.1.5:8081/users`), services simply ask Eureka "Where is the user-service?". Eureka keeps track of all active services.

### 1. Folder Structure

```text
eureka-server/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/ecommerce/eurekaserver/
        │       └── EurekaServerApplication.java
        └── resources/
            └── application.yml
```

### 2. pom.xml dependencies

```xml
<!-- (Parent and Spring Cloud properties identical to Config Server) -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
    </dependencies>
```

### 3. application.yml configuration

```yaml
server:
  port: 8761 # Default industry standard port for Eureka

spring:
  application:
    name: eureka-server
  config:
    import: optional:configserver:http://localhost:8888/ # Fetch config from Config Server

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false # It is the server, so it shouldn't register with itself
    fetch-registry: false # It holds the registry, no need to fetch it
```

### 4. Main Spring Boot Application class

```java
package com.ecommerce.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer // Enables the Eureka registry
@SpringBootApplication
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

---

## STEP 3 — API Gateway

**Why this service exists:**
It acts as the single entry point for all client requests (Mobile, Web, etc.).
**What problem it solves:**
Instead of front-end applications needing to know the URLs of Product, User, and Order services individually, they only call the Gateway. The Gateway routes the request to the correct internal service, and can also handle cross-cutting concerns like security, CORS, and logging.

### 1. Folder Structure

```text
api-gateway/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/ecommerce/apigateway/
        │       └── ApiGatewayApplication.java
        └── resources/
            └── application.yml
```

### 2. pom.xml dependencies

```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId> <!-- Reactive API Gateway -->
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId> <!-- To find services -->
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
    </dependencies>
```

### 3. application.yml configuration

```yaml
server:
  port: 8080 # Client hits port 8080

spring:
  application:
    name: api-gateway
  config:
    import: optional:configserver:http://localhost:8888/

  cloud:
    gateway:
      discovery:
        locator:
          enabled: true # Let gateway use Eureka to find routes natively
      routes:
        - id: user-service
          uri: lb://USER-SERVICE # "lb://" means LoadBalancer, route using Eureka
          predicates:
            - Path=/users/**
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/products/**
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/orders/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### 4. Main Spring Boot Application class

```java
package com.ecommerce.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

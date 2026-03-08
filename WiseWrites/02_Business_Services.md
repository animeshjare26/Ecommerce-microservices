# E-Commerce Microservices - Business Services

These services contain our actual business logic. They each have an independent database, adhering to the "Database per Service" microservice pattern.

---

## STEP 4 — User Service (Port 8081)

**Why this service exists:**
To handle all user-related data (registration, profiles) independently.
**What problem it solves:**
By isolating user data, if the User Service goes down or scales up, it doesn't directly crash the order or product services.

### 1. Folder Structure

```text
user-service/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/ecommerce/userservice/
        │       ├── UserServiceApplication.java
        │       ├── controller/
        │       │   └── UserController.java
        │       ├── entity/
        │       │   └── User.java
        │       ├── repository/
        │       │   └── UserRepository.java
        │       └── service/
        │           └── UserService.java
        └── resources/
            └── application.yml
```

### 2. pom.xml dependencies

```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
```

### 3. application.yml configuration

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service
  config:
    import: optional:configserver:http://localhost:8888/
  datasource:
    url: jdbc:postgresql://localhost:5432/user_db
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update # Automatically creates tables

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### 4. Classes (Entity, Repository, Service, Controller, App)

**Application Class**

```java
@EnableDiscoveryClient
@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

**Entity**

```java
@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
}
```

**Repository**

```java
public interface UserRepository extends JpaRepository<User, Long> {}
```

**Service**

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User createUser(User user) { return userRepository.save(user); }
    public User getUser(Long id) { return userRepository.findById(id).orElse(null); }
}
```

**Controller**

```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public User create(@RequestBody User user) { return userService.createUser(user); }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) { return userService.getUser(id); }
}
```

---

## STEP 5 — Product Service (Port 8082)

**Why this service exists:**
To manage inventory independently.

_(Note: Folder structure and classes are nearly identical to User Service, just reflecting `Product` instead of `User`.)_

### 1-3: Configuration Details

**pom.xml**: Same as User Service.
**application.yml**: Same as User Service but port `8082` and DB `product_db`, name `product-service`.

### 4. Classes

**Entity**

```java
@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double price;
    private Integer quantity;
}
```

**Controller**

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductRepository productRepository; // Simplified directly to repository for brevity

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productRepository.findById(id).orElseThrow();
    }
}
```

---

## STEP 6 — Order Service (Port 8083)

**Why this service exists:**
Responsible for checkout. It must ask the User Service for validation and Product Service for inventory.
**What problem it solves:**
It ties domains together. Here we use **OpenFeign** for clean REST calls and **Resilience4j** to ensure that if the Product Service dies, the Order Service handles it gracefully rather than crashing.

### 1. Folder Structure

```text
order-service/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/ecommerce/orderservice/
        │       ├── OrderServiceApplication.java
        │       ├── controller/
        │       │   └── OrderController.java
        │       ├── entity/
        │       │   └── Order.java
        │       ├── feign/
        │       │   ├── UserClient.java
        │       │   └── ProductClient.java
        │       └── service/
        │           └── OrderService.java
        └── resources/
            └── application.yml
```

### 2. pom.xml dependencies

Add these ON TOP of the standard Web/JPA/Postgres/Eureka/Config dependencies:

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId> <!-- Required for Resilience4j metrics -->
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
```

### 3. application.yml configuration

```yaml
server:
  port: 8083

spring:
  application:
    name: order-service
  config:
    import: optional:configserver:http://localhost:8888/
  datasource:
    url: jdbc:postgresql://localhost:5432/order_db
    username: postgres
    password: password

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

# Resilience4j properties
resilience4j.circuitbreaker:
  instances:
    productServiceCB:
      registerHealthIndicator: true
      slidingWindowSize: 10
      permittedNumberOfCallsInHalfOpenState: 3
      waitDurationInOpenState: 5s
      failureRateThreshold: 50
```

### 4. Main Spring Boot Application class

```java
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### 9. Feign Clients (Inter-service Communication)

Instead of messy `RestTemplate` calls, Feign lets us create interfaces.

```java
@FeignClient(name = "USER-SERVICE") // Calls User Service via Eureka name
public interface UserClient {
    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable("id") Long id);
}

@FeignClient(name = "PRODUCT-SERVICE") // Calls Product Service
public interface ProductClient {
    @GetMapping("/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);
}
```

### 10. Circuit Breaker & Service Layer

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;

    @CircuitBreaker(name = "productServiceCB", fallbackMethod = "productFallback")
    public String placeOrder(Long userId, Long productId) {
        // 1. Call User Service directly
        UserDto user = userClient.getUser(userId);

        // 2. Call Product Service (Protected by Circuit Breaker)
        ProductDto product = productClient.getProduct(productId);

        if(user != null && product != null) {
            Order order = new Order();
            order.setUserId(userId);
            order.setProductId(productId);
            // Save order code here...
            return "Order placed successfully!";
        }
        return "Order failed. Invalid info.";
    }

    // FALLBACK METHOD - Executed if Product Service is down
    public String productFallback(Long userId, Long productId, Throwable throwable) {
        return "Sorry! Product Service is currently down. We cannot process your order right now. Please try again later.";
    }
}
```

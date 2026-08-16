package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderRequestDto;
import com.ecommerce.orderservice.dto.OrderResponseDto;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.feign.ProductClient;
import com.ecommerce.orderservice.feign.ProductDto;
import com.ecommerce.orderservice.feign.UserClient;
import com.ecommerce.orderservice.feign.UserDto;
import com.ecommerce.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderService (The Store Manager)
 * 
 * WHY THIS EXISTS:
 * This service contains the actual business logic for checking out. 
 * It talks to the UserClient (to verify the buyer) and the ProductClient (to verify stock). 
 * Only if both say "OK", does it save the Order to the database and reduce stock.
 */
/**
 * Core business logic for Order Processing.
 *
 * <h2>What this service does</h2>
 * This class orchestrates the checkout process. It is the primary example of 
 * <b>Inter-Process Communication (IPC)</b> in this architecture.
 * <ol>
 *   <li>Receives an order request.</li>
 *   <li>Synchronously queries the {@code user-service} via Feign for shipping details.</li>
 *   <li>Synchronously queries the {@code product-service} via Feign for price and stock.</li>
 *   <li>Applies business logic (stock validation, total price calculation).</li>
 *   <li>Commands the {@code product-service} to deduct inventory.</li>
 *   <li>Persists the final Order record locally.</li>
 * </ol>
 *
 * <h2>Fault Tolerance and Resilience</h2>
 * <p>
 * Because this service relies heavily on network calls to other microservices, it is extremely 
 * vulnerable to cascading failures. If the {@code product-service} goes down, the {@code order-service} 
 * threads will block waiting for a response, eventually crashing this service as well.
 * We use <b>Resilience4j Circuit Breakers</b> to prevent this.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;

    /*
     * @CircuitBreaker
     * ----------------
     * WHY: Protects this method from cascading network failures.
     * 
     * Internally, Resilience4j intercepts calls to this method using an AOP Proxy:
     * - CLOSED State: Calls pass through normally. It monitors failure rates (e.g., HTTP 500s or Timeouts).
     * - OPEN State: If the failure rate exceeds the threshold (e.g., 50%), the circuit "opens". 
     *   Calls are instantly rejected without making network requests, and the `fallbackMethod` is executed.
     * - HALF-OPEN State: After a cooldown period, it lets a few test requests through to see if the 
     *   downstream service has recovered.
     */
    @CircuitBreaker(name = "productServiceCB", fallbackMethod = "productFallback")
    public OrderResponseDto placeOrder(OrderRequestDto requestDto) {
        
        /*
         * EXECUTION FLOW:
         * 1. Synchronous HTTP call to USER-SERVICE via Eureka-resolved proxy.
         */
        UserDto user = userClient.getUser(requestDto.getUserId());

        /*
         * 2. Synchronous HTTP call to PRODUCT-SERVICE.
         */
        ProductDto product = productClient.getProduct(requestDto.getProductId());

        if (user == null || product == null) {
            throw new RuntimeException("Invalid User or Product ID");
        }

        /*
         * 3. Business Logic Validation: Ensure sufficient stock exists.
         */
        if (product.getQuantity() < requestDto.getQuantity()) {
            OrderResponseDto failedResponse = new OrderResponseDto();
            failedResponse.setStatus("FAILED");
            failedResponse.setMessage("Insufficient stock. Only " + product.getQuantity() + " items left.");
            return failedResponse;
        }

        /*
         * 4. State Mutation: Command the product-service to deduct inventory.
         */
        productClient.reduceInventory(product.getId(), requestDto.getQuantity());

        /*
         * 5. Build and persist the aggregate Order entity in the local PostgreSQL DB.
         */
        Order order = new Order();
        order.setUserId(user.getId());
        order.setProductId(product.getId());
        order.setQuantity(requestDto.getQuantity());
        order.setTotalPrice(product.getPrice() * requestDto.getQuantity());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("COMPLETED");

        Order savedOrder = orderRepository.save(order);
        
        return mapToResponseDto(savedOrder, 
                "Order placed successfully! Total Cost: $" + savedOrder.getTotalPrice() + 
                ". Shipping to: " + user.getAddress());
    }

    /**
     * Fallback method invoked by the Circuit Breaker.
     * 
     * <p><b>Method Signature Requirement:</b> Must exactly match the signature of the original method 
     * plus a {@link Throwable} parameter at the end to capture the exact failure reason.</p>
     */
    public OrderResponseDto productFallback(OrderRequestDto requestDto, Throwable throwable) {
        OrderResponseDto fallbackResponse = new OrderResponseDto();
        fallbackResponse.setStatus("FAILED");
        fallbackResponse.setMessage("Sorry! Checkout is temporarily unavailable as the Inventory system is down. Please try again later. Error: " + throwable.getMessage());
        return fallbackResponse;
    }

    public OrderResponseDto getOrderDetails(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return mapToResponseDto(order, "Order found successfully");
    }

    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(order -> mapToResponseDto(order, "Order history retrieved"))
                .collect(Collectors.toList());
    }

    private OrderResponseDto mapToResponseDto(Order order, String message) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setProductId(order.getProductId());
        dto.setQuantity(order.getQuantity());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setOrderDate(order.getOrderDate());
        dto.setMessage(message);
        return dto;
    }
}

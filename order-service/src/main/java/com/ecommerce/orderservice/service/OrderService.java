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

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final ProductClient productClient;

    @CircuitBreaker(name = "productServiceCB", fallbackMethod = "productFallback")
    public OrderResponseDto placeOrder(OrderRequestDto requestDto) {
        
        // 1. Verify User exists
        UserDto user = userClient.getUser(requestDto.getUserId());

        // 2. Fetch Product 
        ProductDto product = productClient.getProduct(requestDto.getProductId());

        if (user == null || product == null) {
            throw new RuntimeException("Invalid User or Product ID");
        }

        // 3. E-commerce Logic: Inventory Check
        if (product.getQuantity() < requestDto.getQuantity()) {
            OrderResponseDto failedResponse = new OrderResponseDto();
            failedResponse.setStatus("FAILED");
            failedResponse.setMessage("Insufficient stock. Only " + product.getQuantity() + " items left.");
            return failedResponse;
        }

        // 4. Reduce Inventory remotely via Feign
        productClient.reduceInventory(product.getId(), requestDto.getQuantity());

        // 5. Calculate Price and build Order
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

    // FALLBACK METHOD - Executed if Product Service is down
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

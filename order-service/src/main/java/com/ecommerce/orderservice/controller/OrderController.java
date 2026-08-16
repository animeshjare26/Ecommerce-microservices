package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.OrderRequestDto;
import com.ecommerce.orderservice.dto.OrderResponseDto;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OrderController (The Cashier's Register)
 * 
 * WHY THIS EXISTS:
 * This controller receives requests to create or view orders. 
 * Just like handing items to a cashier, the frontend sends a request here ("I want to buy product X"). 
 * The controller hands the request to the Order Manager (OrderService) to do the actual checkout process.
 * 
 * ARCHITECTURE ROLE:
 * The Order Service is the "Aggregator" of our microservices. 
 * To successfully place an order, it must orchestrate data from the `user-service` 
 * (to get the shipping address) and the `product-service` (to check inventory and calculate price).
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;

    /**
     * The primary checkout endpoint.
     * 
     * HOW THE DATA FLOWS:
     * 1. Client sends a POST request with the Product ID and Quantity.
     * 2. The OrderService intercepts this and makes synchronous HTTP calls to 
     *    user-service and product-service.
     * 3. If everything is valid, an order is placed.
     */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto checkout(@RequestBody OrderRequestDto requestDto) {
        return orderService.placeOrder(requestDto);
    }

    /**
     * Retrieves the details of a specific order.
     */
    @GetMapping("/{id}")
    public OrderResponseDto getOrderDetails(@PathVariable Long id) {
        return orderService.getOrderDetails(id);
    }

    /**
     * Retrieves all orders (Order History).
     * Typically, this would take a `userId` parameter so users only see their own orders.
     */
    @GetMapping("/history")
    public List<OrderResponseDto> getOrderHistory() {
        return orderService.getAllOrders();
    }
}

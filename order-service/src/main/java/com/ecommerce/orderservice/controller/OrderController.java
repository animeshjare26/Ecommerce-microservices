package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.OrderRequestDto;
import com.ecommerce.orderservice.dto.OrderResponseDto;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto checkout(@RequestBody OrderRequestDto requestDto) {
        return orderService.placeOrder(requestDto);
    }

    @GetMapping("/{id}")
    public OrderResponseDto getOrderDetails(@PathVariable Long id) {
        return orderService.getOrderDetails(id);
    }

    @GetMapping("/history")
    public List<OrderResponseDto> getOrderHistory() {
        return orderService.getAllOrders();
    }
}

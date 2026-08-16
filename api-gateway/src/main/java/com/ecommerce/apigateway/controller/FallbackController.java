package com.ecommerce.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * FallbackController (The Apologetic Receptionist)
 * 
 * This file handles responses when a Circuit Breaker trips in the API Gateway.
 * 
 * WHY WE ADDED IT:
 * In a microservices architecture, sometimes a downstream service (like the user-service) crashes or gets 
 * overwhelmed. Our Gateway has a "Circuit Breaker" to detect this and stop sending traffic to the broken service.
 * Without this FallbackController, the user would see a scary, unreadable "503 Service Unavailable" HTML error.
 * This class catches those broken requests and instead returns a polite, clean JSON response explaining that 
 * the service is temporarily down, ensuring the frontend app doesn't crash trying to parse HTML instead of JSON.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<ResponseEntity<Map<String, Object>>> fallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Service is currently unavailable. Please try again later.");
        response.put("data", null);
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}

package com.ecommerce.orderservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign REST Client for the User Service.
 * 
 * Allows the Order Service to seamlessly fetch User Profiles (specifically their 
 * shipping address) by making an HTTP call to the user-service via Eureka discovery.
 */
@FeignClient(name = "USER-SERVICE")
public interface UserClient {
    
    /**
     * Executes a `GET http://USER-SERVICE/users/{id}/profile`
     */
    @GetMapping("/users/{id}/profile")
    UserDto getUser(@PathVariable("id") Long id);
}

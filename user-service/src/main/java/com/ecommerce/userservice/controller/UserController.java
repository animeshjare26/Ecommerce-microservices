package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.UserRequestDto;
import com.ecommerce.userservice.dto.UserResponseDto;
import com.ecommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserController (The HR Receptionist)
 * 
 * WHY THIS EXISTS:
 * This controller handles incoming HTTP requests related to user profiles. 
 * Just like a receptionist at the HR department, it takes requests (like "update my address"), 
 * checks if the request is valid, hands it off to the HR Manager (UserService) to do the actual work, 
 * and then gives a standardized response back to the caller.
 * 
 * ARCHITECTURE FLOW:
 * 1. A client (e.g., React Frontend) sends a request to the API Gateway: `GET /users/1/profile`
 * 2. The Gateway sees the `/users/**` path and routes it to this microservice.
 * 3. Our `JwtAuthenticationFilter` intercepts the request, verifies the JWT token, and allows it through.
 * 4. This Controller receives the request and maps it to the `getProfile()` method.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    /*
     * NOTE: The old public "POST /users/register" endpoint has been REMOVED.
     *
     * WHY: Letting the public create profiles directly is what caused the auth<->user disconnect —
     * those profiles got a random auto-generated id unrelated to the person's auth identity. Profiles
     * are now created by the auth-service during sign-up, via the internal provisioning endpoint (see
     * InternalUserController). The public surface of this service is now read/update only.
     */

    /**
     * Fetches a specific user's profile by their ID.
     */
    @GetMapping("/{id}/profile")
    public UserResponseDto getProfile(@PathVariable Long id) { 
        return userService.getUserProfile(id); 
    }

    /**
     * Fetches all users. 
     * In a real production system, this MUST be paginated to avoid OutOfMemory errors if there are millions of users.
     */
    @GetMapping
    public List<UserResponseDto> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Updates an entire user profile.
     * PUT implies replacing the whole resource.
     */
    @PutMapping("/{id}/profile")
    public UserResponseDto updateProfile(@PathVariable Long id, @RequestBody UserRequestDto requestDto) {
        return userService.updateProfile(id, requestDto);
    }

    /**
     * Partially updates a user profile (just the address).
     * PATCH implies a partial update. 
     * E-COMMERCE USE CASE: This is specifically optimized for a quick checkout flow where a user 
     * might change their shipping address right before purchasing an order.
     */
    @PatchMapping("/{id}/address")
    public UserResponseDto updateAddress(@PathVariable Long id, @RequestParam String address) {
        return userService.updateAddress(id, address);
    }

    /**
     * Deletes a user profile.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content is the standard response for a successful DELETE
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}

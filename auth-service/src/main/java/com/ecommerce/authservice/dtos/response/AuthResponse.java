package com.ecommerce.authservice.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 15. AuthResponse (The Welcome Package)
 * 
 * INTERN GUIDE: CONTROLLING THE OUTPUT
 * ------------------------------------
 * Data Transfer Object (DTO) sent back to the client after a successful login or registration.
 * 
 * WHY THIS EXISTS:
 * Security! If we just returned the `AuthUser` entity directly from the database, 
 * we would accidentally send the hashed password to the user's browser! 
 * This DTO acts as a strict filter, ensuring the client only receives exactly what 
 * they need to know.
 * 
 * WHAT IT CONTAINS:
 * It contains the "Welcome Package" - the `accessToken` (used for API calls), 
 * the `refreshToken` (used to get new access tokens), and basic profile data 
 * like their `username` and `roles` so the frontend can display their name in the UI.
 */
@Data // Lombok: Generates getters, setters, toString, equals, hashcode behind the scenes.
@AllArgsConstructor // Lombok: Generates a constructor that takes every field as an argument.
public class AuthResponse {
    
    // The short-lived token sent in the Authorization header for API calls
    private String accessToken; 
    
    // The long-lived token used ONLY to get new access tokens when they expire
    private String refreshToken; 
    
    // The standard HTTP authorization scheme prefix (usually "Bearer")
    private String tokenType; 
    
    // Useful for the frontend to fetch the user's detailed profile from the User Service
    private Long userId; 
    
    private String username;
    
    // e.g. "ROLE_ADMIN,ROLE_USER". The frontend uses this to show/hide admin buttons.
    private String roles; 
}

package com.ecommerce.authservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 12. LoginRequest (Data Transfer Object)
 * 
 * INTERN GUIDE: DTOs AND VALIDATION
 * ---------------------------------
 * Data Transfer Object (DTO) for incoming Login requests.
 * 
 * WHY THIS EXISTS:
 * We don't want the frontend sending data directly into our Database Entities (AuthUser).
 * Instead, we use DTOs as "Envelopes". When a user submits a login form, the JSON 
 * payload they send is mapped directly to this object by Spring.
 * 
 * VALIDATION:
 * We use `jakarta.validation.constraints` (@NotBlank) to ensure the client actually 
 * sends data. If they send an empty username, Spring automatically rejects the request 
 * with a 400 Bad Request error before it even reaches our controller logic!
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    private String password;
}

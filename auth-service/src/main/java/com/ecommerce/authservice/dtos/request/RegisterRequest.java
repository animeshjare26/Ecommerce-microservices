package com.ecommerce.authservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 13. RegisterRequest (Data Transfer Object)
 * 
 * INTERN GUIDE: CUSTOM VALIDATION RULES
 * -------------------------------------
 * Data Transfer Object (DTO) for incoming Registration requests.
 * 
 * WHY THIS EXISTS:
 * Separating `LoginRequest` and `RegisterRequest` into two different classes allows 
 * us to enforce entirely different validation rules depending on the action!
 * 
 * For instance, when a user registers, we MUST ensure their new password is strong.
 * So we use `@Size(min = 8)` to enforce a minimum length. If they send a 3-character 
 * password, Spring throws a 400 Bad Request before the request even hits our code.
 * During login (LoginRequest), we don't care about password length validation, we 
 * only care if the password matches what is in the database.
 */
@Data
public class RegisterRequest {
    
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}

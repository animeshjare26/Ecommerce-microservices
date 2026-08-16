package com.ecommerce.userservice.dto;

import lombok.Data;

/**
 * UserRequestDto (The HR Intake Form)
 * 
 * WHY THIS EXISTS:
 * When a user wants to create or update their profile, they submit this specific "form". 
 * It ensures we only accept exactly the data we need (name, email) and nothing malicious.
 * 
 * Data Transfer Object (DTO) for incoming User requests.
 * 
 * WHY THIS EXISTS:
 * When a client sends a JSON payload to update a profile, we don't map it directly 
 * to our Database Entity (`User.java`). 
 * 
 * If we mapped it directly to the Entity, a malicious user could theoretically 
 * send `{"id": 1, "isAdmin": true}` and overwrite sensitive database fields.
 * By using this DTO, we strictly control which fields the user is allowed to submit.
 */
@Data
public class UserRequestDto {
    private String name;
    private String email;
    
    // Crucial for the e-commerce domain. Users will update this during checkout.
    private String address; 
}

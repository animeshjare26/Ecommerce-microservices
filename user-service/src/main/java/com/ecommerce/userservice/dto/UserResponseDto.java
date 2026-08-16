package com.ecommerce.userservice.dto;

import lombok.Data;

/**
 * UserResponseDto (The Censored HR File)
 * 
 * WHY THIS EXISTS:
 * When the frontend asks to see a user's profile, we send back this DTO instead of the raw database `User` object. 
 * This ensures we never accidentally leak internal database IDs or metadata that the frontend shouldn't see.
 * 
 * Data Transfer Object (DTO) for outgoing User responses.
 * 
 * WHY THIS EXISTS:
 * Just like we restrict what users can send us via UserRequestDto, we restrict 
 * what we send back to them. 
 * 
 * For instance, if our `User` entity had a `internalTrustScore` field used by an 
 * anti-fraud microservice, we would NOT include it in this DTO, ensuring it never 
 * leaks to the frontend.
 */
@Data
public class UserResponseDto {
    private Long id;
    private String name;
    private String email;
    private String address; 
}

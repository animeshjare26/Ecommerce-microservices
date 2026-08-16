package com.ecommerce.authservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 14. TokenRequest (The Renewal Form)
 * 
 * INTERN GUIDE: TOKEN RENEWAL
 * ---------------------------
 * When a user's short-lived Access Token expires (usually after 15 minutes), 
 * they use their long-lived Refresh Token to ask the server for a new Access Token.
 * 
 * WHY WE ADDED IT:
 * This is the simple JSON payload the client sends to our `/auth/refresh` endpoint:
 * { "token": "my-long-refresh-token-string" }
 * 
 * The `@NotBlank` annotation ensures they don't accidentally send an empty renewal request.
 */
@Data
public class TokenRequest {
    
    @NotBlank(message = "Token cannot be blank")
    private String token;
}

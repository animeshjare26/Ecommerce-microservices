package com.ecommerce.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Cryptographic Validator for JSON Web Tokens (JWT).
 * 
 * ARCHITECTURE ROLE:
 * This component allows the auth-service to validate its own tokens if a user attempts to 
 * access protected endpoints (e.g., getting their own profile info or changing passwords).
 * 
 * HOW IT WORKS:
 * 1. The `auth-service` signed the JWT using a secret key (HMAC-SHA256).
 * 2. It hashes the incoming token payload. If its hash matches the token's signature,
 *    we know the token is authentic and hasn't been tampered with.
 */
@Component
public class JwtValidator {

    // 12-FACTOR APP: This secret is loaded from environment variables in production!
    @Value("${jwt.secret:defaultSecretKeyThatShouldBeOverriddenInProduction1234567890}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // Convert the string secret into a cryptographically secure Key object.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(key) // Apply our secret key
                .build()
                .parseSignedClaims(token) // This performs the actual math to verify the signature
                .getPayload(); // Returns the JSON body (containing userId, roles, etc.)
    }
}

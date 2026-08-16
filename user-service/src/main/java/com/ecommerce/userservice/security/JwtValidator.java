package com.ecommerce.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JwtValidator (The Badge Scanner)
 * 
 * WHY THIS EXISTS:
 * Remember how the Auth Service had the "Badge Printing Machine" (JwtUtils)? 
 * This class is a Badge Scanner. It doesn't know how to print badges, it only knows 
 * how to scan a badge (JWT) to make sure it's valid, hasn't expired, and wasn't forged. 
 * Every downstream microservice has one of these scanners so they can independently verify 
 * users without having to constantly ask the Auth Service "Is this guy legit?".
 * 
 * ARCHITECTURE ROLE:
 * This component allows the user-service to perform "Distributed Validation".
 * Instead of asking the auth-service "Is this token valid?", the user-service 
 * mathematically verifies the token's cryptographic signature itself.
 * 
 * HOW IT WORKS:
 * 1. The `auth-service` signed the JWT using a secret key (HMAC-SHA256).
 * 2. This `user-service` loads the EXACT SAME secret key from `application.yml`.
 * 3. It hashes the incoming token payload. If its hash matches the token's signature,
 *    we know the token is authentic and hasn't been tampered with.
 */
@Component
public class JwtValidator {

    // 12-FACTOR APP: This secret is loaded from environment variables in production!
    @Value("${jwt.secret:defaultSecretKeyThatShouldBeOverriddenInProduction1234567890}")
    private String secret;

    private SecretKey key;

    /**
     * @PostConstruct tells Spring to run this method exactly once, immediately after 
     * injecting the `secret` value.
     */
    @PostConstruct
    public void init() {
        // Convert the string secret into a cryptographically secure Key object.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * Parses the JWT string into Claims (the payload/JSON body of the token).
     * 
     * ERROR HANDLING:
     * If the token is expired, tampered with, or malformed, the `parseSignedClaims()` 
     * method will throw an exception (like ExpiredJwtException), instantly stopping the request.
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(key) // Apply our secret key
                .build()
                .parseSignedClaims(token) // This performs the actual math to verify the signature
                .getPayload(); // Returns the JSON body (containing userId, roles, etc.)
    }
}

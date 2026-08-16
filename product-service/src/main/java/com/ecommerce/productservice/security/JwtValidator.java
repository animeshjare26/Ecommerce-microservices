package com.ecommerce.productservice.security;

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
 * This class is a Badge Scanner. It doesn't know how to print badges, it only knows 
 * how to scan a badge (JWT) to make sure it's valid, hasn't expired, and wasn't forged. 
 * Every downstream microservice has one of these scanners so they can independently verify 
 * users without having to constantly ask the Auth Service "Is this guy legit?".
 *
 * Validates JWT tokens sent to this microservice.
 */
@Component
public class JwtValidator {

    // This secret must MATCH EXACTLY the one used in auth-service application.yml
    @Value("${jwt.secret:defaultSecretKeyThatShouldBeOverriddenInProduction1234567890}")
    private String secret;

    private SecretKey key;

    // Runs once when the bean is created to initialize the HMAC key from the secret string
    @PostConstruct
    public void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * Parses the JWT. If the signature is invalid, expired, or malformed, 
     * this will throw an exception (e.g., ExpiredJwtException, SignatureException).
     *
     * @param token The raw JWT string
     * @return Claims extracted from the token payload (e.g., subject, roles)
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                // Set the signing key to verify the signature
                .verifyWith(key)
                // Build the parser
                .build()
                // Parse the token string into Claims (the payload)
                .parseSignedClaims(token)
                // Extract the actual payload claims from the Jws wrapper
                .getPayload();
    }
}

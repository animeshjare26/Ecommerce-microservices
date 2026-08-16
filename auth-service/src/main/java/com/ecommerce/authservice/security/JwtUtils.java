package com.ecommerce.authservice.security;

import com.ecommerce.authservice.config.JwtConfig;
import com.ecommerce.authservice.enums.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * 3. JwtUtils (The Badge Printing Machine)

 * ---------------------------------------------
 * This class is responsible for the actual cryptographic heavy lifting. 
 * When a user logs in, we need to issue them an "ID Badge" (a JSON Web Token).
 * 
 * It performs two main jobs:
 * 1. Generating Tokens: Taking user info (like ID and Role) and cryptographically signing it into a secure string.
 * 2. Parsing Tokens: Taking a token string provided by the user, verifying its signature to ensure it wasn't tampered with, and reading the data inside.
 */
@Component
public class JwtUtils {

    // Injects our JwtConfig class which holds the secret keys and expiration times from application.yml
    private final JwtConfig jwtConfig;

    public JwtUtils(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * Generates a new JWT.
     * 
     * @param tokenType Is this an ACCESS_TOKEN or REFRESH_TOKEN?
     * @param subject The primary identifier for the token (usually the User ID).
     * @param claims Additional data we want to store in the token (e.g., username, roles).
     * @return The signed JWT string.
     */
    public String generateToken(TokenType tokenType, String subject, Map<String, ?> claims) {
        // 1. Fetch the correct configuration (secret and expiration time) for this token type.
        JwtConfig.TokenConfig tokenConfig = jwtConfig.getTokenConfigByType(tokenType);
        
        // 2. Use the JJWT library's builder pattern to construct the token.
        return Jwts
                .builder()
                .subject(subject) // Sets the 'sub' (subject) claim, usually the user's ID
                .claims(claims) // Adds our custom payload data (like roles)
                .issuedAt(new Date()) // Sets the 'iat' (issued at) claim to right now
                // Sets the 'exp' (expiration) claim. We multiply by 60 * 1000 to convert minutes to milliseconds.
                .expiration(new Date(new Date().getTime() + (tokenConfig.getExpiration() * 60 * 1000))) 
                // Cryptographically signs the token using HMAC SHA-256 algorithm and our secret key.
                .signWith(getKey(tokenConfig.getSecret()), Jwts.SIG.HS256)
                .compact(); // Assembles it all into the final Base64URL encoded string.
    }

    /**
     * Helper method to convert our plain text secret key from application.yml 
     * into a cryptographic SecretKey object required by the JJWT library.
     */
    private SecretKey getKey(String secretKey) {
        // We decode the Base64 secret key and generate an HMAC SHA key.
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    /**
     * Reads a token, verifies it is authentic, and extracts all its payload data (Claims).
     * 
     * @param tokenType The expected type of token.
     * @param token The actual token string.
     * @return A Claims object containing all the data from the token.
     */
    public Claims getAllClaimsFromToken(TokenType tokenType, String token) {
        // Get the specific secret key used to sign this type of token.
        JwtConfig.TokenConfig tokenConfig = jwtConfig.getTokenConfigByType(tokenType);
        
        // 1. The parser verifies the signature. If the token was tampered with, or signed with a different key, this throws an exception!
        // 2. If valid, it parses the claims and returns them.
        return Jwts.parser().verifyWith(getKey(tokenConfig.getSecret())).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Extracts only the specific claims we care about for our application logic.
     */
    public Map<String, String> getClaimsFromToken(TokenType tokenType, String token) {
        // First, validate and extract ALL claims.
        final Claims claims = getAllClaimsFromToken(tokenType, token);
        
        // Then, pluck out just the pieces we need and return them as a simple Map.
        return Map.of(
                "subject", claims.getSubject(),
                // Safely extract the 'id' claim, handling potential nulls.
                "id", claims.get("id") != null ? claims.get("id").toString() : ""
        );
    }
}

package com.ecommerce.userservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JwtAuthenticationFilter (The HR Door Guard)
 * 
 * WHY THIS EXISTS:
 * Even though the API Gateway is the front door to the building, each specific microservice 
 * (like this User Service) still has a guard at its door. This filter runs on EVERY incoming request. 
 * It takes the "Authorization: Bearer <token>" header, hands the token to the JwtValidator to check 
 * if it's real, and if it is, tells Spring Security "This person is allowed in!"
 * 
 * Before a request ever reaches our `UserController`, it must pass through a chain of filters.
 * This custom filter intercepts the request, looks for a JWT, and tells Spring Security 
 * "Yes, this person is logged in" or "No, reject them".
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    /**
     * This method executes for EVERY single HTTP request made to the user-service.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Grab the "Authorization" header from the HTTP request
        String authHeader = request.getHeader("Authorization");

        // 2. JWTs standardly use the "Bearer " prefix. Check if it exists.
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 3. Strip the first 7 characters ("Bearer ") to get the raw token string
            String token = authHeader.substring(7);

            try {
                // 4. Validate the cryptographic signature
                Claims claims = jwtValidator.validateToken(token);
                
                // 5. Extract the payload data
                String userId = claims.getSubject();
                List<String> roles = claims.get("roles", List.class);
                
                if (roles != null) {
                    // 6. Convert the raw role strings (e.g., "ROLE_USER") into Spring Security GrantedAuthorities
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // 7. Create a token representing the authenticated user.
                    // (We don't need their password here, just their ID and roles).
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    // 8. Place the user into the Security Context.
                    // This tells Spring Security: "This request is authenticated."
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // If validation fails (expired, hacked token), clear the context.
                // Spring Security will automatically reject the request with a 401 Unauthorized later in the chain.
                SecurityContextHolder.clearContext();
            }
        }

        // 9. Proceed to the next filter in the chain. 
        // If authentication was successful, it will eventually reach our UserController.
        filterChain.doFilter(request, response);
    }
}

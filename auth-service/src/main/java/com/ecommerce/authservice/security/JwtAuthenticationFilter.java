package com.ecommerce.authservice.security;

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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 2. JwtAuthenticationFilter (The Security Gatekeeper)

 * ------------------------------------------
 * This custom filter sits in the Security Filter Chain. 
 * Every single HTTP request to this microservice must pass through this class.
 * 
 * Its job is simple:
 * 1. Look at the incoming request's headers.
 * 2. Does it have an "Authorization" header with a JWT?
 * 3. If yes, is the JWT valid?
 * 4. If yes, tell Spring Security "This user is logged in" and let the request proceed.
 * 
 * By extending OncePerRequestFilter, we guarantee this logic runs exactly once per HTTP request.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    /**
     * This method contains the core logic that executes for every request.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Grab the "Authorization" header from the incoming HTTP request.
        // This is where clients (like a React app) place the JWT.
        String authHeader = request.getHeader("Authorization");

        // 2. The standard format is "Authorization: Bearer <token>". 
        // We check if the header exists and starts with the correct prefix.
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            
            // 3. Strip the first 7 characters ("Bearer ") to isolate the raw JWT string.
            String token = authHeader.substring(7);

            try {
                // 4. Validate the cryptographic signature of the token.
                // If the token is expired or was tampered with, this will throw an Exception.
                Claims claims = jwtValidator.validateToken(token);
                
                // 5. The token is valid! Now we extract the payload data we embedded inside it.
                String userId = claims.getSubject();
                // We stored roles as a List when generating the token, so we retrieve it as a List.
                List<String> roles = claims.get("roles", List.class);
                
                if (roles != null) {
                    // 6. Spring Security requires roles/permissions to be represented as "GrantedAuthorities".
                    // We map our simple string roles (e.g., "ROLE_USER") into SimpleGrantedAuthority objects.
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // 7. Create an Authentication token. This is Spring Security's internal way of representing a logged-in user.
                    // We pass the userId as the "principal" (who they are), null for credentials (passwords aren't stored in JWTs), and their authorities.
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    // 8. CRITICAL STEP: We place this authentication token into the SecurityContextHolder.
                    // This tells the rest of the application (like our Controllers) that this request is authenticated and who is making it.
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    request.setAttribute("user-id", userId);
                }
            } catch (Exception e) {
                // If ANY validation fails (e.g., expired token, forged signature), we catch the exception.
                // We clear the Security Context to ensure Spring Security treats this request as anonymous/unauthenticated.
                SecurityContextHolder.clearContext();
            }
        }

        // 9. Proceed to the next filter in the chain. 
        // If we set the SecurityContext, the request proceeds as an authenticated user.
        // If we didn't, the request proceeds as an anonymous user (and will likely be blocked by SecurityConfig rules).
        filterChain.doFilter(request, response);
    }
}

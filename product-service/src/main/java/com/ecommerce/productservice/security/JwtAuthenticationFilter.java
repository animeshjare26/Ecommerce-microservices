package com.ecommerce.productservice.security;

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
 * JwtAuthenticationFilter (The Warehouse Door Guard)
 * 
 * WHY THIS EXISTS:
 * Even though the API Gateway is the front door to the building, each specific microservice 
 * (like this Product Service) still has a guard at its door. This filter runs on EVERY incoming request. 
 * It takes the "Authorization: Bearer <token>" header, hands the token to the JwtValidator to check 
 * if it's real, and if it is, tells Spring Security "This person is allowed in!"
 *
 * <h2>What this filter does</h2>
 * As part of the Spring Security Filter Chain, this class:
 * <ol>
 *   <li>Intercepts every incoming HTTP request before it reaches the Controller.</li>
 *   <li>Extracts the HTTP {@code Authorization} header.</li>
 *   <li>Validates the JWT cryptographically via {@link JwtValidator}.</li>
 *   <li>Parses the roles and user ID from the claims.</li>
 *   <li>Constructs a {@link UsernamePasswordAuthenticationToken} and populates the {@link SecurityContextHolder}.</li>
 * </ol>
 *
 * <h2>Why {@code OncePerRequestFilter}?</h2>
 * <p>
 * Standard Servlet filters can sometimes be invoked multiple times for a single request (e.g., during internal 
 * async dispatches or forwards). Extending {@code OncePerRequestFilter} guarantees this token validation 
 * logic executes exactly <b>once</b> per HTTP request, preventing redundant cryptographic operations.
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    public JwtAuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        /*
         * EXECUTION FLOW:
         * 1. Extract the Authorization header.
         * 2. Ensure it strictly adheres to the "Bearer <token>" scheme.
         */
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                /*
                 * 3. Validate Token
                 * Calls the JwtValidator. If the token is expired, tampered with, or malformed, 
                 * an exception (e.g., ExpiredJwtException, SignatureException) is thrown immediately,
                 * dropping down to the catch block.
                 */
                Claims claims = jwtValidator.validateToken(token);

                /*
                 * 4. Extract Claims
                 * The subject typically holds the unique User ID. The "roles" claim holds the 
                 * JSON array of authorities (e.g., ["ROLE_ADMIN"]).
                 */
                String userId = claims.getSubject();
                List<String> roles = claims.get("roles", List.class);
                
                if (roles != null) {
                    /*
                     * 5. Map to GrantedAuthorities
                     * Spring Security requires permissions to be wrapped in GrantedAuthority objects.
                     */
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    /*
                     * 6. Create Authentication Token
                     * We use UsernamePasswordAuthenticationToken. We pass `null` for credentials because 
                     * the JWT itself *is* the proof of authentication; we don't hold the user's password here.
                     */
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);

                    /*
                     * 7. Populate SecurityContext
                     * This ThreadLocal context tells all downstream Spring components (like @PreAuthorize)
                     * exactly who is making the request.
                     */
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                /*
                 * 8. Handle Failures
                 * If validation fails, we clear the context to ensure absolutely no residual authentication 
                 * leaks into the thread. We do NOT throw the exception back to the servlet container here. 
                 * We simply let the filter chain continue unauthenticated. The subsequent 
                 * AuthorizationFilter will see an empty context and return a clean 401 Unauthorized.
                 */
                SecurityContextHolder.clearContext();
            }
        }

        /*
         * 9. Continue Chain
         * Pass the request and response to the next filter in the pipeline.
         */
        filterChain.doFilter(request, response);
    }
}

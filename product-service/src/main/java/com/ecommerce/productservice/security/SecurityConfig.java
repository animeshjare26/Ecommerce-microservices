package com.ecommerce.productservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig (The Building Blueprints / Security Perimeter)
 * 
 * WHY THIS EXISTS:
 * Just like in the Auth Service, this file tells Spring Security exactly how to protect this 
 * specific warehouse (Product Service). It says: "Nobody gets in unless they have been checked 
 * by the JwtAuthenticationFilter (The Warehouse Door Guard)."
 * It also disables traditional session cookies because microservices must be stateless.
 *
 * Main Spring Security configuration for the product-service.
 * This configures the rules for which endpoints are secured and injects our custom JWT filter.
 */
@Configuration
@EnableWebSecurity // Enables Spring Security web security features
@EnableMethodSecurity // Allows us to use @PreAuthorize("hasRole('ADMIN')") on specific controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF (Cross-Site Request Forgery) protection.
            // CSRF is not necessary for stateless APIs where we use Bearer tokens instead of cookies.
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Configure endpoint authorization rules.
            .authorizeHttpRequests(auth -> auth
                // Allow anyone to fetch products (e.g., a GET request to /api/products could be open)
                // You can customize this based on your exact routes.
                // .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                
                // For this example, let's say ALL requests must be authenticated
                .anyRequest().authenticated()
            )
            
            // 3. Configure Session Management to be STATELESS.
            // This is crucial for microservices! It tells Spring not to create a standard HTTP session/cookie.
            // Every request MUST carry its own token.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 4. Inject our custom filter.
            // We tell Spring Security to run our JwtAuthenticationFilter BEFORE the standard 
            // UsernamePasswordAuthenticationFilter. This ensures our token validation logic runs first.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Build and return the configured security chain.
        return http.build();
    }
}

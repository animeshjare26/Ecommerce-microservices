package com.ecommerce.userservice.security;

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
 * specific HR department (User Service). It says: "Nobody gets in unless they have been checked 
 * by the JwtAuthenticationFilter (The HR Door Guard)."
 * It also disables traditional session cookies because microservices must be stateless.
 * 
 * WHY THIS EXISTS:
 * By default, Spring Security tries to protect apps using Login Forms and Cookies (Sessions).
 * But in a Microservices architecture, we don't have sessions! We use stateless JWTs.
 * This class completely overrides Spring Security's default behavior to fit our architecture.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Allows us to use @PreAuthorize("hasRole('ADMIN')") on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF (Cross-Site Request Forgery) protection.
            // CSRF protection is meant for browsers storing session cookies. 
            // Since we use Bearer tokens passed in headers, CSRF attacks are impossible.
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Define URL access rules
            .authorizeHttpRequests(auth -> auth
                // Force every single request to this microservice to be authenticated.
                .anyRequest().authenticated()
            )
            
            // 3. Make it Stateless
            // Tell Spring NEVER to create an HttpSession in memory. 
            // This is vital for scalability. If a user hits instance A on request 1, 
            // and instance B on request 2, it doesn't matter because the JWT contains all the state!
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 4. Inject our custom JWT filter
            // We insert our JwtAuthenticationFilter BEFORE Spring's default UsernamePassword filter.
            // This ensures our token validation logic is the very first thing that runs.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

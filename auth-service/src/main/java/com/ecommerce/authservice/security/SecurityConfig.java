package com.ecommerce.authservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 1. SecurityConfig (The Building Blueprints / Security Perimeter)
 * 
 * GUIDE: CONFIGURING SPRING SECURITY
 * -----------------------------------------
 * This file completely overrides Spring Security's default behavior.
 * 
 * WHY THIS EXISTS:
 * Out of the box, Spring Security wants to protect every single endpoint and uses 
 * session cookies to track logged-in users. In a Microservices architecture, this 
 * is bad for two reasons:
 * 1. We need some endpoints to be public (how else can a new user register or login?).
 * 2. Microservices must be STATELESS (no sessions). We use JWTs instead.
 * 
 * This class configures the Security Filter Chain, which is a series of gates that 
 * every incoming HTTP request must pass through before it reaches our Controllers.
 */
@Configuration
@EnableWebSecurity // Turns on Spring Security's web security support
@EnableMethodSecurity // Allows us to use annotations like @PreAuthorize on our controllers
@RequiredArgsConstructor
public class SecurityConfig {

    // Our custom filter that checks for JWTs on incoming requests
    private final JwtAuthenticationFilter jwtAuthFilter;
    
    // Our custom handler for what to do when an unauthenticated user gets rejected
    private final AuthEntryPointJwt authEntryPointJwt;

    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    /**
     * This is the core configuration method. It defines the rules for the Security Filter Chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CORS Setup: Tells the browser it's okay for our frontend (React/Angular) to call this API.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. CSRF Disable: Cross-Site Request Forgery protection relies on Sessions. 
            // Since we are stateless and use JWTs, we don't need CSRF protection.
            .csrf(AbstractHttpConfigurer::disable)
            
            // 3. Exception Handling: If a request fails authentication, route it to our custom EntryPoint.
            // This ensures they get a nice JSON error message instead of an ugly default HTML page.
            .exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPointJwt))
            
            // Register our AuthenticationProvider
            .authenticationProvider(authenticationProvider())

            // 4. URL Access Rules: The most important part!
            .authorizeHttpRequests(auth -> auth
                // EXTREMELY IMPORTANT: Allow public, unauthenticated access to the login and registration endpoints.
                .requestMatchers("/auth/login", "/auth/register").permitAll()

                // Internal/ops endpoints (e.g. /auth/internal/reconcile) bypass the JWT filter because
                // they are not called by end users. They are NOT actually open, though: the controller
                // enforces the shared "X-Internal-Key" header itself before doing any work.
                .requestMatchers("/auth/internal/**").permitAll()

                // Force every OTHER request hitting this microservice to have a valid JWT.
                .anyRequest().authenticated()
            )
            
            // 5. Make it Stateless: Tell Spring Security NOT to create HTTP Sessions. 
            // Every single request must carry its own JWT to prove who it is.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 6. Inject Custom Filter: Add our JwtAuthenticationFilter into the chain BEFORE 
            // the standard UsernamePasswordAuthenticationFilter. This means we process the JWT 
            // *before* Spring Security tries to do its default username/password check.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures the AuthenticationProvider to use our custom UserDetailsService 
     * and BCrypt Password Encoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Exposes the AuthenticationManager so we can use it in our AuthServiceImpl.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines the Cross-Origin Resource Sharing (CORS) rules.
     * This allows web browsers running on different domains to make requests to our API.
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        // Allow requests from any origin (*). In production, this should be restricted to your frontend domain.
        corsConfiguration.setAllowedOriginPatterns(List.of("*"));
        
        // Allow standard HTTP methods.
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers to be sent in the request.
        corsConfiguration.setAllowedHeaders(List.of("*"));
        
        // Allow cookies or authorization headers to be included.
        corsConfiguration.setAllowCredentials(true); 
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply these rules to all endpoints (/**).
        source.registerCorsConfiguration("/**", corsConfiguration);

        return source;
    }
}

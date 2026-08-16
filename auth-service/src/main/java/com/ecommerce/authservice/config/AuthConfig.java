package com.ecommerce.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 10. AuthConfig (Bean Factory)
 * 
 * INTERN GUIDE: SPRING BEANS
 * --------------------------
 * Configuration class for Authentication-related beans.
 * 
 * WHY THIS EXISTS:
 * Spring's Dependency Injection framework needs to know HOW to create certain objects.
 * When our `AuthServiceImpl` has a field `private final PasswordEncoder passwordEncoder`,
 * Spring looks through classes annotated with @Configuration to find a @Bean method 
 * that returns a PasswordEncoder. 
 * 
 * WHY BCRYPT:
 * BCrypt is a standard cryptographic hash function. It automatically incorporates a "salt" 
 * (random data) into the hash. This defends against rainbow table attacks. Even if two 
 * users have the exact same password "password123", their BCrypt hashes stored in the 
 * database will look completely different!
 */
@Configuration
public class AuthConfig {

    @Bean // Tells Spring: "Run this method at startup, and keep the returned object in the Application Context."
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

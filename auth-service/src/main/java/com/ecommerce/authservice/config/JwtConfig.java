package com.ecommerce.authservice.config;

import com.ecommerce.authservice.enums.TokenType;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 2. JwtConfig (The Rulebook for JWTs)

 * ------------------------------------
 * This class automatically reads configuration from your application.yml file.
 * In your application.yml, you have a section that looks like this:
 * 
 * jwt:
 *   access:
 *     secret: my-super-secret-key
 *     expiration: 15
 *   refresh:
 *     secret: my-refresh-secret
 *     expiration: 1440
 * 
 * How does Spring Boot know to put those YAML values into this Java class?
 * 1. @Configuration tells Spring to manage this class as a "Bean" (an object Spring creates for you).
 * 2. @ConfigurationProperties(prefix = "jwt") is the magic spell! It tells Spring: 
 *    "Go look in the YAML file for a top-level key called 'jwt'."
 * 
 * 3. Inside 'jwt', there are two nested keys: 'access' and 'refresh'. 
 *    Notice that in this Java class, we have two fields named EXACTLY 'access' and 'refresh'.
 *    Because the names match, Spring knows to map the YAML 'access' block to the 'access' field, 
 *    and the YAML 'refresh' block to the 'refresh' field.
 * 
 * 4. The fields 'access' and 'refresh' are of type `TokenConfig`. 
 *    Inside `TokenConfig`, we have fields called `secret` and `expiration`. 
 *    These match the lowest level keys in the YAML perfectly! 
 *    
 * 5. @Data (from Lombok) automatically generates the "getters" and "setters" 
 *    (e.g., setSecret(), setExpiration()) that Spring uses behind the scenes 
 *    to inject those YAML values into this object.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    // Spring sees 'jwt.refresh' in YAML and uses this field.
    private TokenConfig refresh;
    
    // Spring sees 'jwt.access' in YAML and uses this field.
    private TokenConfig access;

    // This map is just a handy way for our code to fetch the right config 
    // based on whether we are dealing with an ACCESS token or REFRESH token.
    private Map<TokenType, TokenConfig> tokenConfigMap;

    /**
     * @PostConstruct means "run this method right AFTER Spring has finished 
     * injecting the values from the YAML file."
     * 
     * We use it here to neatly package our 'refresh' and 'access' configs 
     * into a HashMap, making it easy to retrieve them later using the enum.
     */
    @PostConstruct
    private void init() {
        tokenConfigMap = new HashMap<>();
        tokenConfigMap.put(TokenType.REFRESH_TOKEN, refresh);
        tokenConfigMap.put(TokenType.ACCESS_TOKEN, access);
    }

    /**
     * A helper method for other classes (like JwtService) to easily grab the 
     * right configuration (secret and expiration) when generating a token.
     */
    public TokenConfig getTokenConfigByType(TokenType tokenType) {
        return tokenConfigMap.get(tokenType);
    }

    /**
     * This inner class represents the exact structure of the nested YAML.
     * In YAML:
     *   secret: ...
     *   expiration: ...
     */
    @Data
    public static class TokenConfig {
        // Maps to jwt.access.secret OR jwt.refresh.secret
        private String secret;
        
        // Maps to jwt.access.expiration OR jwt.refresh.expiration (in minutes)
        private long expiration; 
    }
}

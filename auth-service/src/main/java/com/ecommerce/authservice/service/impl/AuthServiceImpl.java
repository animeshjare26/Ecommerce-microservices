package com.ecommerce.authservice.service.impl;

import com.ecommerce.authservice.dtos.response.AuthResponse;
import com.ecommerce.authservice.dtos.request.LoginRequest;
import com.ecommerce.authservice.dtos.request.RegisterRequest;
import com.ecommerce.authservice.dtos.request.TokenRequest;
import com.ecommerce.authservice.models.AuthUser;
import com.ecommerce.authservice.models.Permission;
import com.ecommerce.authservice.models.Role;
import com.ecommerce.authservice.enums.TokenType;
import com.ecommerce.authservice.repository.AuthUserRepository;
import com.ecommerce.authservice.repository.RoleRepository;
import com.ecommerce.authservice.security.JwtUtils;
import com.ecommerce.authservice.security.UserDetailsImpl;
import com.ecommerce.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;

/**
 * 5. AuthServiceImpl (The Security Guard Worker)
 * 
 * INTERN GUIDE: THE BUSINESS LOGIC
 * --------------------------------
 * This class implements the AuthService interface. It contains the actual 
 * "business logic" (the heavy lifting) for authentication. 
 * 
 * It coordinates three main things:
 * 1. The Database (AuthUserRepository) to look up or save users.
 * 2. The AuthenticationManager to safely verify login credentials.
 * 3. The JwtUtils to print out the ID badges (Tokens).
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // Interfaces injected by Spring (Dependency Injection)
    private final AuthUserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    /**
     * Handles New User Registration
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        // 1. Check if the username already exists to prevent duplicates.
        if (repository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        
        // 2. Create a new User Entity mapping to our database table.
        AuthUser user = new AuthUser();
        user.setUsername(request.getUsername());
        
        // 3. CRITICAL: Never save plain text passwords! We hash it using BCrypt.
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Fetch the default ROLE_USER from the database and assign it
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role not found"));
        user.setUserRoles(List.of(defaultRole));
        
        // 4. Save the user to the database.
        AuthUser saved = repository.save(user);
        
        // 5. Generate and return the JWTs so the user is immediately logged in.
        return toResponse(saved);
    }

    /**
     * Handles User Login via AuthenticationManager
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Delegate the authentication to Spring Security's AuthenticationManager.
        // It will automatically call our UserDetailsServiceImpl to fetch the user and 
        // compare the hashed passwords. If it fails, it throws a BadCredentialsException.
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // 2. If we reach here, the user is authenticated! Extract their details.
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // 3. We need their raw roles string (e.g. "ROLE_USER,CAN_EDIT") to generate the token.
            String roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            // 4. Generate and return the JWTs.
            String accessToken = jwtUtils.generateToken(TokenType.ACCESS_TOKEN, String.valueOf(userDetails.getId()), Map.of("id", UUID.randomUUID(), "username", userDetails.getUsername(), "roles", roles));
            String refreshToken = jwtUtils.generateToken(TokenType.REFRESH_TOKEN, String.valueOf(userDetails.getId()), Map.of("id", UUID.randomUUID(), "username", userDetails.getUsername(), "roles", roles));

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    userDetails.getId(),
                    userDetails.getUsername(),
                    roles);

        } catch (Exception e) {
            // Catch the Spring Security Exception and map it to our custom API Error
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    /**
     * Handles refreshing an expired Access Token using a valid Refresh Token
     */
    @Override
    public Map<TokenType, String> refreshAccessToken(TokenRequest tokenRequest) {
        // 1. Validate the refresh token and extract the user's ID (the subject claim).
        Map<String, String> claims = jwtUtils.getClaimsFromToken(TokenType.REFRESH_TOKEN, tokenRequest.getToken());
        String subject = claims.get("subject");
        
        // 2. Verify the user still exists in the database.
        AuthUser user = repository.findById(Long.parseLong(subject))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        String rolesString = buildRolesString(user);

        // 3. Generate a brand new set of tokens for the user.
        String refreshToken = jwtUtils.generateToken(TokenType.REFRESH_TOKEN, String.valueOf(user.getId()), Map.of("id", UUID.randomUUID(), "username", user.getUsername(), "roles", rolesString));
        String accessToken = jwtUtils.generateToken(TokenType.ACCESS_TOKEN, String.valueOf(user.getId()), Map.of("id", UUID.randomUUID(), "username", user.getUsername(), "roles", rolesString));

        // 4. Return the new tokens to the client.
        return Map.of(
                TokenType.REFRESH_TOKEN, refreshToken,
                TokenType.ACCESS_TOKEN, accessToken
        );
    }

    /**
     * Helper method to generate the AuthResponse payload containing both tokens for Registration.
     */
    private AuthResponse toResponse(AuthUser user) {
        String rolesString = buildRolesString(user);

        // Generate Access Token (short-lived)
        String accessToken = jwtUtils.generateToken(TokenType.ACCESS_TOKEN, String.valueOf(user.getId()), Map.of("id", UUID.randomUUID(), "username", user.getUsername(), "roles", rolesString));
        
        // Generate Refresh Token (long-lived)
        String refreshToken = jwtUtils.generateToken(TokenType.REFRESH_TOKEN, String.valueOf(user.getId()), Map.of("id", UUID.randomUUID(), "username", user.getUsername(), "roles", rolesString));
        
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer", // Standard HTTP Authorization prefix
                user.getId(),
                user.getUsername(),
                rolesString);
    }

    /**
     * Flattens the complex Role/Permission entity tree into a single comma-separated string 
     * to embed into the JWT.
     */
    private String buildRolesString(AuthUser user) {
        List<String> authorities = new ArrayList<>();
        if (user.getUserRoles() != null) {
            for (Role role : user.getUserRoles()) {
                authorities.add(role.getName());
                if (role.getRolePermissions() != null) {
                    for (Permission p : role.getRolePermissions()) {
                        authorities.add(p.getName());
                    }
                }
            }
        }
        return String.join(",", authorities);
    }
}

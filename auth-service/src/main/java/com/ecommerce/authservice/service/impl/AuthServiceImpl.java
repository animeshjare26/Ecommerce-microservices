package com.ecommerce.authservice.service.impl;

import com.ecommerce.authservice.dtos.response.AuthResponse;
import com.ecommerce.authservice.dtos.request.LoginRequest;
import com.ecommerce.authservice.dtos.request.RegisterRequest;
import com.ecommerce.authservice.dtos.request.TokenRequest;
import com.ecommerce.authservice.feign.ProvisionProfileRequest;
import com.ecommerce.authservice.feign.UserProfileClient;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j // Lombok: gives us a ready-to-use `log` object for structured logging (info/warn/error).
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // Interfaces injected by Spring (Dependency Injection)
    private final AuthUserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    // Feign client used to create the matching profile row in the user-service after we create an
    // identity here. This is the write side of the auth -> user data-sync feature.
    private final UserProfileClient userProfileClient;

    /**
     * Handles New User Registration
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        // 1. Check if the username already exists to prevent duplicates.
        if (repository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        // 1b. Email is now a unique identity field, so reject duplicate emails too. We check this
        //     up front to return a clean 409 CONFLICT instead of letting the DB throw a raw
        //     unique-constraint error on save.
        if (repository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        // 2. Create a new User Entity mapping to our database table.
        AuthUser user = new AuthUser();
        user.setUsername(request.getUsername());

        // 2b. Store the email on the identity (auth-service is the source of truth for email).
        user.setEmail(request.getEmail());

        // 3. CRITICAL: Never save plain text passwords! We hash it using BCrypt.
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Fetch the default ROLE_USER from the database and assign it
        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role not found"));
        user.setUserRoles(List.of(defaultRole));

        // 4. Save the user to the database. After this line, `saved.getId()` holds the database-
        //    generated id. THIS id is the value we propagate everywhere: it becomes the JWT subject
        //    AND the primary key of the user-service profile, keeping the two services correlated.
        AuthUser saved = repository.save(user);

        // 4b. SYNC THE PROFILE: tell the user-service to create the matching profile row.
        //     We pass `saved.getId()` so the profile's primary key equals this identity's id.
        //     We do this AFTER the identity is safely persisted, so we never create a profile for
        //     an identity that failed to save.
        provisionProfileSafely(saved, request.getName(), request.getAddress());

        // 5. Generate and return the JWTs so the user is immediately logged in.
        return toResponse(saved);
    }

    /**
     * Sends a profile-creation request to the user-service, WITHOUT letting a failure there break
     * registration.
     *
     * WHY WE SWALLOW THE ERROR (graceful degradation):
     * Creating the identity and creating the profile live in two different databases, so they cannot
     * share one transaction (this is the classic "dual write" problem). We make a deliberate choice:
     * the IDENTITY is the critical part of registration and has already been committed, so the user
     * can log in immediately. If the profile call fails (user-service down, network blip), we do NOT
     * fail the whole registration and throw away a perfectly good account. Instead we log it loudly
     * and rely on {@link #reconcileProfiles()} to create the missing profile later. Because the
     * user-service treats provisioning as an idempotent upsert (keyed on id), that retry is safe.
     *
     * NOTE (Phase 2 evolution): the robust long-term design replaces this direct call with an event
     * published via a transactional outbox, so delivery is guaranteed rather than best-effort. This
     * synchronous version is Phase 1: it works with zero extra infrastructure.
     */
    private void provisionProfileSafely(AuthUser saved, String name, String address) {
        try {
            userProfileClient.provisionProfile(
                    ProvisionProfileRequest.builder()
                            .id(saved.getId())      // correlation key: profile PK == auth user id
                            .email(saved.getEmail())
                            .name(name)
                            .address(address)
                            .build());
            log.info("Provisioned user-service profile for authUserId={}", saved.getId());
        } catch (Exception ex) {
            // Best-effort: registration still succeeds; reconciliation will heal the missing profile.
            log.error("Failed to provision profile for authUserId={}. It will be retried by reconciliation. Cause: {}",
                    saved.getId(), ex.getMessage());
        }
    }

    /**
     * See {@link AuthService#reconcileProfiles()} for the "why".
     *
     * HOW IT WORKS:
     * We load every active identity and re-send a provisioning request for each. The user-service
     * upserts by id, so identities that already have a profile are left unchanged and only the
     * missing ones get created. We count and return how many we attempted.
     *
     * PRODUCTION NOTE: `findAll()` loads every row into memory. That is fine for this project's
     * scale, but at millions of users you would page through the results (Pageable) instead.
     */
    @Override
    public int reconcileProfiles() {
        List<AuthUser> allUsers = repository.findAll();
        for (AuthUser user : allUsers) {
            // We do not have the profile's name/address here (they live in the user-service), so we
            // send only the identity-owned fields. The user-service upsert preserves any existing
            // profile fields it already has and simply ensures a row exists with the correct id/email.
            provisionProfileSafely(user, null, null);
        }
        log.info("Reconciliation complete: re-sent provisioning for {} identities", allUsers.size());
        return allUsers.size();
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

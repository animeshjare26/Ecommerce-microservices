package com.ecommerce.authservice.controllers;

import com.ecommerce.authservice.dtos.response.AuthResponse;
import com.ecommerce.authservice.dtos.request.LoginRequest;
import com.ecommerce.authservice.dtos.request.RegisterRequest;
import com.ecommerce.authservice.service.AuthService;
import com.ecommerce.authservice.utils.GenericResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 6. AuthController (The Front Desk)
 * 
 * INTERN GUIDE: HOW CONTROLLERS WORK
 * ----------------------------------
 * Controllers are the entry points to the application. They don't do any heavy logic 
 * themselves. Instead, they act like a receptionist:
 * 1. They listen for HTTP requests (GET, POST, etc.) on specific URLs.
 * 2. They take the incoming JSON body and convert it to Java objects (DTOs).
 * 3. They validate that the input is correct (using @Valid).
 * 4. They pass the data to the Service Layer (AuthService) to do the actual work.
 * 5. They take the result from the Service Layer and package it into an HTTP Response.
 * 
 * ARCHITECTURE NOTE: The API Gateway automatically routes all requests starting 
 * with "/auth/**" to this specific microservice.
 */
@RestController // Tells Spring this class handles REST API requests and automatically serializes returns into JSON.
@RequestMapping("/auth") // All endpoints in this controller will start with "/auth".
@RequiredArgsConstructor
public class AuthController {

    // We inject the Interface (not the Impl) to maintain loose coupling.
    private final AuthService authService;

    // Shared secret guarding internal/ops-only endpoints (see reconcile below). Injected from
    // the INTERNAL_API_KEY env var. Must match the value used across our services.
    @Value("${internal.api-key}")
    private String internalApiKey;

    /**
     * POST /auth/register
     * Endpoint to create a new user account.
     * 
     * @param request the registration details. The @Valid annotation ensures that 
     *                the payload meets the constraints (e.g., password length) defined in RegisterRequest.
     */
    @PostMapping("/register")
    public ResponseEntity<GenericResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        // Delegates the actual business logic to the AuthService
        AuthResponse response = authService.register(request);
        
        // Wraps the response in our GenericResponse format and returns a 201 Created HTTP status.
        return ResponseEntity.status(HttpStatus.CREATED).body(GenericResponse.success(response));
    }

    /**
     * POST /auth/login
     * Endpoint to log in an existing user.
     */
    @PostMapping("/login")
    public ResponseEntity<GenericResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        // Delegates the validation and token generation to the AuthService
        AuthResponse response = authService.login(request);
        
        // Returns a 200 OK HTTP status with the token payload.
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    /**
     * POST /auth/refresh
     * Endpoint to get a new access token using a valid, unexpired refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<GenericResponse<java.util.Map<com.ecommerce.authservice.enums.TokenType, String>>> refresh(@Valid @RequestBody com.ecommerce.authservice.dtos.request.TokenRequest request) {
        // Asks the service to validate the old token and generate new ones.
        java.util.Map<com.ecommerce.authservice.enums.TokenType, String> tokens = authService.refreshAccessToken(request);

        return ResponseEntity.ok(GenericResponse.success(tokens));
    }

    /**
     * POST /auth/internal/reconcile
     * Ops/maintenance endpoint that re-syncs the user-service with every identity here.
     *
     * WHEN YOU'D CALL THIS:
     * - After the user-service was down during some registrations (to create the profiles that
     *   were missed), or
     * - Once, right after deploying this feature, to backfill profiles for pre-existing accounts.
     *
     * SECURITY:
     * This is NOT a public endpoint. It is protected by the shared "X-Internal-Key" header rather
     * than a user JWT, because it is triggered by operators/automation, not end users. We compare
     * the header to our configured secret and reject mismatches with 403 FORBIDDEN. (In a hardened
     * setup this would instead require an ADMIN token or be exposed only on an internal management
     * port.)
     *
     * @param providedKey the value of the "X-Internal-Key" request header.
     */
    @PostMapping("/internal/reconcile")
    public ResponseEntity<GenericResponse<Integer>> reconcile(
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey) {

        // Constant-ish equality check on the shared secret. If it doesn't match, refuse the call.
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal key");
        }

        int count = authService.reconcileProfiles();
        return ResponseEntity.ok(GenericResponse.success(count, "Reconciliation triggered for " + count + " identities"));
    }
}

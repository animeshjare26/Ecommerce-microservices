package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.ProvisionProfileRequest;
import com.ecommerce.userservice.dto.UserResponseDto;
import com.ecommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * InternalUserController (The Staff-Only Back Door)
 *
 * WHY THIS EXISTS (SEPARATE FROM UserController):
 * We split "internal" service-to-service endpoints away from the public UserController on purpose,
 * so the security model is obvious at a glance: everything under "/users/internal/**" is meant to
 * be called by OTHER SERVICES (currently the auth-service), never by end users or the public web.
 *
 * HOW IT IS PROTECTED:
 * These endpoints are excluded from the normal JWT check (see SecurityConfig) because the caller is
 * a service, not a logged-in user with a JWT. Instead, we require a shared secret in the
 * "X-Internal-Key" header and verify it here before doing anything. A missing/wrong key is rejected
 * with 403 FORBIDDEN. (Hardening path for production: mTLS or signed service tokens + network policy
 * so this port isn't reachable from outside the cluster.)
 */
@RestController
@RequestMapping("/users/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    // The shared secret, injected from the INTERNAL_API_KEY env var (internal.api-key). It MUST
    // match the key the auth-service stamps onto its outgoing Feign calls.
    @Value("${internal.api-key}")
    private String internalApiKey;

    /**
     * POST /users/internal/provision
     * Creates (or idempotently updates) the profile that mirrors an auth-service identity.
     *
     * CALLED BY: auth-service's UserProfileClient, immediately after it creates a new AuthUser, and
     * again by the reconciliation job to backfill any missing profiles.
     *
     * @param providedKey the "X-Internal-Key" header carrying the shared secret.
     * @param request     the profile to provision, including the id to use as the primary key.
     * @return the resulting profile (201-style creation is not distinguished from update here since
     *         the operation is an idempotent upsert).
     */
    @PostMapping("/provision")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDto provision(
            @RequestHeader(value = "X-Internal-Key", required = false) String providedKey,
            @RequestBody ProvisionProfileRequest request) {

        // Gatekeeping: verify the shared secret BEFORE touching the database.
        verifyInternalKey(providedKey);

        return userService.provisionProfile(request);
    }

    /**
     * Rejects the request with 403 FORBIDDEN unless the provided key matches our configured secret.
     * Kept as a small private helper so every future internal endpoint can reuse the exact same guard.
     */
    private void verifyInternalKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal key");
        }
    }
}

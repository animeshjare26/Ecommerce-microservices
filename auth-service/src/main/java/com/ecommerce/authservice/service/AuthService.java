package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dtos.response.AuthResponse;
import com.ecommerce.authservice.dtos.request.LoginRequest;
import com.ecommerce.authservice.dtos.request.RegisterRequest;
import com.ecommerce.authservice.dtos.request.TokenRequest;
import com.ecommerce.authservice.enums.TokenType;
import java.util.Map;

/**
 * 4. AuthService (The Security Guard Job Description)
 * 
 * INTERN GUIDE: INTERFACES vs IMPLEMENTATIONS
 * -------------------------------------------
 * This file is just an Interface. It has no actual code/logic inside its methods.
 * 
 * WHY WE DO THIS:
 * It's a core Java and Spring Boot best practice (Loose Coupling). 
 * This interface acts as a formal contract or "Job Description". 
 * It says: "Whoever takes the role of AuthService MUST know how to do exactly 
 * these three things: register, login, and refresh tokens."
 * 
 * By doing this, our AuthController can talk to this Interface without knowing 
 * or caring how the job is actually done. If we ever want to switch from saving 
 * users in a Database to saving them in AWS Cognito, we just create a new class 
 * that implements this interface, and we don't have to touch the AuthController!
 */
public interface AuthService {
    
    /**
     * Creates a new user account.
     * @param request The user's desired username and password.
     * @return AuthResponse containing the newly minted JWTs.
     */
    AuthResponse register(RegisterRequest request);
    
    /**
     * Verifies an existing user's credentials.
     * @param request The user's username and password.
     * @return AuthResponse containing the newly minted JWTs.
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * Issues new access/refresh tokens using a valid unexpired refresh token.
     * @param tokenRequest The request containing the user's current refresh token.
     * @return A map containing the new Access and Refresh tokens.
     */
    Map<TokenType, String> refreshAccessToken(TokenRequest tokenRequest);

    /**
     * Re-synchronises the user-service with this service's identities (the "safety net").
     *
     * WHY THIS EXISTS:
     * Profile provisioning during registration is a "dual write" (we write to the auth DB and then
     * make a network call to the user-service). Network calls can fail. If the user-service is
     * momentarily down when someone registers, that person ends up with an identity but no profile.
     * This method walks every active identity and re-sends a provisioning request for each, so the
     * user-service can (idempotently) create any profiles it is missing. It is also how we backfill
     * profiles for accounts that existed before this feature was added.
     *
     * @return the number of identities that were re-sent for provisioning.
     */
    int reconcileProfiles();
}

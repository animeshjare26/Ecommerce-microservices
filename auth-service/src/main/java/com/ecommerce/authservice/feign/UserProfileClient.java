package com.ecommerce.authservice.feign;

import com.ecommerce.authservice.config.FeignInternalAuthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * UserProfileClient (The Auth-Service's Phone Line to the User-Service)
 *
 * WHY THIS EXISTS:
 * After creating an identity, the auth-service must ask the user-service to create the matching
 * profile row. Rather than writing verbose HTTP client code (RestTemplate/WebClient), we declare
 * this interface and Spring Cloud OpenFeign generates the real HTTP-calling implementation for us
 * at runtime. We just call `userProfileClient.provisionProfile(...)` like a normal Java method.
 *
 * HOW THE ADDRESSING WORKS (name = "USER-SERVICE"):
 * We do NOT hardcode "http://localhost:8081". Feign asks Eureka "where is USER-SERVICE?" and
 * Eureka returns a live instance's address, load-balancing across instances if there are several.
 * This is the same discovery mechanism the order-service uses for its Feign clients.
 *
 * HOW IT STAYS SECURE (configuration = FeignInternalAuthConfig.class):
 * The endpoint we call is private. This configuration attaches the shared "X-Internal-Key" header
 * to every request (see FeignInternalAuthConfig), so the user-service can trust the call really
 * came from another one of our services and not from the public internet.
 */
@FeignClient(
        name = "USER-SERVICE",                       // Eureka logical service id to route to.
        configuration = FeignInternalAuthConfig.class // Attach the internal-key header to each call.
)
public interface UserProfileClient {

    /**
     * Creates (or idempotently updates) the profile that mirrors a freshly created identity.
     *
     * Executes: POST http://USER-SERVICE/users/internal/provision   with the JSON body below.
     *
     * IDEMPOTENCY CONTRACT:
     * The user-service treats this as an UPSERT keyed on `request.id`. That means calling it twice
     * with the same id is safe and does not create duplicates. This matters because our failure
     * handling (and the reconciliation job) may retry provisioning.
     *
     * @param request the profile to create, including the auth user's id used as the profile's PK.
     */
    @PostMapping("/users/internal/provision")
    void provisionProfile(@RequestBody ProvisionProfileRequest request);
}

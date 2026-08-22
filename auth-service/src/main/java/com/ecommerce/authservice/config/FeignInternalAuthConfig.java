package com.ecommerce.authservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * FeignInternalAuthConfig (The "Staff Pass" Stamper for outgoing internal calls)
 *
 * WHY THIS EXISTS:
 * The auth-service calls a PRIVATE endpoint on the user-service (`POST /users/internal/provision`)
 * to create profiles. That endpoint must NOT be callable by the public internet — otherwise
 * anyone could forge profiles. We protect it with a shared secret header, "X-Internal-Key".
 *
 * WHY A SHARED HEADER KEY AND NOT A JWT:
 * This provisioning call happens DURING registration, before the end user has any JWT. Minting a
 * service JWT here is also awkward because our downstream JWT validators derive their signing key
 * differently than the issuer (a known quirk in this codebase). A simple pre-shared internal key
 * sidesteps all of that: it is easy to reason about, easy to rotate via an env var, and never
 * leaves our private network. (In a hardened production setup you would graduate this to mTLS or
 * signed service tokens — see the Phase 2 plan.)
 *
 * HOW IT WORKS:
 * A Feign `RequestInterceptor` is a hook that runs just before every outgoing request made by any
 * Feign client that is configured with this class. Here we attach the internal key header to every
 * such request automatically, so the calling code in AuthServiceImpl doesn't have to remember to
 * add it each time.
 *
 * NOTE: This class is intentionally NOT annotated with @Configuration. If it were component-scanned
 * globally it would apply to ALL Feign clients. Instead we wire it into ONLY the UserProfileClient
 * via `@FeignClient(configuration = FeignInternalAuthConfig.class)`, keeping its effect scoped.
 */
public class FeignInternalAuthConfig {

    // The shared secret, injected from configuration (env var INTERNAL_API_KEY -> internal.api-key).
    // It MUST match the value the user-service checks for on the receiving end.
    @Value("${internal.api-key}")
    private String internalApiKey;

    /**
     * Registers the interceptor bean. For every outgoing request on the associated Feign client,
     * Spring calls this lambda and we stamp on the "X-Internal-Key" header.
     */
    @Bean
    public RequestInterceptor internalApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header("X-Internal-Key", internalApiKey);
    }
}

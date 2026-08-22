package com.ecommerce.authservice.feign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProvisionProfileRequest (The "Create-This-Profile" Order Form)
 *
 * WHY THIS EXISTS:
 * When the auth-service finishes creating a new identity (an AuthUser), it needs to tell the
 * user-service: "Please create the matching profile row for this person." This class is the
 * exact JSON body we send on that internal service-to-service call.
 *
 * WHY A DEDICATED DTO (instead of reusing an entity or AuthUser):
 * - The auth-service must NOT leak auth-only data (like the password hash) to other services.
 *   This DTO only carries the fields the user-service is allowed to know about.
 * - It is a stable "contract" between the two services. If the AuthUser entity changes
 *   internally, this wire format stays the same, so we don't accidentally break the user-service.
 *
 * THE MOST IMPORTANT FIELD: `id`
 * This is the auth user's id (the same value that becomes the JWT `subject`). We deliberately
 * send it so the user-service can use it as the PRIMARY KEY of the profile row. That is what
 * keeps the two tables correlated: auth_users.id == users.id for the same person. Every
 * downstream service already identifies the user by this id (from the JWT), so aligning the
 * profile's primary key to it means zero id-translation anywhere in the system.
 */
@Data
@Builder
@NoArgsConstructor  // Lombok: needed so Jackson can deserialize this DTO on the receiving side.
@AllArgsConstructor // Lombok: lets us build it in one line from AuthServiceImpl.
public class ProvisionProfileRequest {

    /** The auth user's id. Becomes the user-service profile's primary key (the correlation key). */
    private Long id;

    /** Replicated copy of the identity email, so the profile can display/contact the user. */
    private String email;

    /** Profile display name supplied at registration (owned by the user-service going forward). */
    private String name;

    /** Shipping/contact address supplied at registration (owned by the user-service going forward). */
    private String address;
}

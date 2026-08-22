package com.ecommerce.userservice.dto;

import lombok.Data;

/**
 * ProvisionProfileRequest (The incoming "Create-This-Profile" Order Form)
 *
 * WHY THIS EXISTS:
 * This is the JSON body the auth-service sends to POST /users/internal/provision when it wants us
 * to create (or idempotently update) the profile that mirrors a newly created identity. It is the
 * receiving-end counterpart of the auth-service's own ProvisionProfileRequest — the two must stay
 * field-compatible because they are the wire contract between the services.
 *
 * WHY IT'S SEPARATE FROM UserRequestDto:
 * UserRequestDto is the PUBLIC form end users submit and it must never let a caller choose their own
 * id. This DTO is INTERNAL (service-to-service) and deliberately DOES carry `id`, because the whole
 * point is that the auth-service dictates the id so the two tables stay correlated.
 */
@Data
public class ProvisionProfileRequest {

    /** The auth user's id. We use this AS the profile's primary key (the correlation key). */
    private Long id;

    /** Replicated copy of the identity email (auth-service is the source of truth). */
    private String email;

    /** Optional display name captured at registration. May be null on reconciliation calls. */
    private String name;

    /** Optional address captured at registration. May be null on reconciliation calls. */
    private String address;
}

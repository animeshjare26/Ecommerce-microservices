package com.ecommerce.authservice.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 13. RegisterRequest (Data Transfer Object)
 * 
 * INTERN GUIDE: CUSTOM VALIDATION RULES
 * -------------------------------------
 * Data Transfer Object (DTO) for incoming Registration requests.
 * 
 * WHY THIS EXISTS:
 * Separating `LoginRequest` and `RegisterRequest` into two different classes allows 
 * us to enforce entirely different validation rules depending on the action!
 * 
 * For instance, when a user registers, we MUST ensure their new password is strong.
 * So we use `@Size(min = 8)` to enforce a minimum length. If they send a 3-character 
 * password, Spring throws a 400 Bad Request before the request even hits our code.
 * During login (LoginRequest), we don't care about password length validation, we 
 * only care if the password matches what is in the database.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    /**
     * The email address for the new account.
     *
     * WHY IT IS HERE NOW:
     * Registration is the moment we create the user's IDENTITY. Because email is an
     * identity/credential field owned by the auth-service (used for login/recovery), it must
     * be supplied and validated right here at sign-up.
     *
     * @Email  -> Spring rejects malformed values (e.g. "not-an-email") with a 400 Bad Request
     *            BEFORE any of our code runs, so the service never has to defend against garbage.
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * ---------------------------------------------------------------------------------------
     * SEED PROFILE FIELDS
     * ---------------------------------------------------------------------------------------
     * The fields below are NOT stored in the auth-service. They exist on this request purely so
     * that a single "/auth/register" call can bootstrap BOTH halves of a user:
     *   1. the identity (username/email/password) -> saved locally in auth_users, and
     *   2. the profile  (name/address)            -> forwarded to the user-service to create the
     *                                                matching profile row (see AuthServiceImpl).
     *
     * This gives the client one clean entry point instead of forcing them to call two services.
     * These fields are optional at the validation layer: a caller may register with credentials
     * only and fill in profile details later via the user-service.
     */
    private String name;

    private String address;
}

package com.ecommerce.userservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User (The HR Employee Record)
 *
 * WHY THIS EXISTS:
 * This class maps to the `users` table in this microservice's PostgreSQL database. It holds the
 * user's PROFILE data (name, contact email, address) — NOT their password. Authentication data is
 * owned exclusively by the auth-service.
 *
 * ARCHITECTURE NOTE — THE ID IS THE CORRELATION KEY (this is the important part):
 * This profile's `id` is NOT auto-generated. Instead, it is set to the SAME value as the auth
 * user's id in the auth-service. That auth id is also the JWT `subject` every service reads. By
 * making the profile share that id, "auth user 42" and "profile 42" are guaranteed to be the same
 * person, and no service ever has to translate between an "auth id" and a "profile id".
 *
 * WHO CREATES THESE ROWS:
 * Not the public. The auth-service creates the row (via the internal /users/internal/provision
 * call) right after it creates the identity, passing the id to use. See InternalUserController.
 */
@Entity // Tells Hibernate: "This class represents a database table."
@Data   // Lombok: generates getters/setters/toString/equals/hashCode.
@Table(name = "users") // Explicit table name.
// SOFT DELETE PATTERN (mirrors the auth-service): when someone "deletes" a user, we do NOT remove
// the row (that would orphan historical orders that reference this user). Instead we flip is_active
// to false with this UPDATE. This keeps order history intact and referential data meaningful.
@SQLDelete(sql = "UPDATE users SET is_active = false WHERE id=?")
// And on every normal query, automatically hide soft-deleted rows by adding this WHERE clause.
@SQLRestriction("is_active = true")
public class User {

    /**
     * Primary key = the auth-service user id (the JWT subject).
     *
     * WHY NO @GeneratedValue:
     * We do NOT let the database auto-increment this. The auth-service decides the id (its own auth
     * user id) and sends it to us during provisioning. If we auto-generated it, the two services'
     * ids would drift apart and the whole correlation would break. So the caller ALWAYS supplies id.
     */
    @Id
    private Long id;

    private String name;

    /**
     * Replicated COPY of the identity email (source of truth is the auth-service).
     *
     * WHY IT'S HERE AND YET NOT EDITABLE FROM HERE:
     * We keep a copy so this service can show/contact the user without a network hop to auth. But
     * because email is an auth-owned credential, our profile-update logic refuses to change it (see
     * UserService.updateProfile); email changes must originate in the auth-service and flow down.
     */
    private String email;

    // Crucial for the e-commerce domain. The Order Service reads this address for shipping.
    private String address;

    // Soft-delete flag used by the @SQLDelete / @SQLRestriction pattern above. Defaults to active.
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Hibernate stamps this once, when the profile row is first inserted.
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Hibernate refreshes this on every update, giving us a free "last modified" audit field.
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

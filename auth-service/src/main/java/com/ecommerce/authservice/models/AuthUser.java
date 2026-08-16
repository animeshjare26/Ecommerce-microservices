package com.ecommerce.authservice.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 7. AuthUser (The Database Entity)
 * 
 * INTERN GUIDE: JPA ENTITIES AND SOFT DELETES
 * -------------------------------------------
 * This class directly maps to the `auth_users` table in the PostgreSQL database.
 * 
 * WHY THIS EXISTS:
 * Instead of writing raw SQL strings to save or retrieve users, we use JPA (Java Persistence API).
 * By annotating this class with @Entity, Spring Data JPA knows exactly how to convert 
 * this Java object into a database row, and vice versa.
 * 
 * ARCHITECTURE NOTE: SEPARATION OF CONCERNS
 * Notice that this entity ONLY contains fields absolutely necessary for Authentication 
 * (username, password, roles). It does NOT contain profile data like "firstName" or "address".
 * That data belongs in the `user-service`. This strict separation is a core principle 
 * of microservices architecture!
 */
@Entity // Tells Hibernate: "This class represents a database table."
@Data // Lombok: Automatically generates Getters (e.g., getUsername()), Setters, and toString() behind the scenes.
@Table(name = "auth_users") // Explicitly names the table in PostgreSQL to avoid naming conflicts.
// SOFT DELETE PATTERN: When we call repository.delete(user), don't actually delete the row. 
// Instead, just run this UPDATE statement to flip the is_active flag.
@SQLDelete(sql = "UPDATE auth_users SET is_active = false WHERE id=?")
// When we query for users, ONLY return rows where is_active is true. (Hides deleted users automatically).
@SQLRestriction("is_active = true")
public class AuthUser {
    
    @Id // Marks this field as the Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tells the database to auto-increment this ID
    private Long id;

    // The username must be unique because it's used for login. nullable=false means it's required.
    @Column(nullable = false, unique = true)
    private String username;

    // SECURITY: This stores the BCrypt hash. NEVER store or log the plain text password.
    @Column(nullable = false)
    private String password; 

    // A user can have many roles, and a role can belong to many users.
    // This automatically creates a join table called `user_roles` in the database.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private java.util.List<Role> userRoles;

    // Used by our Soft Delete pattern above to mark accounts as deactivated.
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Hibernate automatically fills this with the current time when the row is first INSERTED.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Hibernate automatically updates this with the current time whenever the row is UPDATED.
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

package com.ecommerce.authservice.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

/**
 * INTERN GUIDE: ROLE ENTITY
 * -------------------------
 * This entity represents a group of permissions. 
 * 
 * WHY WE ADDED IT:
 * Assigning 50 individual permissions to every single user would be a nightmare.
 * Instead, we group permissions into a "Role" (like "ROLE_MANAGER"). 
 * When we assign "ROLE_MANAGER" to a user, they automatically inherit all the 
 * permissions attached to that role via the `role_permissions` join table.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The name of the role, usually prefixed with "ROLE_", e.g., "ROLE_ADMIN"
    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    // A Role contains many Permissions. 
    // This creates a third table `role_permissions` in the database to link them.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private List<Permission> rolePermissions;
}

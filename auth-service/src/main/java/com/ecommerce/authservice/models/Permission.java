package com.ecommerce.authservice.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * INTERN GUIDE: PERMISSION ENTITY
 * -------------------------------
 * This entity represents a specific, granular action a user can take in the system.
 * 
 * WHY WE ADDED IT:
 * Instead of just saying a user is an "ADMIN", we want to know exactly what an ADMIN 
 * is allowed to do. A Permission might be "CAN_CREATE_PRODUCT" or "CAN_DELETE_USER".
 * This allows for extremely fine-grained security rules on our API endpoints.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The name of the permission, usually all caps, e.g., "CAN_EDIT_ORDER"
    @Column(nullable = false, unique = true)
    private String name;

    // A human-readable description of what this permission actually allows
    @Column
    private String description;
}

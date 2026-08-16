package com.ecommerce.userservice.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * User (The HR Employee Record)
 * 
 * WHY THIS EXISTS:
 * This class directly maps to the `users` table in the PostgreSQL database for this specific microservice.
 * Notice that it contains things like `firstName` and `lastName`, but it does NOT contain 
 * a `password` field. That is a deliberate architectural choice to separate authentication 
 * from user profiles.
 * 
 * ARCHITECTURE NOTE:
 * Notice there is NO "password" field here! 
 * The `auth-service` database owns the password. 
 * This database (user_db) owns the profile data. 
 * This separation ensures that if the user_db is compromised, the attacker does not get passwords.
 */
@Entity
@Data
@Table(name = "users")
public class User {
    
    // The ID. In a fully synchronized microservices architecture, this ID often matches 
    // the ID of the AuthUser in the auth-service database to link them together 1-to-1.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    
    // Crucial for the e-commerce domain. The Order Service will need this address!
    private String address; 
}

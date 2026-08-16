package com.ecommerce.authservice.repository;

import com.ecommerce.authservice.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * INTERN GUIDE: ROLE REPOSITORY
 * -----------------------------
 * This interface lets us query the `roles` table in the database.
 * 
 * WHY WE ADDED IT:
 * When a new user registers, we don't want them to be an ADMIN. We need to fetch 
 * the default "ROLE_USER" from the database and attach it to their account. 
 * This repository allows us to call `findByName("ROLE_USER")`.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}

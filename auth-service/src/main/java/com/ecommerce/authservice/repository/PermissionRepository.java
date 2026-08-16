package com.ecommerce.authservice.repository;

import com.ecommerce.authservice.models.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * INTERN GUIDE: PERMISSION REPOSITORY
 * -----------------------------------
 * This interface lets us query the `permissions` table in the database.
 * 
 * WHY WE ADDED IT:
 * We need this primarily for the `AdminSeeder` to create the initial permissions 
 * in the database when the application first starts up, ensuring the ADMIN role 
 * actually has superpowers!
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
}

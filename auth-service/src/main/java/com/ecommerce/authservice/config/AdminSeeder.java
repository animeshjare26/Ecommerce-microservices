package com.ecommerce.authservice.config;

import com.ecommerce.authservice.models.AuthUser;
import com.ecommerce.authservice.models.Permission;
import com.ecommerce.authservice.models.Role;
import com.ecommerce.authservice.repository.AuthUserRepository;
import com.ecommerce.authservice.repository.PermissionRepository;
import com.ecommerce.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 11. AdminSeeder (The Setup Script)
 * 
 * INTERN GUIDE: DATABASE SEEDING
 * ------------------------------
 * Database Seeder for the initial Admin account and Roles.
 * 
 * WHY THIS EXISTS:
 * When you first deploy this microservice to a fresh database, it is completely empty.
 * Nobody will have the "ROLE_ADMIN" role, and the roles themselves won't exist.
 * This class injects the default Roles, Permissions, and Admin user on startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final AuthUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    // The admin's email. Since email is now a required (non-null, unique) identity field, the seeded
    // admin MUST have one or the insert would fail. We give it a default so existing .env files keep
    // working without a new variable; override it via APP_ADMIN_EMAIL when you want a real address.
    @Value("${app.admin.email:admin@ecommerce.local}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        // 1. Seed Permissions if they don't exist
        Permission allAccess = createPermissionIfNotFound("ALL_ACCESS", "Has access to all features");
        Permission readOnly = createPermissionIfNotFound("READ_ONLY", "Can only view items");
        
        // 2. Seed Roles if they don't exist, linking Permissions to them
        Role adminRole = createRoleIfNotFound("ROLE_ADMIN", "Administrator Role", List.of(allAccess));
        Role userRole = createRoleIfNotFound("ROLE_USER", "Standard User Role", List.of(readOnly));
        
        // 3. Seed Admin User
        if (!userRepository.existsByUsername(adminUsername)) {
            AuthUser admin = new AuthUser();
            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail); // required identity field (see AuthUser.email)
            admin.setPassword(passwordEncoder.encode(adminPassword));

            // Assign both Admin and User roles
            admin.setUserRoles(List.of(adminRole, userRole));
            
            userRepository.save(admin);
            log.warn("Seeded admin account '{}'. Change its password immediately in non-dev environments.", adminUsername);
        }
    }

    private Permission createPermissionIfNotFound(String name, String description) {
        return permissionRepository.findByName(name).orElseGet(() -> {
            Permission permission = new Permission();
            permission.setName(name);
            permission.setDescription(description);
            return permissionRepository.save(permission);
        });
    }

    private Role createRoleIfNotFound(String name, String description, List<Permission> permissions) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setRolePermissions(permissions);
            return roleRepository.save(role);
        });
    }
}

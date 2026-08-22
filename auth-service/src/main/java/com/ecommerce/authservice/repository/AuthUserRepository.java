package com.ecommerce.authservice.repository;

import com.ecommerce.authservice.models.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 8. AuthUserRepository (The Database Access Layer)
 * 
 * INTERN GUIDE: SPRING DATA JPA MAGIC
 * -----------------------------------
 * This is an Interface, yet we never actually write an implementation class for it!
 * 
 * WHY THIS EXISTS:
 * This uses Spring Data JPA. By simply extending `JpaRepository<AuthUser, Long>`, 
 * we instantly get access to standard database operations like:
 * - `save(user)` (Generates an INSERT or UPDATE statement)
 * - `findById(1L)` (Generates a SELECT statement)
 * - `delete(user)` (Generates a DELETE statement, or in our case, our custom Soft Delete UPDATE).
 */
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    
    /**
     * Finds a user by their username.
     * 
     * HOW THE MAGIC WORKS:
     * We don't write any SQL. Spring Boot looks at the method name "findByUsername".
     * It sees "findBy", knows it's a SELECT query. It sees "Username", and knows 
     * to look at the `username` field on the `AuthUser` entity.
     * 
     * It automatically generates and executes this SQL under the hood:
     * `SELECT * FROM auth_users WHERE username = ?`
     * 
     * @return Optional<AuthUser> - We return an Optional instead of a direct AuthUser 
     *         to gracefully handle the scenario where the user isn't found, preventing NullPointerExceptions.
     */
    Optional<AuthUser> findByUsername(String username);
    
    /**
     * Checks if a username is already taken.
     * 
     * HOW THE MAGIC WORKS:
     * "existsBy" tells Spring to do a highly efficient count query.
     * 
     * It automatically generates this SQL:
     * `SELECT count(*) FROM auth_users WHERE username = ?`
     *
     * @return true if the username exists, false otherwise.
     */
    boolean existsByUsername(String username);

    /**
     * Checks if an email address is already registered.
     *
     * WHY WE NEED THIS:
     * Email is now a unique identity field (see AuthUser.email). Before creating a new account
     * we call this to reject duplicate-email sign-ups with a friendly 409 CONFLICT, instead of
     * letting the request hit the database and blow up with an ugly unique-constraint violation.
     *
     * Generated SQL: `SELECT count(*) FROM auth_users WHERE email = ?`
     *
     * @return true if the email is already taken, false otherwise.
     */
    boolean existsByEmail(String email);
}

package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * UserRepository (The HR Database Query Tool)
 * 
 * Data Access Layer for the User Profile.
 * 
 * WHY THIS EXISTS:
 * Spring Data JPA automatically provides implementations for standard database operations 
 * (save, findById, findAll, deleteById) so we don't have to write JDBC/SQL boilerplate code.
 * This interface talks exclusively to the `user_db` PostgreSQL database.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}

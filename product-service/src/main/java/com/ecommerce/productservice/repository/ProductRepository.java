package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ProductRepository (The Warehouse Database Query Tool)
 * 
 * WHY THIS EXISTS:
 * Spring Data JPA automatically writes the complex SQL queries for us. 
 * This interface is how our Java code asks the PostgreSQL database for product information.
 */
/**
 * Data Access Layer for Products.
 * 
 * Communicates strictly with the `product_db`.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * CUSTOM QUERY DEFINITION:
     * Spring Data JPA parses the method name "findByQuantityGreaterThan" and automatically 
     * generates the SQL query: `SELECT * FROM product WHERE quantity > ?`
     * 
     * This allows us to efficiently query only available stock directly on the database side.
     */
    List<Product> findByQuantityGreaterThan(Integer quantity);
}

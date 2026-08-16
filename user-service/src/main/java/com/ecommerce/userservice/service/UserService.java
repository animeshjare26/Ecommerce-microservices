package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserRequestDto;
import com.ecommerce.userservice.dto.UserResponseDto;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService (The HR Manager)
 * 
 * WHY THIS EXISTS:
 * This service contains the actual business logic for managing users. 
 * While the UserController (Receptionist) handles HTTP stuff, this class handles the real work: 
 * looking up users in the database (Filing Cabinet), validating data, and returning the results.
 * 
 * Note: Notice how this service does NOT handle passwords or logins. That is strictly the 
 * domain of the Auth Service.
 * 
 * WHY THIS EXISTS:
 * Controllers should only handle HTTP routing and JSON parsing. Repositories should only 
 * handle database queries. This Service layer bridges the gap. It contains the actual "business rules" 
 * and data transformations (mapping Entities to DTOs).
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;

    /**
     * Maps incoming DTO to Entity and saves it.
     */
    public UserResponseDto registerUser(UserRequestDto requestDto) { 
        User user = new User();
        user.setName(requestDto.getName());
        user.setEmail(requestDto.getEmail());
        user.setAddress(requestDto.getAddress());
        
        // Save to PostgreSQL via Spring Data JPA
        User savedUser = userRepository.save(user);
        
        // Convert back to a DTO so we don't expose database internals to the client
        return mapToResponseDto(savedUser);
    }
    
    /**
     * Retrieves a user by ID.
     * ERROR HANDLING: If the user doesn't exist, we throw a RuntimeException. 
     * In a production app, this should throw a custom exception (e.g., ResourceNotFoundException) 
     * that is caught by a @ControllerAdvice to return a neat 404 JSON response.
     */
    public UserResponseDto getUserProfile(Long id) { 
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToResponseDto(user);
    }

    /**
     * Retrieves all users.
     */
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Replaces the user's data with the incoming payload.
     */
    public UserResponseDto updateProfile(Long id, UserRequestDto requestDto) {
        // 1. Fetch existing user
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        // 2. Modify data
        user.setName(requestDto.getName());
        user.setEmail(requestDto.getEmail());
        user.setAddress(requestDto.getAddress());
        
        // 3. Save updates
        User updatedUser = userRepository.save(user);
        return mapToResponseDto(updatedUser);
    }

    /**
     * E-commerce specific: Just updating address for quick checkout flow.
     * This avoids sending the whole profile payload over the network.
     */
    public UserResponseDto updateAddress(Long id, String newAddress) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        user.setAddress(newAddress);
        User updatedUser = userRepository.save(user);
        return mapToResponseDto(updatedUser);
    }

    /**
     * Deletes the user. 
     * Note: In many enterprise apps, we do "Soft Deletes" (setting a flag like is_deleted = true) 
     * instead of permanently deleting rows, to preserve order history.
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Transforms a database Entity into a clean Data Transfer Object (DTO) to return to the client.
     */
    private UserResponseDto mapToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setAddress(user.getAddress());
        return dto;
    }
}

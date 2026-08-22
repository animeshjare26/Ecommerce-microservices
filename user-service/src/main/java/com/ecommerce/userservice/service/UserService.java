package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.ProvisionProfileRequest;
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
     * Creates or updates the profile that mirrors an auth-service identity ("provisioning").
     *
     * WHY THIS REPLACED THE OLD PUBLIC registerUser():
     * Previously the public could POST /users/register and we'd auto-generate a brand-new id that
     * had NOTHING to do with the person's auth identity. That is exactly the disconnect we are
     * fixing. Now profiles are created by the auth-service (which supplies the id), so this method
     * is INTERNAL and always receives the id to use.
     *
     * WHY IT IS AN IDEMPOTENT UPSERT (create-or-update, safe to repeat):
     * The provisioning call can be retried — on a transient failure during registration, or by the
     * reconciliation job. If we blindly inserted every time, retries would explode with duplicate-
     * key errors. Instead we look up the id first:
     *   - if no profile exists, we CREATE it, and
     *   - if one already exists, we leave it in place (only filling gaps) and return it.
     * Calling this twice with the same id therefore has the same effect as calling it once.
     *
     * @param request the profile to provision, INCLUDING the id to use as the primary key.
     * @return the resulting profile as a response DTO.
     */
    public UserResponseDto provisionProfile(ProvisionProfileRequest request) {
        // Look up any existing profile with this id. `orElseGet` builds a fresh User only if absent.
        User user = userRepository.findById(request.getId())
                .orElseGet(() -> {
                    User fresh = new User();
                    // CRITICAL: set the primary key to the auth user's id (do NOT auto-generate).
                    fresh.setId(request.getId());
                    return fresh;
                });

        // Email is auth-owned; we always keep our replicated copy aligned with what auth sends.
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        // Name/address are profile-owned. On first creation we take whatever the caller sent. On a
        // later reconciliation call these may be null, so we only overwrite when a value is provided
        // — that way reconciliation never wipes profile data the user has since filled in.
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        // save() performs an INSERT if the id is new, or an UPDATE if the row already exists.
        User savedUser = userRepository.save(user);
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
     * Updates the PROFILE-owned fields of a user (name, address).
     *
     * WHY WE DO NOT UPDATE EMAIL HERE (important):
     * Email is an identity/credential field owned by the auth-service. If we let users change their
     * email here, this service's copy would silently disagree with the auth-service's login email —
     * the two would drift out of sync, which is the exact bug this whole design prevents. So we
     * DELIBERATELY IGNORE any email in the incoming payload. Changing an email must be done through
     * the auth-service, which then propagates the new value down to our replicated copy.
     */
    public UserResponseDto updateProfile(Long id, UserRequestDto requestDto) {
        // 1. Fetch existing user
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // 2. Modify ONLY profile-owned fields. Note: requestDto.getEmail() is intentionally ignored.
        user.setName(requestDto.getName());
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

package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserRequestDto;
import com.ecommerce.userservice.dto.UserResponseDto;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserResponseDto registerUser(UserRequestDto requestDto) { 
        User user = new User();
        user.setName(requestDto.getName());
        user.setEmail(requestDto.getEmail());
        user.setAddress(requestDto.getAddress());
        
        User savedUser = userRepository.save(user);
        return mapToResponseDto(savedUser);
    }
    
    public UserResponseDto getUserProfile(Long id) { 
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToResponseDto(user);
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public UserResponseDto updateProfile(Long id, UserRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        user.setName(requestDto.getName());
        user.setEmail(requestDto.getEmail());
        user.setAddress(requestDto.getAddress());
        
        User updatedUser = userRepository.save(user);
        return mapToResponseDto(updatedUser);
    }

    // E-commerce specific: Just updating address for quick checkout flow
    public UserResponseDto updateAddress(Long id, String newAddress) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        user.setAddress(newAddress);
        User updatedUser = userRepository.save(user);
        return mapToResponseDto(updatedUser);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserResponseDto mapToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setAddress(user.getAddress());
        return dto;
    }
}

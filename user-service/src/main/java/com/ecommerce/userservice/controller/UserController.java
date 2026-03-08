package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.UserRequestDto;
import com.ecommerce.userservice.dto.UserResponseDto;
import com.ecommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@RequestBody UserRequestDto requestDto) { 
        return userService.registerUser(requestDto); 
    }
    
    @GetMapping("/{id}/profile")
    public UserResponseDto getProfile(@PathVariable Long id) { 
        return userService.getUserProfile(id); 
    }

    @GetMapping
    public List<UserResponseDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}/profile")
    public UserResponseDto updateProfile(@PathVariable Long id, @RequestBody UserRequestDto requestDto) {
        return userService.updateProfile(id, requestDto);
    }

    // Dynamic e-commerce address update during checkout
    @PatchMapping("/{id}/address")
    public UserResponseDto updateAddress(@PathVariable Long id, @RequestParam String address) {
        return userService.updateAddress(id, address);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}

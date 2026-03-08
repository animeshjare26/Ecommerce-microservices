package com.ecommerce.userservice.dto;

import lombok.Data;

@Data
public class UserResponseDto {
    private Long id;
    private String name;
    private String email;
    private String address; // Added for e-commerce
}

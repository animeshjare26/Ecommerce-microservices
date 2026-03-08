package com.ecommerce.orderservice.feign;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String address;
}

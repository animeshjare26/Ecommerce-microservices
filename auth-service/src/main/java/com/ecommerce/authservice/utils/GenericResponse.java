package com.ecommerce.authservice.utils;

import lombok.Builder;
import lombok.Data;

/**
 * GenericResponse (The Standardized Shipping Box)
 * 
 * WHY THIS EXISTS:
 * When a microservice responds to the frontend, we want the response to always look the same, 
 * whether it's a success or an error. Imagine if every department sent packages in different 
 * sized boxes—it would be a nightmare for the mailroom (the frontend) to process them. 
 * This class forces all responses to have a uniform `success`, `message`, and `data` structure.
 */
@Data
@Builder
public class GenericResponse<T> {
    private Boolean success;
    private String message;
    private T data;

    public static <T> GenericResponse<T> success(T data) {
        return GenericResponse.<T>builder()
                .success(true)
                .message("success")
                .data(data)
                .build();
    }

    public static <T> GenericResponse<T> success(T data, String message) {
        return GenericResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> GenericResponse<T> error(T data) {
        return GenericResponse.<T>builder()
                .success(false)
                .message("error")
                .data(data)
                .build();
    }

    public static <T> GenericResponse<T> error(T data, String message) {
        return GenericResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }

}

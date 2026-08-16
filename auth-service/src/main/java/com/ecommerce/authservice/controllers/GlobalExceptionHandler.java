package com.ecommerce.authservice.controllers;

import com.ecommerce.authservice.utils.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 9. GlobalExceptionHandler (The PR / Damage Control Department)
 * 
 * INTERN GUIDE: EXCEPTION HANDLING
 * --------------------------------
 * This class acts as a safety net that catches any errors or exceptions thrown 
 * anywhere in the Auth Service before they reach the user.
 * 
 * WHY WE ADDED IT:
 * If our Security Guard (AuthServiceImpl) throws a "ResponseStatusException" (e.g., wrong password),
 * we don't want Spring Boot to return a messy, unreadable error page or stack trace. 
 * This "Damage Control" department catches that error and neatly packages it into our 
 * standard `GenericResponse<T>` format, so the frontend always gets a clean, predictable JSON response.
 */
@RestControllerAdvice // Tells Spring: "Listen for exceptions across ALL Controllers in this app."
@Slf4j // Lombok annotation for automatic logging.
public class GlobalExceptionHandler {

    /**
     * Catches specifically ResponseStatusExceptions (e.g., when we throw 401 Unauthorized or 409 Conflict).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<GenericResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        log.error("ResponseStatusException: {}", ex.getReason());
        // We pull the exact HTTP status code and error message from the exception and wrap it.
        return ResponseEntity.status(ex.getStatusCode())
                .body(GenericResponse.error(null, ex.getReason()));
    }

    /**
     * The absolute last line of defense. Catches ANY generic Exception we forgot to handle.
     * Prevents the app from crashing and exposing internal server details to the user.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled Exception: ", ex);
        // We return a generic 500 error instead of revealing the exact code failure.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenericResponse.error(null, "An unexpected error occurred. Please try again later."));
    }
}

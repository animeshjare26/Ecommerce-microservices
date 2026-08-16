package com.ecommerce.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.authservice.utils.GenericResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 3. AuthEntryPointJwt (The Bouncer's Rejection Letter)
 * -----------------------------------------------
 * This class implements AuthenticationEntryPoint. Spring Security uses this class 
 * when an unauthenticated user tries to access a protected resource.
 * 
 * WHY WE OVERRIDE IT:
 * By default, Spring Security might just send back a blank 401 Unauthorized status, 
 * or sometimes even try to redirect the user to a default HTML login page.
 * 
 * Since this is a REST API, we want ALL responses (even errors) to be structured JSON.
 * This class intercepts the rejection and writes a formatted JSON response using 
 * our custom GenericResponse wrapper, explaining exactly why the request was blocked.
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    
    /**
     * This method is triggered whenever an AuthenticationException occurs (e.g., missing or invalid JWT).
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        
        // 1. Tell the client (browser/mobile app) that the response is going to be JSON.
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // 2. Set the HTTP Status code to 401 (Unauthorized).
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // 3. We use Jackson's ObjectMapper to convert Java objects into JSON strings.
        ObjectMapper mapper = new ObjectMapper();
        
        // 4. Create our standard API error response. We grab the default exception message (like "Full authentication is required to access this resource").
        GenericResponse<Object> genericResponse = GenericResponse.error(null, authException.getMessage());
        
        // 5. Write the JSON response directly into the HTTP response body stream.
        mapper.writeValue(response.getOutputStream(), genericResponse);
    }
}

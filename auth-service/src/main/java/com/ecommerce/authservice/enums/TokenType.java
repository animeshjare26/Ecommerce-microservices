package com.ecommerce.authservice.enums;

/**
 * 1. TokenType (The ID Badge Types)
 * 
 * In our system, we hand out two main types of ID badges:
 * 
 * - ACCESS_TOKEN: A temporary visitor pass. It expires very quickly (e.g., 15 minutes) for security reasons.
 * - REFRESH_TOKEN: A master pass that the visitor keeps hidden. When their 15-minute Access Token expires, 
 *   they can use this Refresh Token to get a new Access Token without having to type their username and password again.
 * 
 * WHY WE ADDED IT:
 * We created an enum (a strict list of options) so our code knows exactly which type of token 
 * we are generating or reading, preventing typos and bugs.
 */
public enum TokenType {
    ACCESS_TOKEN,
    REFRESH_TOKEN,
    FORGOT_PASSWORD
}

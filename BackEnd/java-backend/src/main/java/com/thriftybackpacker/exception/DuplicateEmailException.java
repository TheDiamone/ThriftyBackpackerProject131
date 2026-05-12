package com.thriftybackpacker.exception;

/**
 * Thrown by UserService.registerUser when the supplied email is already in use.
 * Mapped to HTTP 409 Conflict by GlobalExceptionHandler.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}

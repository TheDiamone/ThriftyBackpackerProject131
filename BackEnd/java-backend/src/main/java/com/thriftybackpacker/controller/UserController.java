package com.thriftybackpacker.controller;

import com.thriftybackpacker.model.User;
import com.thriftybackpacker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User endpoints — added to satisfy frontend calls not present in the Python backend.
 *
 * Missing in Python backend:
 *   - GET /api/v1/users/login  — called by frontend authService.js
 *   - POST /api/v1/users/register — for creating new users
 *
 * The Python backend's User model has NO password field, so login is email-only lookup.
 * No JWT is issued — the frontend stores userId in localStorage (matching Python behaviour).
 */
@Tag(name = "Users", description = "User lookup and registration (no JWT — matching Python backend behaviour)")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Login — called by frontend authService.js as GET /users/login?email=&password=
     *
     * The Python backend has no password field and no authentication system.
     * This endpoint looks up the user by email only. Password parameter is accepted
     * for frontend compatibility but is not verified (no password column exists).
     *
     * Returns: { userId, message }
     */
    @Operation(
            summary = "User login (email lookup)",
            description = "Looks up a user by email. Password parameter accepted for frontend compatibility " +
                    "but NOT verified — the Python backend has no password storage. Returns userId."
    )
    @GetMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam String email,
            @RequestParam(required = false) String password) {
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId(),
                "User_ID", user.getUserId(),
                "message", "Login successful"
        ));
    }

    /**
     * Register — creates a new user.
     * Returns { userId, message } on success.
     * Returns 400 for missing/invalid email or missing firstName.
     * Returns 409 if the email is already registered.
     * No password is stored or required.
     */
    @Operation(
            summary = "Register a new user",
            description = "Creates a user record with no password. Returns 409 if the email is already registered."
    )
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String firstName = body.getOrDefault("First_Name", body.getOrDefault("firstName", "")).trim();
        String lastName  = body.getOrDefault("Last_Name",  body.getOrDefault("lastName",  "")).trim();
        String email     = body.getOrDefault("Email",       body.getOrDefault("email",     "")).trim().toLowerCase();
        String phone     = body.getOrDefault("Phone_Number", body.getOrDefault("phoneNumber", "")).trim();

        if (firstName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "firstName is required."));
        }
        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "email is required."));
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest().body(Map.of("detail", "email is not a valid address."));
        }

        User user = userService.registerUser(firstName, lastName, email, phone);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "userId", user.getUserId(),
                "User_ID", user.getUserId(),
                "message", "User created successfully"
        ));
    }
}

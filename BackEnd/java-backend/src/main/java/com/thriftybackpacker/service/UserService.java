package com.thriftybackpacker.service;

import com.thriftybackpacker.exception.DuplicateEmailException;
import com.thriftybackpacker.exception.ResourceNotFoundException;
import com.thriftybackpacker.model.User;
import com.thriftybackpacker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management service.
 *
 * The Python backend's User model has no password field — the Python project has
 * no authentication system. This Java backend replicates that behaviour:
 *   - GET /api/v1/users/login looks up by email only
 *   - No password is verified or stored
 *   - No JWT token is issued
 *
 * This is documented as a known gap from the Python implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** Used by GET /api/v1/users/login — lookup by email only (Python has no password field). */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Find-or-create — used only by GET /api/v1/setup-seed-data/ so the seed
     * endpoint stays idempotent (calling it twice doesn't throw).
     */
    @Transactional
    public User createUser(String firstName, String lastName, String email, String phoneNumber, String password) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).orElseThrow();
        }
        return buildAndSave(firstName, lastName, email, phoneNumber, password);
    }

    /**
     * Used by POST /api/v1/users/register.
     * Throws DuplicateEmailException (→ 409) when the email is already in use.
     */
    @Transactional
    public User registerUser(String firstName, String lastName, String email, String phoneNumber, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        return buildAndSave(firstName, lastName, email, phoneNumber, password);
    }

    private User buildAndSave(String firstName, String lastName, String email, String phoneNumber, String password) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(password);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}

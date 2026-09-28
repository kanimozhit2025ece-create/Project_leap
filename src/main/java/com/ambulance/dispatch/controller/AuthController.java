
package com.ambulance.dispatch.controller;

import com.ambulance.dispatch.entity.UserAccount;
import com.ambulance.dispatch.repository.UserAccountRepository;
import com.ambulance.dispatch.service.PasswordService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserAccountRepository userRepository;
    private final PasswordService passwordService;

    public AuthController(
            UserAccountRepository userRepository,
            PasswordService passwordService
    ) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    public record AuthRequest(
            String fullName,
            String email,
            String password
    ) {}

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record ChangePasswordRequest(
            String currentPassword,
            String newPassword
    ) {}

    public record UserResponse(
            Long id,
            String fullName,
            String email,
            String role
    ) {}


    // ================= PUBLIC REGISTRATION =================

    // Anyone can register, but only as PUBLIC.

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UserResponse register(
            @RequestBody AuthRequest request
    ) {
        return createUser(request, "PUBLIC");
    }


    // ================= STAFF REGISTRATION =================

    // Only an existing ADMIN can create STAFF accounts.

    @PostMapping("/register-staff")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UserResponse registerStaff(
            @RequestBody AuthRequest request,
            HttpServletRequest servletRequest
    ) {
        UserAccount admin = requireUser(servletRequest);

        if (!"ADMIN".equals(admin.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only ADMIN can register staff"
            );
        }

        return createUser(request, "STAFF");
    }


    // ================= COMMON USER CREATION =================

    private UserResponse createUser(
            AuthRequest request,
            String role
    ) {
        if (request == null ||
                request.fullName() == null ||
                request.fullName().isBlank() ||
                request.email() == null ||
                request.email().isBlank() ||
                request.password() == null ||
                request.password().length() < 8) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter name, email and password of at least 8 characters"
            );
        }

        String email = request.email()
                .trim()
                .toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        UserAccount user = new UserAccount();

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordService.hashPassword(
                        request.password()
                )
        );

        user.setRole(role);

        return toResponse(userRepository.save(user));
    }


    // ================= LOGIN =================

    @PostMapping("/login")
    public UserResponse login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        if (request == null ||
                request.email() == null ||
                request.password() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email and password required"
            );
        }

        UserAccount user = userRepository
                .findByEmail(
                        request.email().trim().toLowerCase()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password"
                        )
                );

        if (!passwordService.verifyPassword(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        HttpSession oldSession =
                servletRequest.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session =
                servletRequest.getSession(true);

        session.setAttribute(
                "userId",
                user.getId()
        );

        session.setAttribute(
                "role",
                user.getRole()
        );

        session.setMaxInactiveInterval(60 * 60);

        return toResponse(user);
    }


    // ================= CURRENT USER =================

    @GetMapping("/me")
    public UserResponse currentUser(
            HttpServletRequest request
    ) {
        return toResponse(requireUser(request));
    }


    // ================= CHANGE PASSWORD =================

    @PutMapping("/change-password")
    @Transactional
    public Map<String, String> changePassword(
            @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        UserAccount user = requireUser(servletRequest);

        if (request == null ||
                request.currentPassword() == null ||
                request.newPassword() == null ||
                request.newPassword().length() < 8) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password and new password (at least 8 characters) required"
            );
        }

        if (!passwordService.verifyPassword(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect"
            );
        }

        if (request.currentPassword()
                .equals(request.newPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose a different new password"
            );
        }

        user.setPasswordHash(
                passwordService.hashPassword(
                        request.newPassword()
                )
        );

        userRepository.save(user);

        return Map.of(
                "message",
                "Password updated successfully"
        );
    }


    // ================= LOGOUT =================

    @PostMapping("/logout")
    public Map<String, String> logout(
            HttpServletRequest request
    ) {
        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return Map.of(
                "message",
                "Logged out successfully"
        );
    }


    // ================= AUTH HELPER =================

    private UserAccount requireUser(
            HttpServletRequest request
    ) {
        HttpSession session =
                request.getSession(false);

        if (session == null ||
                !(session.getAttribute("userId")
                        instanceof Long userId)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please login"
            );
        }

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "User not found"
                        )
                );
    }


    // ================= RESPONSE HELPER =================

    private UserResponse toResponse(
            UserAccount user
    ) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
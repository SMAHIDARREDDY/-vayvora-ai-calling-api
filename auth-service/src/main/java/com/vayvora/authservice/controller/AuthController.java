package com.vayvora.authservice.controller;

import com.vayvora.shared.entities.User;
import com.vayvora.authservice.dto.*;
import com.vayvora.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {
    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Register new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Registration failed: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/auth/login
     * User login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Login failed: Invalid credentials"));
        }
    }

    /**
     * POST /api/v1/auth/refresh-token
     * Refresh access token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            TokenResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token refresh failed"));
        }
    }

    /**
     * POST /api/v1/auth/verify
     * Verify JWT token
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestHeader("Authorization") String token) {
        try {
            boolean valid = authService.verifyToken(token.replace("Bearer ", ""));
            return ResponseEntity.ok(new VerifyResponse(valid));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token verification failed"));
        }
    }

    /**
     * POST /api/v1/auth/logout
     * User logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            authService.logout(token.replace("Bearer ", ""));
            return ResponseEntity.ok(new SuccessResponse("Logout successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Logout failed"));
        }
    }

    /**
     * GET /api/v1/auth/profile
     * Get user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
        try {
            User user = authService.getUserFromToken(token.replace("Bearer ", ""));
            return ResponseEntity.ok(authService.toUserResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Failed to fetch profile"));
        }
    }

    /**
     * PUT /api/v1/auth/profile
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateProfileRequest request) {
        try {
            User user = authService.getUserFromToken(token.replace("Bearer ", ""));
            User updated = authService.updateProfile(user.getId(), request);
            return ResponseEntity.ok(authService.toUserResponse(updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Profile update failed"));
        }
    }

    /**
     * POST /api/v1/auth/change-password
     * Change user password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody ChangePasswordRequest request) {
        try {
            User user = authService.getUserFromToken(token.replace("Bearer ", ""));
            authService.changePassword(user.getId(), request);
            return ResponseEntity.ok(new SuccessResponse("Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Password change failed: " + e.getMessage()));
        }
    }
}

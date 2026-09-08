package com.vayvora.authservice.controller;

import com.vayvora.authservice.dto.AuthDtos.AuthResponse;
import com.vayvora.authservice.dto.AuthDtos.ChangePasswordRequest;
import com.vayvora.authservice.dto.AuthDtos.LoginRequest;
import com.vayvora.authservice.dto.AuthDtos.MessageResponse;
import com.vayvora.authservice.dto.AuthDtos.RefreshRequest;
import com.vayvora.authservice.dto.AuthDtos.RegisterRequest;
import com.vayvora.authservice.dto.AuthDtos.TokenResponse;
import com.vayvora.authservice.dto.AuthDtos.UpdateProfileRequest;
import com.vayvora.authservice.dto.AuthDtos.UserResponse;
import com.vayvora.authservice.service.AuthService;
import com.vayvora.shared.web.Web;
import com.vayvora.shared.web.Web.UnauthorizedException;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints (spec §30).
 *
 * <p>Paths here are relative to the service's {@code /api/v1} context path, so
 * {@code /auth/login} is served at {@code /api/v1/auth/login}.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh-token")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return new MessageResponse("Signed out");
    }

    /**
     * Token introspection used by the gateway.
     *
     * <p>Returns 200 with {@code valid: true} or 401; both outcomes are useful
     * to a caller deciding whether to refresh.
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verify(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = stripBearer(authorization);
        boolean valid = token != null && authService.verify(token);
        return valid
                ? ResponseEntity.ok(Map.of("valid", true))
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
    }

    /** Current user, identified by the gateway-set header. */
    @GetMapping("/me")
    public UserResponse me(@RequestHeader(Web.Headers.USER_ID) String userId) {
        return authService.currentUser(userId);
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(@RequestHeader(Web.Headers.USER_ID) String userId,
                                      @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(userId, request);
    }

    @PostMapping("/change-password")
    public MessageResponse changePassword(
            @RequestHeader(Web.Headers.USER_ID) String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return new MessageResponse("Password changed. Existing sessions were signed out.");
    }

    private static String stripBearer(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        return authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7).trim()
                : authorization.trim();
    }

    /** Surfaces a missing user header as 401 rather than a 400 from Spring. */
    @org.springframework.web.bind.annotation.ExceptionHandler(
            org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<Web.ApiError> missingHeader(
            org.springframework.web.bind.MissingRequestHeaderException e) {
        throw new UnauthorizedException("Authentication required");
    }
}

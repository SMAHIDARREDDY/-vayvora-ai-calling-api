package com.vayvora.authservice.dto;

import com.vayvora.shared.entity.Identity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request and response payloads for the auth API. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /**
     * Self-service signup.
     *
     * <p>Creates the organization and its first user together: the first person
     * through the door becomes the organization admin (spec §5).
     */
    public record RegisterRequest(
            @NotBlank(message = "Organization name is required")
            String organizationName,

            @NotBlank(message = "First name is required")
            String firstName,

            String lastName,

            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password) {
    }

    public record LoginRequest(
            @NotBlank @Email(message = "Enter a valid email address") String email,
            @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
            String newPassword) {
    }

    public record UpdateProfileRequest(String firstName, String lastName) {
    }

    /** Issued token pair plus the authenticated user. */
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInMillis,
            UserResponse user) {
    }

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresInMillis) {
    }

    public record UserResponse(
            String id,
            String organizationId,
            String email,
            String firstName,
            String lastName,
            String role,
            String status,
            Instant lastActiveAt) {

        public static UserResponse from(Identity.User u) {
            return new UserResponse(
                    u.getId(),
                    u.getOrganizationId(),
                    u.getEmail(),
                    u.getFirstName(),
                    u.getLastName(),
                    u.getRole().name(),
                    u.getStatus().name(),
                    u.getLastActiveAt());
        }
    }

    public record OrganizationResponse(
            String id,
            String name,
            String slug,
            String status,
            String timezone,
            String defaultLanguage) {

        public static OrganizationResponse from(Identity.Organization o) {
            return new OrganizationResponse(
                    o.getId(),
                    o.getName(),
                    o.getSlug(),
                    o.getStatus().name(),
                    o.getTimezone(),
                    o.getDefaultLanguage());
        }
    }

    public record MessageResponse(String message) {
    }
}

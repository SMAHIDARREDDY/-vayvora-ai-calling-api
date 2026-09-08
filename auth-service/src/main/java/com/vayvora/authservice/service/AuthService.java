package com.vayvora.authservice.service;

import com.vayvora.authservice.dto.AuthDtos.AuthResponse;
import com.vayvora.authservice.dto.AuthDtos.ChangePasswordRequest;
import com.vayvora.authservice.dto.AuthDtos.LoginRequest;
import com.vayvora.authservice.dto.AuthDtos.RegisterRequest;
import com.vayvora.authservice.dto.AuthDtos.TokenResponse;
import com.vayvora.authservice.dto.AuthDtos.UpdateProfileRequest;
import com.vayvora.authservice.dto.AuthDtos.UserResponse;
import com.vayvora.shared.entity.Identity;
import com.vayvora.shared.enums.Enums.OrganizationStatus;
import com.vayvora.shared.enums.Enums.UserRole;
import com.vayvora.shared.enums.Enums.UserStatus;
import com.vayvora.shared.repository.Repositories.OrganizationRepository;
import com.vayvora.shared.repository.Repositories.OrganizationUserRepository;
import com.vayvora.shared.repository.Repositories.RefreshTokenRepository;
import com.vayvora.shared.repository.Repositories.UserRepository;
import com.vayvora.shared.security.JwtService;
import com.vayvora.shared.web.Web.ConflictException;
import com.vayvora.shared.web.Web.NotFoundException;
import com.vayvora.shared.web.Web.UnauthorizedException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login, token lifecycle and profile management.
 *
 * <p>Access tokens are short-lived JWTs carrying the organization id; refresh
 * tokens are opaque random strings persisted as SHA-256 digests so they can be
 * revoked on logout or password change (spec §32).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final OrganizationUserRepository memberships;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTtlMillis;

    /**
     * Creates an organization and its first user.
     *
     * <p>The registering user becomes the organization admin, since there is no
     * one else yet to grant them the role.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account already exists for " + email);
        }

        Identity.Organization org = organizations.save(Identity.Organization.builder()
                .name(request.organizationName())
                .slug(uniqueSlug(request.organizationName()))
                .primaryContactEmail(email)
                .status(OrganizationStatus.TRIAL)
                .build());

        Identity.User user = users.save(Identity.User.builder()
                .organizationId(org.getId())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName() == null ? "" : request.lastName())
                .role(UserRole.ORG_ADMIN)
                .status(UserStatus.ACTIVE)
                .lastActiveAt(Instant.now())
                .build());

        memberships.save(Identity.OrganizationUser.builder()
                .organizationId(org.getId())
                .userId(user.getId())
                .role(UserRole.ORG_ADMIN)
                .joinedAt(Instant.now())
                .build());

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Identity.User user = users.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException(
                    "This account is " + user.getStatus().name().toLowerCase(Locale.ROOT));
        }

        user.setLastActiveAt(Instant.now());
        users.save(user);
        return issueTokens(user);
    }

    /**
     * Exchanges a refresh token for a new access token.
     *
     * <p>The refresh token itself is left in place; rotating it here would
     * break clients that retry a failed request with the same token.
     */
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        Identity.RefreshToken stored = refreshTokens
                .findByTokenHash(JwtService.sha256(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isActive()) {
            throw new UnauthorizedException("Refresh token has expired or been revoked");
        }

        Identity.User user = users.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));

        String access = jwtService.issueAccessToken(
                user.getId(), user.getOrganizationId(), user.getEmail(), user.getRole().name());
        return new TokenResponse(access, "Bearer", jwtService.accessTtlMillis());
    }

    /** Revokes the presented refresh token. Idempotent. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(JwtService.sha256(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokens.save(token);
                });
    }

    public UserResponse currentUser(String userId) {
        return UserResponse.from(users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found")));
    }

    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        Identity.User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        return UserResponse.from(users.save(user));
    }

    /**
     * Changes the password and revokes every outstanding refresh token.
     *
     * <p>Revoking sessions is the point of the operation: a password change
     * that leaves old sessions alive does not lock out whoever prompted it.
     */
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        Identity.User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        users.save(user);

        Instant now = Instant.now();
        refreshTokens.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> {
            token.setRevokedAt(now);
            refreshTokens.save(token);
        });
    }

    public boolean verify(String accessToken) {
        return jwtService.isValid(accessToken);
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private AuthResponse issueTokens(Identity.User user) {
        String access = jwtService.issueAccessToken(
                user.getId(), user.getOrganizationId(), user.getEmail(), user.getRole().name());

        String rawRefresh = randomToken();
        refreshTokens.save(Identity.RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(JwtService.sha256(rawRefresh))
                .expiresAt(Instant.now().plusMillis(refreshTtlMillis))
                .build());

        return new AuthResponse(access, rawRefresh, "Bearer",
                jwtService.accessTtlMillis(), UserResponse.from(user));
    }

    private static String randomToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Derives a URL-safe slug, appending a counter if the base is taken.
     *
     * <p>Bounded so a pathological run of collisions cannot loop forever.
     */
    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "org";
        }
        if (!organizations.existsBySlug(base)) {
            return base;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = base + "-" + i;
            if (!organizations.existsBySlug(candidate)) {
                return candidate;
            }
        }
        return base + "-" + Instant.now().truncatedTo(ChronoUnit.SECONDS).toEpochMilli();
    }
}

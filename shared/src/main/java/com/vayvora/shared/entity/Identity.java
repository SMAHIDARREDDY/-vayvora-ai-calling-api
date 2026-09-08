package com.vayvora.shared.entity;

import com.vayvora.shared.enums.Enums.OrganizationStatus;
import com.vayvora.shared.enums.Enums.UserRole;
import com.vayvora.shared.enums.Enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Identity and organization tables (spec §23).
 *
 * <p>The organization is the root entity: every other table in the platform
 * carries an {@code organization_id} and is filtered by it, which is what makes
 * the tenant isolation requirement in §33 enforceable at the query layer.
 */
public final class Identity {

    private Identity() {
    }

    /** A business customer (tenant) of the platform. */
    @Entity
    @Table(name = "organizations", indexes = {
            @Index(name = "idx_org_slug", columnList = "slug", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Organization extends BaseEntity {

        @Column(nullable = false)
        private String name;

        @Column(nullable = false, unique = true)
        private String slug;

        @Column(name = "primary_contact_email")
        private String primaryContactEmail;

        @Column(name = "timezone", nullable = false)
        @Builder.Default
        private String timezone = "Asia/Kolkata";

        @Column(name = "default_language", nullable = false)
        @Builder.Default
        private String defaultLanguage = "en-IN";

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private OrganizationStatus status = OrganizationStatus.TRIAL;

        /** Announce to the caller that they are speaking to an AI (spec §34). */
        @Column(name = "ai_disclosure_enabled", nullable = false)
        @Builder.Default
        private boolean aiDisclosureEnabled = true;

        @Column(name = "record_calls", nullable = false)
        @Builder.Default
        private boolean recordCalls = true;

        @Column(name = "redact_pii", nullable = false)
        @Builder.Default
        private boolean redactPii = true;

        /** Recording retention in days; null means retain indefinitely. */
        @Column(name = "recording_retention_days")
        @Builder.Default
        private Integer recordingRetentionDays = 90;
    }

    @Entity
    @Table(name = "users", indexes = {
            @Index(name = "idx_users_email", columnList = "email", unique = true),
            @Index(name = "idx_users_org", columnList = "organization_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class User extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(nullable = false, unique = true)
        private String email;

        /** BCrypt hash. Never the plaintext password. */
        @Column(name = "password_hash", nullable = false)
        private String passwordHash;

        @Column(name = "first_name", nullable = false)
        private String firstName;

        @Column(name = "last_name", nullable = false)
        private String lastName;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private UserRole role = UserRole.AGENT;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private UserStatus status = UserStatus.ACTIVE;

        @Column(name = "last_active_at")
        private Instant lastActiveAt;

        public String fullName() {
            return firstName + " " + lastName;
        }
    }

    /** Named role definition, for organizations that customize beyond the defaults. */
    @Entity
    @Table(name = "roles", uniqueConstraints = {
            @UniqueConstraint(name = "uk_role_org_name", columnNames = {"organization_id", "name"})
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Role extends BaseEntity {

        @Column(name = "organization_id")
        private String organizationId;

        @Column(nullable = false, length = 64)
        private String name;

        @Column(length = 255)
        private String description;

        /** True for the five built-in roles, which cannot be edited. */
        @Column(name = "is_system", nullable = false)
        @Builder.Default
        private boolean system = false;
    }

    /** A single capability that can be granted to a role. */
    @Entity
    @Table(name = "permissions", indexes = {
            @Index(name = "idx_perm_code", columnList = "code", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Permission extends BaseEntity {

        /** Stable machine code, e.g. {@code agents.create}. */
        @Column(nullable = false, unique = true, length = 96)
        private String code;

        @Column(length = 255)
        private String description;
    }

    /** Grants a permission to a role. */
    @Entity
    @Table(name = "role_permissions", uniqueConstraints = {
            @UniqueConstraint(name = "uk_role_perm", columnNames = {"role_id", "permission_id"})
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RolePermission extends BaseEntity {

        @Column(name = "role_id", nullable = false)
        private String roleId;

        @Column(name = "permission_id", nullable = false)
        private String permissionId;
    }

    /**
     * Membership of a user in an organization.
     *
     * <p>Kept as its own table so a user can later belong to more than one
     * organization without reshaping the users table.
     */
    @Entity
    @Table(name = "organization_users", uniqueConstraints = {
            @UniqueConstraint(name = "uk_org_user", columnNames = {"organization_id", "user_id"})
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizationUser extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "user_id", nullable = false)
        private String userId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        private UserRole role;

        @Column(name = "invited_at")
        private Instant invitedAt;

        @Column(name = "joined_at")
        private Instant joinedAt;
    }

    /**
     * Issued refresh token.
     *
     * <p>Stored so a token can be revoked before its natural expiry — on
     * logout, password change, or when a session is terminated by an admin.
     */
    @Entity
    @Table(name = "refresh_tokens", indexes = {
            @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
            @Index(name = "idx_refresh_user", columnList = "user_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefreshToken extends BaseEntity {

        @Column(name = "user_id", nullable = false)
        private String userId;

        /** SHA-256 of the token; the raw value is never persisted. */
        @Column(name = "token_hash", nullable = false, unique = true, length = 128)
        private String tokenHash;

        @Column(name = "expires_at", nullable = false)
        private Instant expiresAt;

        @Column(name = "revoked_at")
        private Instant revokedAt;

        public boolean isActive() {
            return revokedAt == null && expiresAt.isAfter(Instant.now());
        }
    }
}

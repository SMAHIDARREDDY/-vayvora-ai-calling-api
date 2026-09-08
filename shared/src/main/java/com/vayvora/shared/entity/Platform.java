package com.vayvora.shared.entity;

import com.vayvora.shared.enums.Enums.IntegrationType;
import com.vayvora.shared.enums.Enums.InvoiceStatus;
import com.vayvora.shared.enums.Enums.NotificationChannel;
import com.vayvora.shared.enums.Enums.NotificationSeverity;
import com.vayvora.shared.enums.Enums.PaymentStatus;
import com.vayvora.shared.enums.Enums.SubscriptionStatus;
import com.vayvora.shared.enums.Enums.UsageMetric;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Billing, integration, notification and audit tables (spec §21, §22, §23,
 * §40, §41).
 *
 * <p>Money is stored in minor units (paise) as {@code long}. Floating point is
 * never used for currency.
 */
public final class Platform {

    private Platform() {
    }

    // -----------------------------------------------------------------------
    // Billing
    // -----------------------------------------------------------------------

    @Entity
    @Table(name = "plans", indexes = {
            @Index(name = "idx_plan_code", columnList = "code", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Plan extends BaseEntity {

        @Column(nullable = false, unique = true, length = 48)
        private String code;

        @Column(nullable = false, length = 96)
        private String name;

        /** Monthly price in paise. Null for custom-priced Enterprise. */
        @Column(name = "monthly_price_minor")
        private Long monthlyPriceMinor;

        @Column(length = 8)
        @Builder.Default
        private String currency = "INR";

        @Column(name = "included_minutes")
        private Integer includedMinutes;

        @Column(name = "max_agents")
        private Integer maxAgents;

        @Column(name = "max_phone_numbers")
        private Integer maxPhoneNumbers;

        @Column(name = "included_storage_gb")
        private Integer includedStorageGb;

        /** Per-minute charge beyond the included allowance, in paise. */
        @Column(name = "overage_per_minute_minor")
        private Long overagePerMinuteMinor;

        @Column(nullable = false)
        @Builder.Default
        private boolean active = true;
    }

    @Entity
    @Table(name = "subscriptions", indexes = {
            @Index(name = "idx_sub_org", columnList = "organization_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Subscription extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "plan_id", nullable = false)
        private String planId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private SubscriptionStatus status = SubscriptionStatus.TRIALING;

        @Column(name = "current_period_start", nullable = false)
        private LocalDate currentPeriodStart;

        @Column(name = "current_period_end", nullable = false)
        private LocalDate currentPeriodEnd;

        @Column(name = "cancel_at_period_end", nullable = false)
        @Builder.Default
        private boolean cancelAtPeriodEnd = false;

        @Column(name = "external_subscription_id", length = 128)
        private String externalSubscriptionId;
    }

    /**
     * A metered usage event.
     *
     * <p>Append-only: invoices are computed by summing records in a period, so
     * a usage row is never updated after it is written.
     */
    @Entity
    @Table(name = "usage_records", indexes = {
            @Index(name = "idx_usage_org_period", columnList = "organization_id,recorded_at"),
            @Index(name = "idx_usage_metric", columnList = "organization_id,metric")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageRecord extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        private UsageMetric metric;

        /** Minutes, tokens, gigabytes — unit depends on {@code metric}. */
        @Column(nullable = false)
        private Double quantity;

        /** Call, phone number or document that generated the usage. */
        @Column(name = "source_ref")
        private String sourceRef;

        @Column(name = "recorded_at", nullable = false)
        private Instant recordedAt;

        @Column(name = "cost_minor")
        private Long costMinor;
    }

    @Entity
    @Table(name = "invoices", indexes = {
            @Index(name = "idx_invoice_org", columnList = "organization_id"),
            @Index(name = "idx_invoice_number", columnList = "invoice_number", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Invoice extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "invoice_number", nullable = false, unique = true, length = 64)
        private String invoiceNumber;

        @Column(name = "period_start", nullable = false)
        private LocalDate periodStart;

        @Column(name = "period_end", nullable = false)
        private LocalDate periodEnd;

        @Column(name = "subtotal_minor", nullable = false)
        @Builder.Default
        private Long subtotalMinor = 0L;

        @Column(name = "tax_minor", nullable = false)
        @Builder.Default
        private Long taxMinor = 0L;

        @Column(name = "total_minor", nullable = false)
        @Builder.Default
        private Long totalMinor = 0L;

        @Column(length = 8)
        @Builder.Default
        private String currency = "INR";

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private InvoiceStatus status = InvoiceStatus.DRAFT;

        @Column(name = "issued_at")
        private Instant issuedAt;

        @Column(name = "due_at")
        private Instant dueAt;

        @Column(name = "paid_at")
        private Instant paidAt;
    }

    @Entity
    @Table(name = "payments", indexes = {
            @Index(name = "idx_payment_invoice", columnList = "invoice_id"),
            @Index(name = "idx_payment_org", columnList = "organization_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Payment extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "invoice_id", nullable = false)
        private String invoiceId;

        @Column(name = "amount_minor", nullable = false)
        private Long amountMinor;

        @Column(length = 8)
        @Builder.Default
        private String currency = "INR";

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private PaymentStatus status = PaymentStatus.PENDING;

        @Column(name = "provider", length = 64)
        private String provider;

        @Column(name = "provider_payment_id", length = 128)
        private String providerPaymentId;

        @Column(name = "failure_reason", length = 512)
        private String failureReason;
    }

    // -----------------------------------------------------------------------
    // Integrations
    // -----------------------------------------------------------------------

    @Entity
    @Table(name = "integrations", indexes = {
            @Index(name = "idx_integration_org", columnList = "organization_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Integration extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        private IntegrationType type;

        @Column(nullable = false, length = 64)
        private String provider;

        @Column(name = "display_name", length = 128)
        private String displayName;

        /**
         * Encrypted credential blob. Never store provider secrets in plaintext;
         * this column holds ciphertext produced by the platform's KMS.
         */
        @Column(name = "credentials_encrypted", columnDefinition = "TEXT")
        private String credentialsEncrypted;

        @Column(name = "config", columnDefinition = "TEXT")
        private String config;

        @Column(nullable = false)
        @Builder.Default
        private boolean active = true;

        @Column(name = "last_sync_at")
        private Instant lastSyncAt;
    }

    @Entity
    @Table(name = "api_keys", indexes = {
            @Index(name = "idx_apikey_org", columnList = "organization_id"),
            @Index(name = "idx_apikey_prefix", columnList = "key_prefix")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiKey extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(nullable = false, length = 128)
        private String label;

        /** Leading characters shown in the UI, e.g. {@code vay_live_}. */
        @Column(name = "key_prefix", nullable = false, length = 32)
        private String keyPrefix;

        /** SHA-256 of the full key. The raw key is shown once, then discarded. */
        @Column(name = "key_hash", nullable = false, unique = true, length = 128)
        private String keyHash;

        /** Comma-separated scopes, e.g. {@code calls:read,contacts:rw}. */
        @Column(length = 512)
        private String scopes;

        @Column(name = "created_by")
        private String createdBy;

        @Column(name = "last_used_at")
        private Instant lastUsedAt;

        @Column(name = "revoked_at")
        private Instant revokedAt;

        @Column(name = "expires_at")
        private Instant expiresAt;

        public boolean isActive() {
            return revokedAt == null
                    && (expiresAt == null || expiresAt.isAfter(Instant.now()));
        }
    }

    @Entity
    @Table(name = "webhooks", indexes = {
            @Index(name = "idx_webhook_org", columnList = "organization_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Webhook extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(nullable = false, length = 1024)
        private String url;

        /** Comma-separated event names, e.g. {@code call.completed}. */
        @Column(nullable = false, length = 1024)
        private String events;

        /** Shared secret used to sign delivery payloads. */
        @Column(name = "signing_secret", length = 128)
        private String signingSecret;

        @Column(nullable = false)
        @Builder.Default
        private boolean active = true;

        @Column(name = "delivery_success_count", nullable = false)
        @Builder.Default
        private Long deliverySuccessCount = 0L;

        @Column(name = "delivery_failure_count", nullable = false)
        @Builder.Default
        private Long deliveryFailureCount = 0L;

        @Column(name = "last_delivery_at")
        private Instant lastDeliveryAt;

        /** Rolling delivery success rate, 0..1. */
        public double successRate() {
            long total = deliverySuccessCount + deliveryFailureCount;
            return total == 0 ? 1.0 : (double) deliverySuccessCount / total;
        }
    }

    // -----------------------------------------------------------------------
    // Notifications and auditing
    // -----------------------------------------------------------------------

    @Entity
    @Table(name = "notifications", indexes = {
            @Index(name = "idx_notif_org", columnList = "organization_id"),
            @Index(name = "idx_notif_user", columnList = "user_id,read_at")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Notification extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        /** Null for organization-wide notifications. */
        @Column(name = "user_id")
        private String userId;

        @Column(nullable = false, length = 255)
        private String title;

        @Column(columnDefinition = "TEXT")
        private String body;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 24)
        @Builder.Default
        private NotificationChannel channel = NotificationChannel.IN_APP;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 16)
        @Builder.Default
        private NotificationSeverity severity = NotificationSeverity.INFO;

        /** Entity this notification points at, e.g. a call or campaign id. */
        @Column(name = "target_ref")
        private String targetRef;

        @Column(name = "read_at")
        private Instant readAt;

        @Column(name = "sent_at")
        private Instant sentAt;
    }

    /**
     * Append-only record of access and configuration changes.
     *
     * <p>Never updated or deleted through the application; retention is handled
     * by an out-of-band policy job.
     */
    @Entity
    @Table(name = "audit_logs", indexes = {
            @Index(name = "idx_audit_org", columnList = "organization_id,created_at"),
            @Index(name = "idx_audit_actor", columnList = "actor_user_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditLog extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "actor_user_id")
        private String actorUserId;

        @Column(name = "actor_label", length = 255)
        private String actorLabel;

        /** Dotted action code, e.g. {@code agent.published}. */
        @Column(nullable = false, length = 96)
        private String action;

        @Column(name = "target_type", length = 64)
        private String targetType;

        @Column(name = "target_ref", length = 255)
        private String targetRef;

        @Column(name = "ip_address", length = 64)
        private String ipAddress;

        @Column(name = "user_agent", length = 512)
        private String userAgent;

        /** JSON detail of what changed. */
        @Column(columnDefinition = "TEXT")
        private String metadata;
    }
}

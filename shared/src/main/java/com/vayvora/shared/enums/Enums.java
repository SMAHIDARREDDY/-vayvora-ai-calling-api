package com.vayvora.shared.enums;

/**
 * Platform enumerations.
 *
 * <p>These mirror the state machines and classifications defined in the product
 * documentation: the call lifecycle (§10), campaign lifecycle (§9), agent
 * version lifecycle (§6/§36), lead score bands (§16), knowledge ingestion
 * pipeline (§7) and user roles (§5). Keeping them in one file makes the state
 * space of the platform reviewable in a single place.
 */
public final class Enums {

    private Enums() {
    }

    /** Platform and organization roles (spec §5). */
    public enum UserRole {
        SUPER_ADMIN,
        ORG_ADMIN,
        MANAGER,
        AGENT,
        DEVELOPER
    }

    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        INVITED
    }

    public enum OrganizationStatus {
        ACTIVE,
        TRIAL,
        SUSPENDED,
        OVER_LIMIT,
        CLOSED
    }

    /** Primary purpose an agent is configured for (spec §6). */
    public enum AgentType {
        LEAD_QUALIFICATION,
        CUSTOMER_SUPPORT,
        APPOINTMENT_SCHEDULING,
        RETENTION,
        RECEPTIONIST
    }

    /** Agent and prompt version lifecycle (spec §6, §36). */
    public enum AgentStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }

    public enum PromptVersionState {
        DRAFT,
        IN_REVIEW,
        PUBLISHED,
        ARCHIVED
    }

    /**
     * Call lifecycle (spec §10).
     *
     * <p>The LISTENING -> PROCESSING -> RESPONDING cycle repeats once per
     * conversational turn until the call reaches a terminal state. States are
     * recorded rather than inferred so billing, transfer logic and analytics
     * all read the same source of truth.
     */
    public enum CallState {
        INITIATED,
        RINGING,
        CONNECTED,
        GREETING,
        LISTENING,
        PROCESSING,
        RESPONDING,
        TRANSFER_REQUESTED,
        HUMAN_AGENT,
        CALLBACK_REQUIRED,
        SCHEDULED,
        COMPLETED,
        FAILED,
        NO_ANSWER,
        BUSY;

        /** True while the call is still being handled in real time. */
        public boolean isLive() {
            return this == CONNECTED
                    || this == GREETING
                    || this == LISTENING
                    || this == PROCESSING
                    || this == RESPONDING
                    || this == TRANSFER_REQUESTED
                    || this == HUMAN_AGENT;
        }

        /** True once the call can no longer change state. */
        public boolean isTerminal() {
            return this == COMPLETED
                    || this == FAILED
                    || this == NO_ANSWER
                    || this == BUSY;
        }
    }

    public enum CallDirection {
        INBOUND,
        OUTBOUND
    }

    /** Per-contact result of a campaign attempt (spec §9). */
    public enum CallOutcome {
        INTERESTED,
        NOT_INTERESTED,
        FOLLOW_UP,
        CONVERTED,
        NO_ANSWER,
        BUSY,
        FAILED,
        RESOLVED,
        ESCALATED
    }

    public enum Sentiment {
        POSITIVE,
        NEUTRAL,
        NEGATIVE
    }

    /** Campaign lifecycle (spec §9). */
    public enum CampaignState {
        DRAFT,
        SCHEDULED,
        RUNNING,
        PAUSED,
        COMPLETED
    }

    public enum CampaignContactState {
        PENDING,
        IN_PROGRESS,
        ATTEMPTED,
        COMPLETED,
        EXHAUSTED,
        SKIPPED
    }

    /**
     * Lead score bands (spec §16).
     *
     * <p>Thresholds: 80-100 HOT, 60-79 WARM, 40-59 COLD, 0-39 LOW.
     */
    public enum LeadBand {
        HOT,
        WARM,
        COLD,
        LOW;

        public static LeadBand fromScore(int score) {
            if (score >= 80) {
                return HOT;
            }
            if (score >= 60) {
                return WARM;
            }
            if (score >= 40) {
                return COLD;
            }
            return LOW;
        }
    }

    public enum LeadStatus {
        NEW,
        CONTACTED,
        QUALIFIED,
        PROPOSAL,
        WON,
        LOST,
        NURTURE
    }

    /** Supported knowledge sources (spec §7). */
    public enum KnowledgeSourceType {
        PDF,
        DOCX,
        TXT,
        CSV,
        URL,
        FAQ
    }

    /** Knowledge ingestion pipeline stages (spec §7). */
    public enum IngestStatus {
        QUEUED,
        PARSING,
        CHUNKING,
        EMBEDDING,
        INDEXED,
        FAILED
    }

    public enum PhoneNumberDirection {
        INBOUND,
        OUTBOUND,
        BOTH
    }

    public enum AppointmentStatus {
        CONFIRMED,
        RESCHEDULED,
        CANCELLED,
        COMPLETED,
        NO_SHOW
    }

    public enum TaskStatus {
        OPEN,
        IN_PROGRESS,
        DONE,
        CANCELLED
    }

    public enum SubscriptionStatus {
        ACTIVE,
        PAST_DUE,
        CANCELLED,
        TRIALING
    }

    public enum InvoiceStatus {
        DRAFT,
        ISSUED,
        PAID,
        OVERDUE,
        VOID
    }

    public enum PaymentStatus {
        PENDING,
        SUCCEEDED,
        FAILED,
        REFUNDED
    }

    /** Metered usage categories for billing (spec §22). */
    public enum UsageMetric {
        VOICE_MINUTES,
        AI_TOKENS,
        PHONE_NUMBER,
        STORAGE_GB,
        TRANSCRIPTION_MINUTES
    }

    public enum NotificationChannel {
        IN_APP,
        EMAIL,
        SMS,
        WHATSAPP
    }

    public enum NotificationSeverity {
        INFO,
        WARNING,
        ERROR
    }

    public enum IntegrationType {
        CRM,
        CALENDAR,
        TICKETING,
        MESSAGING,
        TELEPHONY,
        CUSTOM
    }

    public enum ParticipantRole {
        AI_AGENT,
        CUSTOMER,
        HUMAN_AGENT,
        SUPERVISOR
    }

    /** Speaker on a transcript turn. */
    public enum SpeakerType {
        AI,
        CUSTOMER,
        HUMAN_AGENT,
        SYSTEM
    }
}

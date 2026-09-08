package com.vayvora.shared.entity;

import com.vayvora.shared.enums.Enums.CallDirection;
import com.vayvora.shared.enums.Enums.CallOutcome;
import com.vayvora.shared.enums.Enums.CallState;
import com.vayvora.shared.enums.Enums.CampaignContactState;
import com.vayvora.shared.enums.Enums.CampaignState;
import com.vayvora.shared.enums.Enums.ParticipantRole;
import com.vayvora.shared.enums.Enums.Sentiment;
import com.vayvora.shared.enums.Enums.SpeakerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Campaign and call tables (spec §9, §10, §14, §15, §16, §23).
 *
 * <p>The call is the entity with the richest relationships in the model: it
 * ties together the agent, campaign, contact and phone number, and owns the
 * recording, transcript, summary, sentiment and intent artifacts it produces.
 * Those artifacts are separate tables because they are written at different
 * points in the pipeline and read independently.
 */
public final class Calls {

    private Calls() {
    }

    // -----------------------------------------------------------------------
    // Campaigns
    // -----------------------------------------------------------------------

    @Entity
    @Table(name = "campaigns", indexes = {
            @Index(name = "idx_campaign_org", columnList = "organization_id"),
            @Index(name = "idx_campaign_state", columnList = "organization_id,state")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Campaign extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(nullable = false)
        private String name;

        @Column(name = "agent_id", nullable = false)
        private String agentId;

        @Column(name = "phone_number_id")
        private String phoneNumberId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private CampaignState state = CampaignState.DRAFT;

        @Column(name = "scheduled_start_at")
        private Instant scheduledStartAt;

        @Column(name = "started_at")
        private Instant startedAt;

        @Column(name = "completed_at")
        private Instant completedAt;

        @Column(name = "call_window_start", length = 8)
        @Builder.Default
        private String callWindowStart = "10:00";

        @Column(name = "call_window_end", length = 8)
        @Builder.Default
        private String callWindowEnd = "19:00";

        /** Attempts allowed per contact before it is marked exhausted. */
        @Column(name = "max_attempts", nullable = false)
        @Builder.Default
        private Integer maxAttempts = 3;

        /** Calls placed concurrently; caps load on the telephony provider. */
        @Column(name = "concurrency", nullable = false)
        @Builder.Default
        private Integer concurrency = 5;

        @Column(name = "created_by")
        private String createdBy;
    }

    /**
     * Membership of a contact in a campaign, with its per-contact progress.
     *
     * <p>Aggregate campaign metrics (spec §9) are derived from these rows rather
     * than kept as counters on the campaign, so a retried or corrected attempt
     * cannot leave the totals inconsistent.
     */
    @Entity
    @Table(name = "campaign_contacts", uniqueConstraints = {
            @UniqueConstraint(name = "uk_campaign_contact",
                    columnNames = {"campaign_id", "contact_id"})
    }, indexes = {
            @Index(name = "idx_camp_contact_state", columnList = "campaign_id,state")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CampaignContact extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "campaign_id", nullable = false)
        private String campaignId;

        @Column(name = "contact_id", nullable = false)
        private String contactId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private CampaignContactState state = CampaignContactState.PENDING;

        @Column(nullable = false)
        @Builder.Default
        private Integer attempts = 0;

        @Column(name = "last_attempt_at")
        private Instant lastAttemptAt;

        @Column(name = "next_attempt_at")
        private Instant nextAttemptAt;

        @Enumerated(EnumType.STRING)
        @Column(length = 32)
        private CallOutcome outcome;

        @Column(name = "last_call_id")
        private String lastCallId;
    }

    // -----------------------------------------------------------------------
    // Calls
    // -----------------------------------------------------------------------

    @Entity
    @Table(name = "calls", indexes = {
            @Index(name = "idx_call_org", columnList = "organization_id"),
            @Index(name = "idx_call_state", columnList = "organization_id,state"),
            @Index(name = "idx_call_started", columnList = "organization_id,started_at"),
            @Index(name = "idx_call_agent", columnList = "organization_id,agent_id"),
            @Index(name = "idx_call_campaign", columnList = "campaign_id"),
            @Index(name = "idx_call_contact", columnList = "contact_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Call extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "agent_id")
        private String agentId;

        /** Version of the agent that handled the call, for reproducibility. */
        @Column(name = "agent_version")
        private Integer agentVersion;

        @Column(name = "campaign_id")
        private String campaignId;

        @Column(name = "contact_id")
        private String contactId;

        @Column(name = "phone_number_id")
        private String phoneNumberId;

        /** The customer's number in E.164. */
        @Column(name = "customer_number", length = 24)
        private String customerNumber;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 16)
        private CallDirection direction;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private CallState state = CallState.INITIATED;

        @Enumerated(EnumType.STRING)
        @Column(length = 32)
        private CallOutcome outcome;

        @Column(name = "started_at")
        private Instant startedAt;

        /** Set when the call reaches CONNECTED; billing meters from here. */
        @Column(name = "connected_at")
        private Instant connectedAt;

        @Column(name = "ended_at")
        private Instant endedAt;

        @Column(name = "duration_seconds", nullable = false)
        @Builder.Default
        private Integer durationSeconds = 0;

        @Column(length = 16)
        private String language;

        /** Provider's own call identifier, for reconciliation. */
        @Column(name = "provider_call_id", length = 128)
        private String providerCallId;

        @Column(name = "transferred_to_user_id")
        private String transferredToUserId;

        @Column(name = "transfer_reason", length = 255)
        private String transferReason;

        @Column(name = "recording_enabled", nullable = false)
        @Builder.Default
        private boolean recordingEnabled = true;

        /** Share of speaking time taken by the AI, 0..1. */
        @Column(name = "talk_ratio")
        private Double talkRatio;

        @Column(name = "lead_score")
        private Integer leadScore;
    }

    /** Who was on the call, and for how long. */
    @Entity
    @Table(name = "call_participants", indexes = {
            @Index(name = "idx_participant_call", columnList = "call_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallParticipant extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "call_id", nullable = false)
        private String callId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        private ParticipantRole role;

        /** User id for a human agent, agent id for the AI, else null. */
        @Column(name = "participant_ref")
        private String participantRef;

        @Column(name = "display_name", length = 255)
        private String displayName;

        @Column(name = "joined_at")
        private Instant joinedAt;

        @Column(name = "left_at")
        private Instant leftAt;
    }

    @Entity
    @Table(name = "call_recordings", indexes = {
            @Index(name = "idx_recording_call", columnList = "call_id", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallRecording extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "call_id", nullable = false, unique = true)
        private String callId;

        /** Object-storage key. Audio never lives in the database. */
        @Column(name = "storage_key", nullable = false, length = 1024)
        private String storageKey;

        @Column(name = "format", length = 16)
        @Builder.Default
        private String format = "mp3";

        @Column(name = "size_bytes")
        private Long sizeBytes;

        @Column(name = "duration_seconds")
        private Integer durationSeconds;

        /** When retention policy deletes this recording (spec §34). */
        @Column(name = "expires_at")
        private Instant expiresAt;
    }

    @Entity
    @Table(name = "call_transcripts", indexes = {
            @Index(name = "idx_transcript_call", columnList = "call_id", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallTranscript extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "call_id", nullable = false, unique = true)
        private String callId;

        @Column(length = 16)
        private String language;

        /** Whole transcript as plain text, for search and export. */
        @Lob
        @Column(name = "full_text", columnDefinition = "TEXT")
        private String fullText;

        @Column(name = "engine", length = 96)
        private String engine;

        @Column(name = "word_count")
        private Integer wordCount;
    }

    /** One conversational turn. */
    @Entity
    @Table(name = "call_messages", indexes = {
            @Index(name = "idx_message_call", columnList = "call_id,sequence")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallMessage extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "call_id", nullable = false)
        private String callId;

        /** Monotonic ordering within the call. */
        @Column(nullable = false)
        private Integer sequence;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 24)
        private SpeakerType speaker;

        @Lob
        @Column(nullable = false, columnDefinition = "TEXT")
        private String content;

        /** Milliseconds from call start. */
        @Column(name = "offset_ms")
        private Integer offsetMs;

        @Enumerated(EnumType.STRING)
        @Column(length = 16)
        private Sentiment sentiment;

        /** Tool invoked on this turn, when the agent called one. */
        @Column(name = "tool_code", length = 96)
        private String toolCode;

        @Column(name = "tool_payload", columnDefinition = "TEXT")
        private String toolPayload;
    }

    /** AI-generated call summary (spec §15). */
    @Entity
    @Table(name = "call_summaries", indexes = {
            @Index(name = "idx_summary_call", columnList = "call_id", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallSummary extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "call_id", nullable = false, unique = true)
        private String callId;

        @Lob
        @Column(columnDefinition = "TEXT")
        private String summary;

        /** Newline-separated action items. */
        @Column(name = "action_items", columnDefinition = "TEXT")
        private String actionItems;

        /** Newline-separated objections raised by the customer. */
        @Column(columnDefinition = "TEXT")
        private String objections;

        @Column(name = "next_follow_up_at")
        private Instant nextFollowUpAt;

        @Column(name = "model", length = 96)
        private String model;
    }

    /** Sentiment reading for a call (spec §14). */
    @Entity
    @Table(name = "call_sentiments", indexes = {
            @Index(name = "idx_sentiment_call", columnList = "call_id", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallSentiment extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "call_id", nullable = false, unique = true)
        private String callId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 16)
        private Sentiment overall;

        /** -1.0 (negative) to 1.0 (positive). */
        @Column(name = "score")
        private Double score;

        @Column(name = "positive_ratio")
        private Double positiveRatio;

        @Column(name = "neutral_ratio")
        private Double neutralRatio;

        @Column(name = "negative_ratio")
        private Double negativeRatio;
    }

    /** Detected customer intent and topics (spec §14). */
    @Entity
    @Table(name = "call_intents", indexes = {
            @Index(name = "idx_intent_call", columnList = "call_id"),
            @Index(name = "idx_intent_org", columnList = "organization_id,intent")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallIntent extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "call_id", nullable = false)
        private String callId;

        @Column(nullable = false, length = 128)
        private String intent;

        @Column(name = "confidence")
        private Double confidence;

        /** Comma-separated topic labels. */
        @Column(length = 512)
        private String topics;

        /** True for the call's dominant intent when several are detected. */
        @Column(name = "is_primary", nullable = false)
        @Builder.Default
        private boolean primary = true;
    }
}

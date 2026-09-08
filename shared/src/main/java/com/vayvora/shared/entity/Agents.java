package com.vayvora.shared.entity;

import com.vayvora.shared.enums.Enums.AgentStatus;
import com.vayvora.shared.enums.Enums.AgentType;
import com.vayvora.shared.enums.Enums.PromptVersionState;
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
 * AI agent tables (spec §23).
 *
 * <p>Agent configuration is versioned separately from the application
 * deployment (§6, §36) so agent behaviour can be revised without a platform
 * release. The {@code ai_agents} row holds identity and the pointer to the
 * currently published version; each {@code ai_agent_versions} row is an
 * immutable snapshot of a configuration.
 */
public final class Agents {

    private Agents() {
    }

    @Entity
    @Table(name = "ai_agents", indexes = {
            @Index(name = "idx_agent_org", columnList = "organization_id"),
            @Index(name = "idx_agent_status", columnList = "organization_id,status")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiAgent extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(nullable = false)
        private String name;

        @Enumerated(EnumType.STRING)
        @Column(name = "agent_type", nullable = false, length = 48)
        private AgentType agentType;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private AgentStatus status = AgentStatus.DRAFT;

        /** Version number currently serving traffic; null until first publish. */
        @Column(name = "published_version")
        private Integer publishedVersion;

        @Column(name = "current_version", nullable = false)
        @Builder.Default
        private Integer currentVersion = 1;

        @Column(name = "created_by")
        private String createdBy;

        @Column(name = "published_at")
        private Instant publishedAt;
    }

    /**
     * Immutable snapshot of an agent configuration.
     *
     * <p>List-valued fields (languages, personality, tools) are stored as
     * comma-separated text rather than join tables: they are small, always read
     * as a whole, and never queried by element.
     */
    @Entity
    @Table(name = "ai_agent_versions", uniqueConstraints = {
            @UniqueConstraint(name = "uk_agent_version",
                    columnNames = {"agent_id", "version"})
    }, indexes = {
            @Index(name = "idx_agent_version_agent", columnList = "agent_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiAgentVersion extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "agent_id", nullable = false)
        private String agentId;

        @Column(nullable = false)
        private Integer version;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private PromptVersionState state = PromptVersionState.DRAFT;

        /** Comma-separated BCP-47 tags, e.g. {@code en-IN,hi-IN,te-IN}. */
        @Column(name = "languages", length = 255)
        private String languages;

        @Column(name = "voice_id")
        private String voiceId;

        /** Comma-separated tone attributes, e.g. {@code PROFESSIONAL,CONCISE}. */
        @Column(name = "personality", length = 255)
        private String personality;

        @Column(name = "system_prompt", columnDefinition = "TEXT")
        private String systemPrompt;

        @Column(name = "knowledge_base_id")
        private String knowledgeBaseId;

        /** Comma-separated tool codes the agent may invoke (spec §12). */
        @Column(name = "enabled_tools", length = 1024)
        private String enabledTools;

        @Column(name = "max_call_minutes", nullable = false)
        @Builder.Default
        private Integer maxCallMinutes = 15;

        @Column(name = "calling_window_start", length = 8)
        @Builder.Default
        private String callingWindowStart = "09:00";

        @Column(name = "calling_window_end", length = 8)
        @Builder.Default
        private String callingWindowEnd = "20:00";

        @Column(name = "handoff_on_request", nullable = false)
        @Builder.Default
        private boolean handoffOnRequest = true;

        @Column(name = "handoff_on_negative_sentiment", nullable = false)
        @Builder.Default
        private boolean handoffOnNegativeSentiment = true;

        @Column(name = "handoff_on_billing_dispute", nullable = false)
        @Builder.Default
        private boolean handoffOnBillingDispute = false;

        @Column(name = "published_at")
        private Instant publishedAt;

        @Column(name = "published_by")
        private String publishedBy;
    }

    /**
     * Prompt text tracked independently of the agent version.
     *
     * <p>Lets an AI or product team iterate on wording and roll back without
     * touching the rest of the agent configuration.
     */
    @Entity
    @Table(name = "ai_prompts", indexes = {
            @Index(name = "idx_prompt_agent", columnList = "agent_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiPrompt extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "agent_id", nullable = false)
        private String agentId;

        @Column(nullable = false)
        private Integer version;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private PromptVersionState state = PromptVersionState.DRAFT;

        @Column(name = "content", columnDefinition = "TEXT", nullable = false)
        private String content;

        @Column(name = "change_note", length = 512)
        private String changeNote;

        @Column(name = "created_by")
        private String createdBy;
    }

    /** Catalogue of text-to-speech voices available to agents. */
    @Entity
    @Table(name = "ai_voices", indexes = {
            @Index(name = "idx_voice_code", columnList = "code", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiVoice extends BaseEntity {

        @Column(nullable = false, unique = true, length = 64)
        private String code;

        @Column(name = "display_name", nullable = false)
        private String displayName;

        @Column(nullable = false, length = 16)
        private String gender;

        /** BCP-47 locale, e.g. {@code en-IN}. */
        @Column(nullable = false, length = 16)
        private String locale;

        @Column(length = 64)
        private String provider;

        @Column(nullable = false)
        @Builder.Default
        private boolean active = true;
    }

    /**
     * A controlled backend function an agent may invoke during a call (spec §12).
     *
     * <p>Tools are explicit rows rather than free-form instructions: the agent
     * can only ever call something an administrator has registered and enabled.
     */
    @Entity
    @Table(name = "ai_tools", uniqueConstraints = {
            @UniqueConstraint(name = "uk_tool_org_code",
                    columnNames = {"organization_id", "code"})
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiTool extends BaseEntity {

        /** Null for platform-provided tools available to every organization. */
        @Column(name = "organization_id")
        private String organizationId;

        @Column(nullable = false, length = 96)
        private String code;

        @Column(name = "display_name", nullable = false)
        private String displayName;

        @Column(length = 512)
        private String description;

        /** JSON schema describing the tool's parameters. */
        @Column(name = "parameters_schema", columnDefinition = "TEXT")
        private String parametersSchema;

        /** Whether invoking this tool requires spoken customer confirmation. */
        @Column(name = "requires_confirmation", nullable = false)
        @Builder.Default
        private boolean requiresConfirmation = false;

        @Column(nullable = false)
        @Builder.Default
        private boolean active = true;
    }
}

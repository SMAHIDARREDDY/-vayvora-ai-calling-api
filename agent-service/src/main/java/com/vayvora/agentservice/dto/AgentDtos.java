package com.vayvora.agentservice.dto;

import com.vayvora.shared.entity.Agents;
import com.vayvora.shared.enums.Enums.AgentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Request and response payloads for the agent API. */
public final class AgentDtos {

    private AgentDtos() {
    }

    /** Splits the comma-separated storage form into a list. */
    static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return List.of(csv.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    static String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }

    /**
     * Agent configuration, covering every step of the builder workflow.
     *
     * <p>Used for both create and update: an update replaces the draft version
     * wholesale rather than patching fields, which keeps a version an accurate
     * snapshot of what was published.
     */
    public record AgentConfigRequest(
            @NotBlank(message = "Agent name is required")
            String name,

            @NotNull(message = "Agent type is required")
            AgentType agentType,

            List<String> languages,
            String voiceId,
            List<String> personality,
            String systemPrompt,
            String knowledgeBaseId,
            List<String> enabledTools,

            @Min(value = 1, message = "Maximum call duration must be at least 1 minute")
            @Max(value = 120, message = "Maximum call duration cannot exceed 120 minutes")
            Integer maxCallMinutes,

            String callingWindowStart,
            String callingWindowEnd,
            Boolean handoffOnRequest,
            Boolean handoffOnNegativeSentiment,
            Boolean handoffOnBillingDispute) {
    }

    public record AgentResponse(
            String id,
            String organizationId,
            String name,
            String agentType,
            String status,
            Integer currentVersion,
            Integer publishedVersion,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt,
            AgentVersionResponse configuration) {

        public static AgentResponse from(Agents.AiAgent agent, Agents.AiAgentVersion version) {
            return new AgentResponse(
                    agent.getId(),
                    agent.getOrganizationId(),
                    agent.getName(),
                    agent.getAgentType().name(),
                    agent.getStatus().name(),
                    agent.getCurrentVersion(),
                    agent.getPublishedVersion(),
                    agent.getPublishedAt(),
                    agent.getCreatedAt(),
                    agent.getUpdatedAt(),
                    version == null ? null : AgentVersionResponse.from(version));
        }
    }

    public record AgentVersionResponse(
            String id,
            Integer version,
            String state,
            List<String> languages,
            String voiceId,
            List<String> personality,
            String systemPrompt,
            String knowledgeBaseId,
            List<String> enabledTools,
            Integer maxCallMinutes,
            String callingWindowStart,
            String callingWindowEnd,
            boolean handoffOnRequest,
            boolean handoffOnNegativeSentiment,
            boolean handoffOnBillingDispute,
            Instant publishedAt) {

        public static AgentVersionResponse from(Agents.AiAgentVersion v) {
            return new AgentVersionResponse(
                    v.getId(),
                    v.getVersion(),
                    v.getState().name(),
                    split(v.getLanguages()),
                    v.getVoiceId(),
                    split(v.getPersonality()),
                    v.getSystemPrompt(),
                    v.getKnowledgeBaseId(),
                    split(v.getEnabledTools()),
                    v.getMaxCallMinutes(),
                    v.getCallingWindowStart(),
                    v.getCallingWindowEnd(),
                    v.isHandoffOnRequest(),
                    v.isHandoffOnNegativeSentiment(),
                    v.isHandoffOnBillingDispute(),
                    v.getPublishedAt());
        }
    }

    public record VoiceResponse(
            String id,
            String code,
            String displayName,
            String gender,
            String locale,
            String provider) {

        public static VoiceResponse from(Agents.AiVoice v) {
            return new VoiceResponse(v.getId(), v.getCode(), v.getDisplayName(),
                    v.getGender(), v.getLocale(), v.getProvider());
        }
    }

    public record ToolResponse(
            String id,
            String code,
            String displayName,
            String description,
            boolean requiresConfirmation) {

        public static ToolResponse from(Agents.AiTool t) {
            return new ToolResponse(t.getId(), t.getCode(), t.getDisplayName(),
                    t.getDescription(), t.isRequiresConfirmation());
        }
    }

    public record MessageResponse(String message) {
    }
}

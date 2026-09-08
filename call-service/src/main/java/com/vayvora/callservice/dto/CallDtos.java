package com.vayvora.callservice.dto;

import com.vayvora.shared.entity.Calls;
import com.vayvora.shared.enums.Enums.CallDirection;
import com.vayvora.shared.enums.Enums.CallOutcome;
import com.vayvora.shared.enums.Enums.CallState;
import com.vayvora.shared.enums.Enums.Sentiment;
import com.vayvora.shared.enums.Enums.SpeakerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Request and response payloads for the call API. */
public final class CallDtos {

    private CallDtos() {
    }

    /** Places an outbound call, or registers an inbound one already in progress. */
    public record InitiateCallRequest(
            @NotBlank(message = "Agent is required")
            String agentId,

            @NotBlank(message = "Customer number is required")
            String customerNumber,

            String contactId,
            String campaignId,
            String phoneNumberId,

            @NotNull(message = "Direction is required")
            CallDirection direction,

            String language) {
    }

    /**
     * Advances the call through its state machine (spec §10).
     *
     * <p>The service validates the transition; an illegal move is rejected
     * rather than silently applied.
     */
    public record UpdateCallStateRequest(
            @NotNull(message = "State is required")
            CallState state,
            CallOutcome outcome,
            String transferToUserId,
            String transferReason) {
    }

    /** Appends one conversational turn. */
    public record AppendMessageRequest(
            @NotNull(message = "Speaker is required")
            SpeakerType speaker,

            @NotBlank(message = "Content is required")
            String content,

            Integer offsetMs,
            Sentiment sentiment,
            String toolCode,
            String toolPayload) {
    }

    /** Attaches the post-call AI artifacts (spec §14, §15, §16). */
    public record CallIntelligenceRequest(
            String summary,
            List<String> actionItems,
            List<String> objections,
            Sentiment overallSentiment,
            Double sentimentScore,
            String intent,
            List<String> topics,
            Integer leadScore,
            Double talkRatio) {
    }

    public record CallResponse(
            String id,
            String organizationId,
            String agentId,
            Integer agentVersion,
            String campaignId,
            String contactId,
            String phoneNumberId,
            String customerNumber,
            String direction,
            String state,
            String outcome,
            Instant startedAt,
            Instant connectedAt,
            Instant endedAt,
            Integer durationSeconds,
            String language,
            Double talkRatio,
            Integer leadScore,
            String transferredToUserId,
            String transferReason) {

        public static CallResponse from(Calls.Call c) {
            return new CallResponse(
                    c.getId(),
                    c.getOrganizationId(),
                    c.getAgentId(),
                    c.getAgentVersion(),
                    c.getCampaignId(),
                    c.getContactId(),
                    c.getPhoneNumberId(),
                    c.getCustomerNumber(),
                    c.getDirection().name(),
                    c.getState().name(),
                    c.getOutcome() == null ? null : c.getOutcome().name(),
                    c.getStartedAt(),
                    c.getConnectedAt(),
                    c.getEndedAt(),
                    c.getDurationSeconds(),
                    c.getLanguage(),
                    c.getTalkRatio(),
                    c.getLeadScore(),
                    c.getTransferredToUserId(),
                    c.getTransferReason());
        }
    }

    public record MessageResponse(
            String id,
            Integer sequence,
            String speaker,
            String content,
            Integer offsetMs,
            String sentiment,
            String toolCode) {

        public static MessageResponse from(Calls.CallMessage m) {
            return new MessageResponse(
                    m.getId(),
                    m.getSequence(),
                    m.getSpeaker().name(),
                    m.getContent(),
                    m.getOffsetMs(),
                    m.getSentiment() == null ? null : m.getSentiment().name(),
                    m.getToolCode());
        }
    }

    /** Full transcript plus the turns that compose it. */
    public record TranscriptResponse(
            String callId,
            String language,
            String fullText,
            Integer wordCount,
            List<MessageResponse> messages) {
    }

    /** Everything derived from the call after it completed. */
    public record CallDetailResponse(
            CallResponse call,
            String summary,
            List<String> actionItems,
            List<String> objections,
            String sentiment,
            Double sentimentScore,
            String intent,
            List<String> topics,
            String recordingUrl,
            List<MessageResponse> transcript) {
    }

    public record AckResponse(String message) {
    }
}

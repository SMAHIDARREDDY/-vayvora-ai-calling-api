package com.vayvora.callservice.service;

import com.vayvora.callservice.dto.CallDtos.AppendMessageRequest;
import com.vayvora.callservice.dto.CallDtos.CallDetailResponse;
import com.vayvora.callservice.dto.CallDtos.CallIntelligenceRequest;
import com.vayvora.callservice.dto.CallDtos.CallResponse;
import com.vayvora.callservice.dto.CallDtos.InitiateCallRequest;
import com.vayvora.callservice.dto.CallDtos.MessageResponse;
import com.vayvora.callservice.dto.CallDtos.TranscriptResponse;
import com.vayvora.callservice.dto.CallDtos.UpdateCallStateRequest;
import com.vayvora.shared.entity.Calls;
import com.vayvora.shared.enums.Enums.CallState;
import com.vayvora.shared.enums.Enums.LeadBand;
import com.vayvora.shared.enums.Enums.ParticipantRole;
import com.vayvora.shared.enums.Enums.SpeakerType;
import com.vayvora.shared.repository.Repositories.CallIntentRepository;
import com.vayvora.shared.repository.Repositories.CallMessageRepository;
import com.vayvora.shared.repository.Repositories.CallParticipantRepository;
import com.vayvora.shared.repository.Repositories.CallRecordingRepository;
import com.vayvora.shared.repository.Repositories.CallRepository;
import com.vayvora.shared.repository.Repositories.CallSentimentRepository;
import com.vayvora.shared.repository.Repositories.CallSummaryRepository;
import com.vayvora.shared.repository.Repositories.CallTranscriptRepository;
import com.vayvora.shared.repository.Repositories.ContactRepository;
import com.vayvora.shared.tenant.TenantContext;
import com.vayvora.shared.web.Web.ConflictException;
import com.vayvora.shared.web.Web.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Call lifecycle and the artifacts a call produces.
 *
 * <p>The state machine is enforced here rather than trusted from the caller:
 * dashboards, billing and transfer logic all read call state, so a call that
 * jumps from RINGING to COMPLETED without ever connecting would silently
 * corrupt every one of them (spec §10).
 */
@Service
@RequiredArgsConstructor
public class CallService {

    /**
     * Legal transitions out of each state.
     *
     * <p>The conversational cycle LISTENING -> PROCESSING -> RESPONDING ->
     * LISTENING repeats per turn, so those states point back at one another.
     */
    private static final Map<CallState, Set<CallState>> TRANSITIONS =
            new EnumMap<>(CallState.class);

    static {
        TRANSITIONS.put(CallState.INITIATED,
                Set.of(CallState.RINGING, CallState.FAILED));
        TRANSITIONS.put(CallState.RINGING,
                Set.of(CallState.CONNECTED, CallState.NO_ANSWER,
                        CallState.BUSY, CallState.FAILED));
        TRANSITIONS.put(CallState.CONNECTED,
                Set.of(CallState.GREETING, CallState.LISTENING,
                        CallState.TRANSFER_REQUESTED, CallState.CALLBACK_REQUIRED,
                        CallState.COMPLETED, CallState.FAILED));
        TRANSITIONS.put(CallState.GREETING,
                Set.of(CallState.LISTENING, CallState.TRANSFER_REQUESTED,
                        CallState.COMPLETED, CallState.FAILED));
        TRANSITIONS.put(CallState.LISTENING,
                Set.of(CallState.PROCESSING, CallState.TRANSFER_REQUESTED,
                        CallState.CALLBACK_REQUIRED, CallState.COMPLETED,
                        CallState.FAILED));
        TRANSITIONS.put(CallState.PROCESSING,
                Set.of(CallState.RESPONDING, CallState.TRANSFER_REQUESTED,
                        CallState.COMPLETED, CallState.FAILED));
        TRANSITIONS.put(CallState.RESPONDING,
                Set.of(CallState.LISTENING, CallState.TRANSFER_REQUESTED,
                        CallState.CALLBACK_REQUIRED, CallState.COMPLETED,
                        CallState.FAILED));
        TRANSITIONS.put(CallState.TRANSFER_REQUESTED,
                Set.of(CallState.HUMAN_AGENT, CallState.COMPLETED, CallState.FAILED));
        TRANSITIONS.put(CallState.HUMAN_AGENT,
                Set.of(CallState.COMPLETED, CallState.FAILED));
        TRANSITIONS.put(CallState.CALLBACK_REQUIRED,
                Set.of(CallState.SCHEDULED, CallState.COMPLETED));
        TRANSITIONS.put(CallState.SCHEDULED, Set.of(CallState.COMPLETED));
        // Terminal states accept no further transitions.
        TRANSITIONS.put(CallState.COMPLETED, Set.of());
        TRANSITIONS.put(CallState.FAILED, Set.of());
        TRANSITIONS.put(CallState.NO_ANSWER, Set.of());
        TRANSITIONS.put(CallState.BUSY, Set.of());
    }

    private final CallRepository calls;
    private final CallMessageRepository messages;
    private final CallTranscriptRepository transcripts;
    private final CallSummaryRepository summaries;
    private final CallSentimentRepository sentiments;
    private final CallIntentRepository intents;
    private final CallRecordingRepository recordings;
    private final CallParticipantRepository participants;
    private final ContactRepository contacts;

    @Transactional
    public CallResponse initiate(InitiateCallRequest request) {
        String orgId = TenantContext.requireOrganizationId();

        Calls.Call call = calls.save(Calls.Call.builder()
                .organizationId(orgId)
                .agentId(request.agentId())
                .contactId(request.contactId())
                .campaignId(request.campaignId())
                .phoneNumberId(request.phoneNumberId())
                .customerNumber(request.customerNumber())
                .direction(request.direction())
                .state(CallState.INITIATED)
                .language(request.language())
                .startedAt(Instant.now())
                .build());

        participants.save(Calls.CallParticipant.builder()
                .organizationId(orgId)
                .callId(call.getId())
                .role(ParticipantRole.AI_AGENT)
                .participantRef(request.agentId())
                .joinedAt(Instant.now())
                .build());

        return CallResponse.from(call);
    }

    @Transactional(readOnly = true)
    public Page<CallResponse> list(CallState state, String agentId, Pageable pageable) {
        String orgId = TenantContext.requireOrganizationId();

        Page<Calls.Call> page;
        if (state != null) {
            page = calls.findByOrganizationIdAndState(orgId, state, pageable);
        } else if (agentId != null && !agentId.isBlank()) {
            page = calls.findByOrganizationIdAndAgentId(orgId, agentId, pageable);
        } else {
            page = calls.findByOrganizationIdOrderByStartedAtDesc(orgId, pageable);
        }
        return page.map(CallResponse::from);
    }

    /** Calls currently in a live state, for the monitoring view. */
    @Transactional(readOnly = true)
    public List<CallResponse> live() {
        String orgId = TenantContext.requireOrganizationId();
        List<CallState> liveStates = List.of(CallState.values()).stream()
                .filter(CallState::isLive)
                .toList();
        return calls.findByOrganizationIdAndStateIn(orgId, liveStates).stream()
                .map(CallResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CallResponse get(String callId) {
        return CallResponse.from(require(callId));
    }

    /**
     * Applies a state transition.
     *
     * <p>Timestamps are set as side effects of specific transitions:
     * {@code connectedAt} when the call first connects, since billing meters
     * voice minutes from that instant, and {@code endedAt} plus duration on
     * reaching a terminal state.
     */
    @Transactional
    public CallResponse updateState(String callId, UpdateCallStateRequest request) {
        Calls.Call call = require(callId);
        CallState from = call.getState();
        CallState to = request.state();

        if (from == to) {
            return CallResponse.from(call);
        }
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new ConflictException(
                    "Illegal call transition " + from + " -> " + to);
        }

        call.setState(to);

        if (to == CallState.CONNECTED && call.getConnectedAt() == null) {
            call.setConnectedAt(Instant.now());
        }
        if (to == CallState.TRANSFER_REQUESTED || to == CallState.HUMAN_AGENT) {
            call.setTransferredToUserId(request.transferToUserId());
            call.setTransferReason(request.transferReason());
        }
        if (to.isTerminal()) {
            Instant endedAt = Instant.now();
            call.setEndedAt(endedAt);
            Instant from0 = call.getConnectedAt() != null
                    ? call.getConnectedAt()
                    : call.getStartedAt();
            call.setDurationSeconds(from0 == null
                    ? 0
                    : (int) Duration.between(from0, endedAt).toSeconds());
        }
        if (request.outcome() != null) {
            call.setOutcome(request.outcome());
        }

        return CallResponse.from(calls.save(call));
    }

    /**
     * Appends a transcript turn.
     *
     * <p>Sequence numbers come from the persisted maximum rather than a
     * counter held in memory, so a restart mid-call cannot reuse a number.
     */
    @Transactional
    public MessageResponse appendMessage(String callId, AppendMessageRequest request) {
        Calls.Call call = require(callId);

        int next = messages.maxSequence(callId) + 1;
        Calls.CallMessage message = messages.save(Calls.CallMessage.builder()
                .organizationId(call.getOrganizationId())
                .callId(callId)
                .sequence(next)
                .speaker(request.speaker())
                .content(request.content())
                .offsetMs(request.offsetMs())
                .sentiment(request.sentiment())
                .toolCode(request.toolCode())
                .toolPayload(request.toolPayload())
                .build());

        return MessageResponse.from(message);
    }

    @Transactional(readOnly = true)
    public TranscriptResponse transcript(String callId) {
        Calls.Call call = require(callId);
        List<Calls.CallMessage> turns = messages.findByCallIdOrderBySequenceAsc(callId);

        Calls.CallTranscript stored = transcripts.findByCallId(callId).orElse(null);
        String fullText = stored != null && stored.getFullText() != null
                ? stored.getFullText()
                : turns.stream()
                        .map(m -> m.getSpeaker() + ": " + m.getContent())
                        .collect(Collectors.joining("\n"));

        return new TranscriptResponse(
                callId,
                stored != null ? stored.getLanguage() : call.getLanguage(),
                fullText,
                stored != null ? stored.getWordCount() : countWords(fullText),
                turns.stream().map(MessageResponse::from).toList());
    }

    /**
     * Stores the post-call AI artifacts and applies the lead score.
     *
     * <p>Written in one transaction because the dashboard treats a call as
     * "analysed" once any of these exist; a partial write would show a call
     * with a sentiment but no summary.
     */
    @Transactional
    public CallDetailResponse attachIntelligence(String callId,
                                                 CallIntelligenceRequest request) {
        Calls.Call call = require(callId);
        String orgId = call.getOrganizationId();

        if (request.summary() != null || request.actionItems() != null) {
            Calls.CallSummary summary = summaries.findByCallId(callId)
                    .orElseGet(() -> Calls.CallSummary.builder()
                            .organizationId(orgId).callId(callId).build());
            summary.setSummary(request.summary());
            summary.setActionItems(joinLines(request.actionItems()));
            summary.setObjections(joinLines(request.objections()));
            summaries.save(summary);
        }

        if (request.overallSentiment() != null) {
            Calls.CallSentiment sentiment = sentiments.findByCallId(callId)
                    .orElseGet(() -> Calls.CallSentiment.builder()
                            .organizationId(orgId).callId(callId).build());
            sentiment.setOverall(request.overallSentiment());
            sentiment.setScore(request.sentimentScore());
            sentiments.save(sentiment);
        }

        if (request.intent() != null && !request.intent().isBlank()) {
            intents.save(Calls.CallIntent.builder()
                    .organizationId(orgId)
                    .callId(callId)
                    .intent(request.intent())
                    .topics(request.topics() == null
                            ? null : String.join(",", request.topics()))
                    .primary(true)
                    .build());
        }

        if (request.talkRatio() != null) {
            call.setTalkRatio(request.talkRatio());
        }
        if (request.leadScore() != null) {
            call.setLeadScore(request.leadScore());
            applyLeadScoreToContact(call.getContactId(), orgId, request.leadScore());
        }
        calls.save(call);

        return detail(callId);
    }

    @Transactional(readOnly = true)
    public CallDetailResponse detail(String callId) {
        Calls.Call call = require(callId);

        Calls.CallSummary summary = summaries.findByCallId(callId).orElse(null);
        Calls.CallSentiment sentiment = sentiments.findByCallId(callId).orElse(null);
        List<Calls.CallIntent> callIntents = intents.findByCallId(callId);
        Calls.CallRecording recording = recordings.findByCallId(callId).orElse(null);

        Calls.CallIntent primary = callIntents.stream()
                .filter(Calls.CallIntent::isPrimary)
                .findFirst()
                .orElse(callIntents.isEmpty() ? null : callIntents.get(0));

        return new CallDetailResponse(
                CallResponse.from(call),
                summary == null ? null : summary.getSummary(),
                summary == null ? List.of() : splitLines(summary.getActionItems()),
                summary == null ? List.of() : splitLines(summary.getObjections()),
                sentiment == null ? null : sentiment.getOverall().name(),
                sentiment == null ? null : sentiment.getScore(),
                primary == null ? null : primary.getIntent(),
                primary == null || primary.getTopics() == null
                        ? List.of()
                        : List.of(primary.getTopics().split(",")),
                recording == null ? null : recording.getStorageKey(),
                messages.findByCallIdOrderBySequenceAsc(callId).stream()
                        .map(MessageResponse::from)
                        .toList());
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private Calls.Call require(String callId) {
        return calls.findByIdAndOrganizationId(callId, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Call " + callId + " not found"));
    }

    /** Keeps the contact's denormalized score and band in step with the call. */
    private void applyLeadScoreToContact(String contactId, String orgId, int score) {
        if (contactId == null) {
            return;
        }
        contacts.findByIdAndOrganizationId(contactId, orgId).ifPresent(contact -> {
            contact.applyScore(score);
            contact.setLastContactedAt(Instant.now());
            contacts.save(contact);
        });
    }

    private static String joinLines(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join("\n", values);
    }

    private static List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\n")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /** Exposed for the analytics service's band rollups. */
    public static LeadBand bandFor(int score) {
        return LeadBand.fromScore(score);
    }

    /** Speaker types that count as the AI side when computing talk ratio. */
    public static boolean isAiSide(SpeakerType speaker) {
        return speaker == SpeakerType.AI;
    }
}

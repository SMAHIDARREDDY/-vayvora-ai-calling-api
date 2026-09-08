package com.vayvora.campaignservice.dto;

import com.vayvora.shared.entity.Calls;
import com.vayvora.shared.entity.Crm;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

/** Request and response payloads for campaigns and contacts. */
public final class CampaignDtos {

    private CampaignDtos() {
    }

    public record CreateCampaignRequest(
            @NotBlank(message = "Campaign name is required")
            String name,

            @NotBlank(message = "Agent is required")
            String agentId,

            String phoneNumberId,
            Instant scheduledStartAt,
            String callWindowStart,
            String callWindowEnd,

            @Min(value = 1, message = "At least one attempt is required")
            @Max(value = 10, message = "No more than 10 attempts per contact")
            Integer maxAttempts,

            @Min(value = 1, message = "Concurrency must be at least 1")
            @Max(value = 100, message = "Concurrency cannot exceed 100")
            Integer concurrency,

            /** Contacts to enrol at creation time. */
            List<String> contactIds) {
    }

    /** Adds contacts to an existing campaign. */
    public record AddContactsRequest(List<String> contactIds) {
    }

    public record CampaignResponse(
            String id,
            String organizationId,
            String name,
            String agentId,
            String phoneNumberId,
            String state,
            Instant scheduledStartAt,
            Instant startedAt,
            Instant completedAt,
            String callWindowStart,
            String callWindowEnd,
            Integer maxAttempts,
            Integer concurrency,
            Instant createdAt) {

        public static CampaignResponse from(Calls.Campaign c) {
            return new CampaignResponse(
                    c.getId(),
                    c.getOrganizationId(),
                    c.getName(),
                    c.getAgentId(),
                    c.getPhoneNumberId(),
                    c.getState().name(),
                    c.getScheduledStartAt(),
                    c.getStartedAt(),
                    c.getCompletedAt(),
                    c.getCallWindowStart(),
                    c.getCallWindowEnd(),
                    c.getMaxAttempts(),
                    c.getConcurrency(),
                    c.getCreatedAt());
        }
    }

    /**
     * Aggregate outcomes for a campaign (spec §9).
     *
     * <p>Computed from {@code campaign_contacts} on read rather than kept as
     * counters, so a corrected attempt cannot leave the totals inconsistent.
     */
    public record CampaignMetricsResponse(
            String campaignId,
            long totalContacts,
            long attempted,
            long connected,
            long noAnswer,
            long busy,
            long failed,
            long interested,
            long notInterested,
            long followUp,
            long converted,
            double progress,
            double connectRate,
            double conversionRate) {
    }

    public record CreateContactRequest(
            @NotBlank(message = "First name is required")
            String firstName,

            String lastName,

            @NotBlank(message = "Phone number is required")
            String phone,

            @Email(message = "Enter a valid email address")
            String email,

            String companyName,
            String preferredLanguage,
            String nextAction,
            String ownerUserId) {
    }

    public record ContactResponse(
            String id,
            String organizationId,
            String firstName,
            String lastName,
            String fullName,
            String phone,
            String email,
            String companyName,
            String preferredLanguage,
            Integer leadScore,
            String leadBand,
            String leadStatus,
            String nextAction,
            String ownerUserId,
            Instant lastContactedAt,
            boolean doNotCall) {

        public static ContactResponse from(Crm.Contact c) {
            return new ContactResponse(
                    c.getId(),
                    c.getOrganizationId(),
                    c.getFirstName(),
                    c.getLastName(),
                    c.fullName(),
                    c.getPhone(),
                    c.getEmail(),
                    c.getCompanyName(),
                    c.getPreferredLanguage(),
                    c.getLeadScore(),
                    c.getLeadBand().name(),
                    c.getLeadStatus().name(),
                    c.getNextAction(),
                    c.getOwnerUserId(),
                    c.getLastContactedAt(),
                    c.isDoNotCall());
        }
    }

    public record CampaignContactResponse(
            String id,
            String contactId,
            String state,
            Integer attempts,
            Instant lastAttemptAt,
            Instant nextAttemptAt,
            String outcome,
            String lastCallId) {

        public static CampaignContactResponse from(Calls.CampaignContact cc) {
            return new CampaignContactResponse(
                    cc.getId(),
                    cc.getContactId(),
                    cc.getState().name(),
                    cc.getAttempts(),
                    cc.getLastAttemptAt(),
                    cc.getNextAttemptAt(),
                    cc.getOutcome() == null ? null : cc.getOutcome().name(),
                    cc.getLastCallId());
        }
    }

    public record MessageResponse(String message) {
    }
}

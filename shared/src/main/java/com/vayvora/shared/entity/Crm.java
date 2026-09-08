package com.vayvora.shared.entity;

import com.vayvora.shared.enums.Enums.AppointmentStatus;
import com.vayvora.shared.enums.Enums.LeadBand;
import com.vayvora.shared.enums.Enums.LeadStatus;
import com.vayvora.shared.enums.Enums.PhoneNumberDirection;
import com.vayvora.shared.enums.Enums.TaskStatus;
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
 * Telephony, contact, lead and appointment tables (spec §17, §20, §23).
 */
public final class Crm {

    private Crm() {
    }

    // -----------------------------------------------------------------------
    // Telephony
    // -----------------------------------------------------------------------

    @Entity
    @Table(name = "telephony_providers", indexes = {
            @Index(name = "idx_provider_code", columnList = "code", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TelephonyProvider extends BaseEntity {

        @Column(nullable = false, unique = true, length = 64)
        private String code;

        @Column(name = "display_name", nullable = false)
        private String displayName;

        /** SIP gateway or provider API endpoint (spec §8). */
        @Column(name = "gateway_uri", length = 512)
        private String gatewayUri;

        @Column(nullable = false)
        @Builder.Default
        private boolean active = true;
    }

    @Entity
    @Table(name = "phone_numbers", indexes = {
            @Index(name = "idx_phone_org", columnList = "organization_id"),
            @Index(name = "idx_phone_e164", columnList = "e164", unique = true)
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PhoneNumber extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        /** E.164 format, e.g. {@code +914045678900}. */
        @Column(nullable = false, unique = true, length = 24)
        private String e164;

        @Column(length = 128)
        private String label;

        @Column(name = "provider_id")
        private String providerId;

        /** Agent answering inbound calls on this number; null if unassigned. */
        @Column(name = "assigned_agent_id")
        private String assignedAgentId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 16)
        @Builder.Default
        private PhoneNumberDirection direction = PhoneNumberDirection.BOTH;

        @Column(name = "monthly_cost_minor")
        private Long monthlyCostMinor;

        @Column(nullable = false)
        @Builder.Default
        private boolean active = true;
    }

    // -----------------------------------------------------------------------
    // Contacts, companies, leads
    // -----------------------------------------------------------------------

    @Entity
    @Table(name = "companies", indexes = {
            @Index(name = "idx_company_org", columnList = "organization_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Company extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(nullable = false)
        private String name;

        @Column(length = 128)
        private String industry;

        @Column(length = 255)
        private String website;

        @Column(name = "employee_count")
        private Integer employeeCount;
    }

    @Entity
    @Table(name = "contacts", uniqueConstraints = {
            @UniqueConstraint(name = "uk_contact_org_phone",
                    columnNames = {"organization_id", "phone"})
    }, indexes = {
            @Index(name = "idx_contact_org", columnList = "organization_id"),
            @Index(name = "idx_contact_phone", columnList = "phone"),
            @Index(name = "idx_contact_score", columnList = "organization_id,lead_score")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Contact extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "first_name", nullable = false)
        private String firstName;

        @Column(name = "last_name")
        private String lastName;

        /** E.164 format. Unique per organization. */
        @Column(nullable = false, length = 24)
        private String phone;

        @Column(length = 255)
        private String email;

        @Column(name = "company_id")
        private String companyId;

        /** Denormalized for list views that would otherwise join on every row. */
        @Column(name = "company_name", length = 255)
        private String companyName;

        @Column(name = "preferred_language", length = 16)
        private String preferredLanguage;

        @Column(name = "lead_score", nullable = false)
        @Builder.Default
        private Integer leadScore = 0;

        @Enumerated(EnumType.STRING)
        @Column(name = "lead_band", nullable = false, length = 16)
        @Builder.Default
        private LeadBand leadBand = LeadBand.LOW;

        @Enumerated(EnumType.STRING)
        @Column(name = "lead_status", nullable = false, length = 32)
        @Builder.Default
        private LeadStatus leadStatus = LeadStatus.NEW;

        @Column(name = "next_action", length = 255)
        private String nextAction;

        @Column(name = "owner_user_id")
        private String ownerUserId;

        @Column(name = "last_contacted_at")
        private Instant lastContactedAt;

        /** Contact has asked not to be called again (spec §34). */
        @Column(name = "do_not_call", nullable = false)
        @Builder.Default
        private boolean doNotCall = false;

        public String fullName() {
            return lastName == null || lastName.isBlank()
                    ? firstName
                    : firstName + " " + lastName;
        }

        /** Keeps the denormalized band in step with the score. */
        public void applyScore(int score) {
            this.leadScore = score;
            this.leadBand = LeadBand.fromScore(score);
        }
    }

    /**
     * A qualification opportunity attached to a contact.
     *
     * <p>Separate from the contact so one person can be worked more than once —
     * a lost lead this quarter and a fresh one the next.
     */
    @Entity
    @Table(name = "leads", indexes = {
            @Index(name = "idx_lead_org", columnList = "organization_id"),
            @Index(name = "idx_lead_contact", columnList = "contact_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Lead extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "contact_id", nullable = false)
        private String contactId;

        @Column(name = "campaign_id")
        private String campaignId;

        /** Call on which this lead was captured. */
        @Column(name = "source_call_id")
        private String sourceCallId;

        @Column(nullable = false)
        @Builder.Default
        private Integer score = 0;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 16)
        @Builder.Default
        private LeadBand band = LeadBand.LOW;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private LeadStatus status = LeadStatus.NEW;

        /** Comma-separated signal codes that produced the score (spec §16). */
        @Column(name = "score_signals", length = 512)
        private String scoreSignals;

        @Column(name = "requirement", length = 1024)
        private String requirement;

        @Column(name = "budget", length = 128)
        private String budget;

        @Column(name = "timeline", length = 128)
        private String timeline;

        @Column(name = "is_decision_maker")
        private Boolean decisionMaker;

        @Column(name = "owner_user_id")
        private String ownerUserId;
    }

    // -----------------------------------------------------------------------
    // Appointments, tasks, notes
    // -----------------------------------------------------------------------

    @Entity
    @Table(name = "appointments", indexes = {
            @Index(name = "idx_appt_org", columnList = "organization_id"),
            @Index(name = "idx_appt_time", columnList = "organization_id,scheduled_at")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Appointment extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "contact_id", nullable = false)
        private String contactId;

        @Column(name = "call_id")
        private String callId;

        /** Agent that booked the appointment, when booked by tool call. */
        @Column(name = "booked_by_agent_id")
        private String bookedByAgentId;

        @Column(name = "assigned_user_id")
        private String assignedUserId;

        @Column(name = "scheduled_at", nullable = false)
        private Instant scheduledAt;

        @Column(name = "duration_minutes", nullable = false)
        @Builder.Default
        private Integer durationMinutes = 30;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private AppointmentStatus status = AppointmentStatus.CONFIRMED;

        @Column(length = 512)
        private String subject;

        @Column(name = "external_event_id", length = 255)
        private String externalEventId;
    }

    @Entity
    @Table(name = "tasks", indexes = {
            @Index(name = "idx_task_org", columnList = "organization_id"),
            @Index(name = "idx_task_assignee", columnList = "assigned_user_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Task extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(nullable = false, length = 512)
        private String title;

        @Column(columnDefinition = "TEXT")
        private String description;

        @Column(name = "contact_id")
        private String contactId;

        @Column(name = "call_id")
        private String callId;

        @Column(name = "assigned_user_id")
        private String assignedUserId;

        @Column(name = "due_at")
        private Instant dueAt;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 32)
        @Builder.Default
        private TaskStatus status = TaskStatus.OPEN;
    }

    @Entity
    @Table(name = "notes", indexes = {
            @Index(name = "idx_note_org", columnList = "organization_id"),
            @Index(name = "idx_note_contact", columnList = "contact_id")
    })
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Note extends BaseEntity {

        @Column(name = "organization_id", nullable = false)
        private String organizationId;

        @Column(name = "contact_id")
        private String contactId;

        @Column(name = "call_id")
        private String callId;

        @Column(name = "author_user_id")
        private String authorUserId;

        @Column(nullable = false, columnDefinition = "TEXT")
        private String body;
    }
}

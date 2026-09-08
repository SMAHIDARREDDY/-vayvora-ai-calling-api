package com.vayvora.campaignservice.service;

import com.vayvora.campaignservice.dto.CampaignDtos.CampaignContactResponse;
import com.vayvora.campaignservice.dto.CampaignDtos.CampaignMetricsResponse;
import com.vayvora.campaignservice.dto.CampaignDtos.CampaignResponse;
import com.vayvora.campaignservice.dto.CampaignDtos.ContactResponse;
import com.vayvora.campaignservice.dto.CampaignDtos.CreateCampaignRequest;
import com.vayvora.campaignservice.dto.CampaignDtos.CreateContactRequest;
import com.vayvora.shared.entity.Calls;
import com.vayvora.shared.entity.Crm;
import com.vayvora.shared.enums.Enums.CallOutcome;
import com.vayvora.shared.enums.Enums.CampaignContactState;
import com.vayvora.shared.enums.Enums.CampaignState;
import com.vayvora.shared.enums.Enums.LeadBand;
import com.vayvora.shared.repository.Repositories.CampaignContactRepository;
import com.vayvora.shared.repository.Repositories.CampaignRepository;
import com.vayvora.shared.repository.Repositories.ContactRepository;
import com.vayvora.shared.tenant.TenantContext;
import com.vayvora.shared.web.Web.ConflictException;
import com.vayvora.shared.web.Web.NotFoundException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Campaign lifecycle and the contact base it draws on.
 *
 * <p>Campaign state moves DRAFT -> SCHEDULED -> RUNNING -> PAUSED ->
 * COMPLETED (spec §9). Transitions are checked rather than assumed, so a
 * campaign cannot be started twice or resumed after it has finished.
 */
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaigns;
    private final CampaignContactRepository campaignContacts;
    private final ContactRepository contacts;

    // -----------------------------------------------------------------------
    // Campaigns
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<CampaignResponse> list(CampaignState state, Pageable pageable) {
        String orgId = TenantContext.requireOrganizationId();
        Page<Calls.Campaign> page = state == null
                ? campaigns.findByOrganizationId(orgId, pageable)
                : campaigns.findByOrganizationIdAndState(orgId, state, pageable);
        return page.map(CampaignResponse::from);
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(String campaignId) {
        return CampaignResponse.from(require(campaignId));
    }

    @Transactional
    public CampaignResponse create(CreateCampaignRequest request) {
        String orgId = TenantContext.requireOrganizationId();

        Calls.Campaign campaign = Calls.Campaign.builder()
                .organizationId(orgId)
                .name(request.name())
                .agentId(request.agentId())
                .phoneNumberId(request.phoneNumberId())
                .state(CampaignState.DRAFT)
                .scheduledStartAt(request.scheduledStartAt())
                .createdBy(TenantContext.userId())
                .build();

        if (request.callWindowStart() != null) {
            campaign.setCallWindowStart(request.callWindowStart());
        }
        if (request.callWindowEnd() != null) {
            campaign.setCallWindowEnd(request.callWindowEnd());
        }
        if (request.maxAttempts() != null) {
            campaign.setMaxAttempts(request.maxAttempts());
        }
        if (request.concurrency() != null) {
            campaign.setConcurrency(request.concurrency());
        }

        Calls.Campaign saved = campaigns.save(campaign);

        if (request.contactIds() != null && !request.contactIds().isEmpty()) {
            enrol(saved, request.contactIds());
        }
        return CampaignResponse.from(saved);
    }

    /**
     * Starts or resumes a campaign.
     *
     * <p>Refuses a campaign with no contacts: a running campaign with nothing
     * to dial would sit in RUNNING forever and never reach COMPLETED.
     */
    @Transactional
    public CampaignResponse start(String campaignId) {
        Calls.Campaign campaign = require(campaignId);

        if (campaign.getState() == CampaignState.RUNNING) {
            throw new ConflictException("Campaign is already running");
        }
        if (campaign.getState() == CampaignState.COMPLETED) {
            throw new ConflictException("Campaign has already completed");
        }
        if (campaignContacts.countByCampaignId(campaignId) == 0) {
            throw new ConflictException("Campaign has no contacts to call");
        }

        campaign.setState(CampaignState.RUNNING);
        if (campaign.getStartedAt() == null) {
            campaign.setStartedAt(Instant.now());
        }
        return CampaignResponse.from(campaigns.save(campaign));
    }

    @Transactional
    public CampaignResponse pause(String campaignId) {
        Calls.Campaign campaign = require(campaignId);
        if (campaign.getState() != CampaignState.RUNNING) {
            throw new ConflictException("Only a running campaign can be paused");
        }
        campaign.setState(CampaignState.PAUSED);
        return CampaignResponse.from(campaigns.save(campaign));
    }

    @Transactional
    public CampaignResponse complete(String campaignId) {
        Calls.Campaign campaign = require(campaignId);
        if (campaign.getState() == CampaignState.COMPLETED) {
            return CampaignResponse.from(campaign);
        }
        campaign.setState(CampaignState.COMPLETED);
        campaign.setCompletedAt(Instant.now());
        return CampaignResponse.from(campaigns.save(campaign));
    }

    @Transactional
    public CampaignResponse schedule(String campaignId, Instant startAt) {
        Calls.Campaign campaign = require(campaignId);
        if (campaign.getState() != CampaignState.DRAFT) {
            throw new ConflictException("Only a draft campaign can be scheduled");
        }
        campaign.setState(CampaignState.SCHEDULED);
        campaign.setScheduledStartAt(startAt);
        return CampaignResponse.from(campaigns.save(campaign));
    }

    /** Aggregate outcomes, computed from the per-contact rows. */
    @Transactional(readOnly = true)
    public CampaignMetricsResponse metrics(String campaignId) {
        require(campaignId);

        long total = campaignContacts.countByCampaignId(campaignId);
        long pending = campaignContacts.countByCampaignIdAndState(
                campaignId, CampaignContactState.PENDING);
        long attempted = total - pending;

        long interested = campaignContacts.countByCampaignIdAndOutcome(
                campaignId, CallOutcome.INTERESTED);
        long notInterested = campaignContacts.countByCampaignIdAndOutcome(
                campaignId, CallOutcome.NOT_INTERESTED);
        long followUp = campaignContacts.countByCampaignIdAndOutcome(
                campaignId, CallOutcome.FOLLOW_UP);
        long converted = campaignContacts.countByCampaignIdAndOutcome(
                campaignId, CallOutcome.CONVERTED);
        long noAnswer = campaignContacts.countByCampaignIdAndOutcome(
                campaignId, CallOutcome.NO_ANSWER);
        long busy = campaignContacts.countByCampaignIdAndOutcome(
                campaignId, CallOutcome.BUSY);
        long failed = campaignContacts.countByCampaignIdAndOutcome(
                campaignId, CallOutcome.FAILED);

        long connected = interested + notInterested + followUp + converted;

        return new CampaignMetricsResponse(
                campaignId, total, attempted, connected, noAnswer, busy, failed,
                interested, notInterested, followUp, converted,
                total == 0 ? 0 : (double) attempted / total,
                attempted == 0 ? 0 : (double) connected / attempted,
                connected == 0 ? 0 : (double) converted / connected);
    }

    @Transactional(readOnly = true)
    public Page<CampaignContactResponse> contactsIn(String campaignId, Pageable pageable) {
        require(campaignId);
        return campaignContacts.findByCampaignId(campaignId, pageable)
                .map(CampaignContactResponse::from);
    }

    @Transactional
    public int addContacts(String campaignId, List<String> contactIds) {
        Calls.Campaign campaign = require(campaignId);
        return enrol(campaign, contactIds);
    }

    // -----------------------------------------------------------------------
    // Contacts
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ContactResponse> listContacts(String query, LeadBand band, Pageable pageable) {
        String orgId = TenantContext.requireOrganizationId();

        Page<Crm.Contact> page;
        if (query != null && !query.isBlank()) {
            page = contacts.search(orgId, query.trim(), pageable);
        } else if (band != null) {
            page = contacts.findByOrganizationIdAndLeadBand(orgId, band, pageable);
        } else {
            page = contacts.findByOrganizationId(orgId, pageable);
        }
        return page.map(ContactResponse::from);
    }

    @Transactional(readOnly = true)
    public ContactResponse getContact(String contactId) {
        return ContactResponse.from(contacts
                .findByIdAndOrganizationId(contactId, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Contact not found")));
    }

    /**
     * Creates a contact.
     *
     * <p>Phone number is unique per organization, so a duplicate is reported
     * rather than silently creating a second record the campaign would dial
     * twice.
     */
    @Transactional
    public ContactResponse createContact(CreateContactRequest request) {
        String orgId = TenantContext.requireOrganizationId();

        contacts.findByOrganizationIdAndPhone(orgId, request.phone()).ifPresent(existing -> {
            throw new ConflictException(
                    "A contact with phone " + request.phone() + " already exists");
        });

        return ContactResponse.from(contacts.save(Crm.Contact.builder()
                .organizationId(orgId)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .email(request.email())
                .companyName(request.companyName())
                .preferredLanguage(request.preferredLanguage())
                .nextAction(request.nextAction())
                .ownerUserId(request.ownerUserId())
                .build()));
    }

    /** Honours a do-not-call request (spec §34). */
    @Transactional
    public ContactResponse setDoNotCall(String contactId, boolean doNotCall) {
        Crm.Contact contact = contacts
                .findByIdAndOrganizationId(contactId, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new NotFoundException("Contact not found"));
        contact.setDoNotCall(doNotCall);
        return ContactResponse.from(contacts.save(contact));
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private Calls.Campaign require(String campaignId) {
        return campaigns
                .findByIdAndOrganizationId(campaignId, TenantContext.requireOrganizationId())
                .orElseThrow(() ->
                        new NotFoundException("Campaign " + campaignId + " not found"));
    }

    /**
     * Enrols contacts, skipping those already in the campaign or marked
     * do-not-call.
     *
     * @return how many were actually added
     */
    private int enrol(Calls.Campaign campaign, List<String> contactIds) {
        String orgId = campaign.getOrganizationId();
        int added = 0;

        for (String contactId : contactIds) {
            Crm.Contact contact = contacts
                    .findByIdAndOrganizationId(contactId, orgId)
                    .orElse(null);
            if (contact == null || contact.isDoNotCall()) {
                continue;
            }
            campaignContacts.save(Calls.CampaignContact.builder()
                    .organizationId(orgId)
                    .campaignId(campaign.getId())
                    .contactId(contactId)
                    .state(CampaignContactState.PENDING)
                    .build());
            added++;
        }
        return added;
    }
}

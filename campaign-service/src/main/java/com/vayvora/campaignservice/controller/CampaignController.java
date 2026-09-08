package com.vayvora.campaignservice.controller;

import com.vayvora.campaignservice.dto.CampaignDtos.AddContactsRequest;
import com.vayvora.campaignservice.dto.CampaignDtos.CampaignContactResponse;
import com.vayvora.campaignservice.dto.CampaignDtos.CampaignMetricsResponse;
import com.vayvora.campaignservice.dto.CampaignDtos.CampaignResponse;
import com.vayvora.campaignservice.dto.CampaignDtos.ContactResponse;
import com.vayvora.campaignservice.dto.CampaignDtos.CreateCampaignRequest;
import com.vayvora.campaignservice.dto.CampaignDtos.CreateContactRequest;
import com.vayvora.campaignservice.dto.CampaignDtos.MessageResponse;
import com.vayvora.campaignservice.service.CampaignService;
import com.vayvora.shared.enums.Enums.CampaignState;
import com.vayvora.shared.enums.Enums.LeadBand;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Campaign endpoints (spec §30). */
@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CampaignService campaignService;

    @GetMapping
    public Page<CampaignResponse> list(
            @RequestParam(required = false) CampaignState state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return campaignService.list(state,
                PageRequest.of(Math.max(0, page), clamp(size),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    public CampaignResponse get(@PathVariable String id) {
        return campaignService.get(id);
    }

    @GetMapping("/{id}/metrics")
    public CampaignMetricsResponse metrics(@PathVariable String id) {
        return campaignService.metrics(id);
    }

    @GetMapping("/{id}/contacts")
    public Page<CampaignContactResponse> contacts(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return campaignService.contactsIn(id,
                PageRequest.of(Math.max(0, page), clamp(size)));
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(
            @Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.create(request));
    }

    @PostMapping("/{id}/contacts")
    public MessageResponse addContacts(@PathVariable String id,
                                       @RequestBody AddContactsRequest request) {
        int added = campaignService.addContacts(id, request.contactIds());
        return new MessageResponse(added + " contact(s) added to the campaign");
    }

    @PostMapping("/{id}/start")
    public CampaignResponse start(@PathVariable String id) {
        return campaignService.start(id);
    }

    @PostMapping("/{id}/pause")
    public CampaignResponse pause(@PathVariable String id) {
        return campaignService.pause(id);
    }

    @PostMapping("/{id}/complete")
    public CampaignResponse complete(@PathVariable String id) {
        return campaignService.complete(id);
    }

    @PostMapping("/{id}/schedule")
    public CampaignResponse schedule(@PathVariable String id,
                                     @RequestParam Instant startAt) {
        return campaignService.schedule(id, startAt);
    }

    private static int clamp(int size) {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }
}

/** Contact endpoints, served by the same service (spec §17, §30). */
@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
class ContactController {

    private static final int MAX_PAGE_SIZE = 200;

    private final CampaignService campaignService;

    @GetMapping
    public Page<ContactResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LeadBand band,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return campaignService.listContacts(q, band,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                        Sort.by(Sort.Direction.DESC, "leadScore")));
    }

    @GetMapping("/{id}")
    public ContactResponse get(@PathVariable String id) {
        return campaignService.getContact(id);
    }

    @PostMapping
    public ResponseEntity<ContactResponse> create(
            @Valid @RequestBody CreateContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.createContact(request));
    }

    @PatchMapping("/{id}/do-not-call")
    public ContactResponse doNotCall(@PathVariable String id,
                                     @RequestParam boolean value) {
        return campaignService.setDoNotCall(id, value);
    }
}

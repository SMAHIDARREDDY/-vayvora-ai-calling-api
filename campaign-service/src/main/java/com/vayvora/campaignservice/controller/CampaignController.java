package com.vayvora.campaignservice.controller;

import com.vayvora.campaignservice.dto.CreateCampaignRequest;
import com.vayvora.campaignservice.dto.CampaignResponse;
import com.vayvora.campaignservice.dto.CampaignMetricsResponse;
import com.vayvora.campaignservice.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CampaignController {
    private final CampaignService campaignService;

    @GetMapping
    public ResponseEntity<?> getCampaigns(@RequestParam(defaultValue = "50") int limit,
                                         @RequestParam(defaultValue = "0") int offset,
                                         @RequestParam(required = false) String status,
                                         @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            return ResponseEntity.ok(campaignService.getCampaigns(organizationId, limit, offset, status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch campaigns"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCampaign(@RequestBody CreateCampaignRequest request,
                                           @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            CampaignResponse response = campaignService.createCampaign(organizationId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to create campaign"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCampaign(@PathVariable Long id,
                                        @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            CampaignResponse response = campaignService.getCampaign(id, organizationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Campaign not found"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCampaign(@PathVariable Long id,
                                           @RequestBody CreateCampaignRequest request,
                                           @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            CampaignResponse response = campaignService.updateCampaign(id, organizationId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to update campaign"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCampaign(@PathVariable Long id,
                                           @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            campaignService.deleteCampaign(id, organizationId);
            return ResponseEntity.ok(new SuccessResponse("Campaign deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to delete campaign"));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startCampaign(@PathVariable Long id,
                                          @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            CampaignResponse response = campaignService.startCampaign(id, organizationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to start campaign"));
        }
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pauseCampaign(@PathVariable Long id,
                                          @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            CampaignResponse response = campaignService.pauseCampaign(id, organizationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to pause campaign"));
        }
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<?> getCampaignMetrics(@PathVariable Long id,
                                               @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            CampaignMetricsResponse metrics = campaignService.getCampaignMetrics(id, organizationId);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to fetch metrics"));
        }
    }

    @PostMapping("/{id}/contacts/upload")
    public ResponseEntity<?> uploadContacts(@PathVariable Long id,
                                           @RequestBody ContactUploadRequest request,
                                           @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            campaignService.uploadContacts(id, organizationId, request.getContacts());
            return ResponseEntity.ok(new SuccessResponse(request.getContacts().size() + " contacts uploaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to upload contacts"));
        }
    }
}

class ContactUploadRequest {
    private java.util.List<java.util.Map<String, Object>> contacts;
    public java.util.List<java.util.Map<String, Object>> getContacts() { return contacts; }
}

class ErrorResponse {
    private String error;
    public ErrorResponse(String error) { this.error = error; }
}

class SuccessResponse {
    private String message;
    public SuccessResponse(String message) { this.message = message; }
}

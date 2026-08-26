package com.vayvora.analyticsservice.controller;

import com.vayvora.analyticsservice.dto.*;
import com.vayvora.analyticsservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard/{organizationId}")
    public ResponseEntity<?> getDashboard(@PathVariable Long organizationId,
                                         @RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) {
        try {
            DashboardResponse response = analyticsService.getDashboard(organizationId, startDate, endDate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch dashboard"));
        }
    }

    @GetMapping("/calls/{organizationId}")
    public ResponseEntity<?> getCallAnalytics(@PathVariable Long organizationId,
                                             @RequestParam(required = false) Long agentId,
                                             @RequestParam(required = false) Long campaignId,
                                             @RequestParam(required = false) String sentiment) {
        try {
            return ResponseEntity.ok(analyticsService.getCallAnalytics(organizationId, agentId, campaignId, sentiment));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch call analytics"));
        }
    }

    @GetMapping("/sentiment/{organizationId}")
    public ResponseEntity<?> getSentimentAnalysis(@PathVariable Long organizationId) {
        try {
            return ResponseEntity.ok(analyticsService.getSentimentAnalysis(organizationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch sentiment analysis"));
        }
    }

    @GetMapping("/agent-performance/{organizationId}")
    public ResponseEntity<?> getAgentPerformance(@PathVariable Long organizationId) {
        try {
            return ResponseEntity.ok(analyticsService.getAgentPerformance(organizationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch agent performance"));
        }
    }

    @GetMapping("/campaign-performance/{campaignId}")
    public ResponseEntity<?> getCampaignPerformance(@PathVariable Long campaignId,
                                                   @RequestHeader("X-Organization-Id") Long organizationId) {
        try {
            return ResponseEntity.ok(analyticsService.getCampaignPerformance(campaignId, organizationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch campaign performance"));
        }
    }

    @GetMapping("/trends/{organizationId}")
    public ResponseEntity<?> getTrends(@PathVariable Long organizationId,
                                      @RequestParam(defaultValue = "30") int days) {
        try {
            return ResponseEntity.ok(analyticsService.getTrends(organizationId, days));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch trends"));
        }
    }

    @GetMapping("/export/{organizationId}")
    public ResponseEntity<?> exportData(@PathVariable Long organizationId,
                                       @RequestParam(defaultValue = "csv") String format) {
        try {
            return ResponseEntity.ok(analyticsService.exportData(organizationId, format));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to export data"));
        }
    }
}

class ErrorResponse {
    private String error;
    public ErrorResponse(String error) { this.error = error; }
}

package com.vayvora.analyticsservice.controller;

import com.vayvora.analyticsservice.dto.AnalyticsDtos.AgentPerformanceResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.BillingResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.DashboardResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.LeadBandResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.SentimentResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.TopicsResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.TrendResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.UsageResponse;
import com.vayvora.analyticsservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Analytics endpoints (spec §18, §30). */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final int MAX_WINDOW_DAYS = 365;

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam(defaultValue = "1") int days) {
        return analyticsService.dashboard(clampDays(days));
    }

    @GetMapping("/trend")
    public TrendResponse trend(@RequestParam(defaultValue = "14") int days) {
        return analyticsService.trend(clampDays(days));
    }

    @GetMapping("/sentiment")
    public SentimentResponse sentiment() {
        return analyticsService.sentiment();
    }

    @GetMapping("/topics")
    public TopicsResponse topics(@RequestParam(defaultValue = "6") int limit) {
        return analyticsService.topics(limit);
    }

    @GetMapping("/agents")
    public AgentPerformanceResponse agents(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.agentPerformance(clampDays(days));
    }

    @GetMapping("/lead-bands")
    public LeadBandResponse leadBands() {
        return analyticsService.leadBands();
    }

    private static int clampDays(int days) {
        return Math.min(Math.max(1, days), MAX_WINDOW_DAYS);
    }
}

/** Usage and billing endpoints, served by the analytics service (spec §30). */
@RestController
@RequiredArgsConstructor
class UsageBillingController {

    private final AnalyticsService analyticsService;

    @GetMapping("/usage")
    public UsageResponse usage() {
        return analyticsService.usage();
    }

    @GetMapping("/billing")
    public BillingResponse billing() {
        return analyticsService.billing();
    }
}

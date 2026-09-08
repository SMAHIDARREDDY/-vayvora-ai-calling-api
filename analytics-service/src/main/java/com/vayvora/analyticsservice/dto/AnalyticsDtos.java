package com.vayvora.analyticsservice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Response payloads for the analytics API. */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    /** Headline dashboard figures (spec §18). */
    public record DashboardResponse(
            long callsToday,
            long connectedToday,
            long aiConversations,
            long leadsCaptured,
            long hotLeads,
            String averageDuration,
            int averageDurationSeconds,
            double connectRate,
            double successRate,
            int windowDays) {
    }

    /** One day of call volume, split by direction. */
    public record VolumePoint(LocalDate date, long inbound, long outbound) {
    }

    public record TrendResponse(int windowDays, List<VolumePoint> points) {
    }

    public record SentimentResponse(
            long positive,
            long neutral,
            long negative,
            double positiveRatio,
            double neutralRatio,
            double negativeRatio) {
    }

    public record TopicShare(String topic, long count, double share) {
    }

    public record TopicsResponse(long totalClassified, List<TopicShare> topics) {
    }

    public record AgentPerformanceRow(
            String agentId,
            String agentName,
            long totalCalls,
            long connectedCalls,
            double connectRate,
            int averageDurationSeconds,
            String averageDuration) {
    }

    public record AgentPerformanceResponse(List<AgentPerformanceRow> agents) {
    }

    /** Contact counts by lead band (spec §16). */
    public record LeadBandResponse(Map<String, Long> counts, long total) {
    }

    /** Metered usage for the current billing period (spec §22). */
    public record UsageResponse(
            LocalDate periodStart,
            LocalDate periodEnd,
            double voiceMinutesUsed,
            Integer voiceMinutesIncluded,
            double storageGbUsed,
            Integer storageGbIncluded,
            long activePhoneNumbers,
            long publishedAgents,
            Integer maxAgents) {
    }

    public record BillingResponse(
            String planCode,
            String planName,
            Long monthlyPriceMinor,
            String currency,
            String subscriptionStatus,
            LocalDate currentPeriodStart,
            LocalDate currentPeriodEnd,
            long estimatedTotalMinor,
            List<LineItem> lineItems) {

        public record LineItem(String label, long amountMinor) {
        }
    }
}

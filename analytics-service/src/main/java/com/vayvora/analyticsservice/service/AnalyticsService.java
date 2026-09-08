package com.vayvora.analyticsservice.service;

import com.vayvora.analyticsservice.dto.AnalyticsDtos.AgentPerformanceResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.AgentPerformanceRow;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.BillingResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.DashboardResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.LeadBandResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.SentimentResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.TopicShare;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.TopicsResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.TrendResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.UsageResponse;
import com.vayvora.analyticsservice.dto.AnalyticsDtos.VolumePoint;
import com.vayvora.shared.entity.Agents;
import com.vayvora.shared.entity.Platform;
import com.vayvora.shared.enums.Enums.AgentStatus;
import com.vayvora.shared.enums.Enums.CallDirection;
import com.vayvora.shared.enums.Enums.LeadBand;
import com.vayvora.shared.enums.Enums.Sentiment;
import com.vayvora.shared.enums.Enums.UsageMetric;
import com.vayvora.shared.repository.Repositories.AiAgentRepository;
import com.vayvora.shared.repository.Repositories.CallIntentRepository;
import com.vayvora.shared.repository.Repositories.CallRepository;
import com.vayvora.shared.repository.Repositories.CallSentimentRepository;
import com.vayvora.shared.repository.Repositories.ContactRepository;
import com.vayvora.shared.repository.Repositories.PhoneNumberRepository;
import com.vayvora.shared.repository.Repositories.PlanRepository;
import com.vayvora.shared.repository.Repositories.SubscriptionRepository;
import com.vayvora.shared.repository.Repositories.UsageRecordRepository;
import com.vayvora.shared.tenant.TenantContext;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only aggregations over call, contact and usage data (spec §18).
 *
 * <p>Everything here is computed from the operational tables on request. That
 * is correct for the volumes a single organization produces; the Kafka
 * consumer in this service is where pre-aggregation would land if read latency
 * became a problem at platform scale (spec §28).
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final CallRepository calls;
    private final CallSentimentRepository sentiments;
    private final CallIntentRepository intents;
    private final ContactRepository contacts;
    private final AiAgentRepository agents;
    private final PhoneNumberRepository phoneNumbers;
    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final UsageRecordRepository usage;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(int windowDays) {
        String orgId = TenantContext.requireOrganizationId();
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        long total = calls.countByOrganizationIdAndStartedAtAfter(orgId, since);
        long connected = calls
                .countByOrganizationIdAndConnectedAtIsNotNullAndStartedAtAfter(orgId, since);
        int avgSeconds = (int) Math.round(calls.averageDurationSeconds(orgId, since));

        long hot = contacts.countByOrganizationIdAndLeadBand(orgId, LeadBand.HOT);
        long warm = contacts.countByOrganizationIdAndLeadBand(orgId, LeadBand.WARM);

        // A call handled entirely by the AI is one that connected and never
        // moved to a human agent; transfers are counted out of the AI total.
        long transferred = calls.findByOrganizationIdAndStateIn(orgId,
                List.of(com.vayvora.shared.enums.Enums.CallState.HUMAN_AGENT,
                        com.vayvora.shared.enums.Enums.CallState.TRANSFER_REQUESTED)).size();
        long aiHandled = Math.max(0, connected - transferred);

        return new DashboardResponse(
                total,
                connected,
                aiHandled,
                hot + warm,
                hot,
                formatDuration(avgSeconds),
                avgSeconds,
                total == 0 ? 0 : (double) connected / total,
                connected == 0 ? 0 : (double) aiHandled / connected,
                windowDays);
    }

    /**
     * Daily inbound and outbound volume.
     *
     * <p>Days with no calls are filled with zeros so a chart drawn from this
     * has an even x-axis rather than collapsing gaps.
     */
    @Transactional(readOnly = true)
    public TrendResponse trend(int windowDays) {
        String orgId = TenantContext.requireOrganizationId();
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        Map<LocalDate, long[]> byDay = new TreeMap<>();
        for (Object[] row : calls.dailyVolume(orgId, since)) {
            LocalDate day = toLocalDate(row[0]);
            CallDirection direction = (CallDirection) row[1];
            long count = ((Number) row[2]).longValue();

            long[] pair = byDay.computeIfAbsent(day, d -> new long[2]);
            if (direction == CallDirection.INBOUND) {
                pair[0] += count;
            } else {
                pair[1] += count;
            }
        }

        LocalDate start = LocalDate.now(ZoneOffset.UTC).minusDays(windowDays - 1L);
        List<VolumePoint> points = new ArrayList<>(windowDays);
        for (int i = 0; i < windowDays; i++) {
            LocalDate day = start.plusDays(i);
            long[] pair = byDay.getOrDefault(day, new long[2]);
            points.add(new VolumePoint(day, pair[0], pair[1]));
        }
        return new TrendResponse(windowDays, points);
    }

    @Transactional(readOnly = true)
    public SentimentResponse sentiment() {
        String orgId = TenantContext.requireOrganizationId();

        Map<Sentiment, Long> counts = new EnumMap<>(Sentiment.class);
        for (Sentiment s : Sentiment.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : sentiments.distribution(orgId)) {
            counts.put((Sentiment) row[0], ((Number) row[1]).longValue());
        }

        long positive = counts.get(Sentiment.POSITIVE);
        long neutral = counts.get(Sentiment.NEUTRAL);
        long negative = counts.get(Sentiment.NEGATIVE);
        long total = positive + neutral + negative;

        return new SentimentResponse(
                positive, neutral, negative,
                ratio(positive, total), ratio(neutral, total), ratio(negative, total));
    }

    @Transactional(readOnly = true)
    public TopicsResponse topics(int limit) {
        String orgId = TenantContext.requireOrganizationId();
        Pageable page = PageRequest.of(0, Math.min(Math.max(1, limit), 25));

        List<Object[]> rows = intents.topIntents(orgId, page);
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        List<TopicShare> topics = rows.stream()
                .map(r -> {
                    long count = ((Number) r[1]).longValue();
                    return new TopicShare((String) r[0], count, ratio(count, total));
                })
                .toList();

        return new TopicsResponse(total, topics);
    }

    @Transactional(readOnly = true)
    public AgentPerformanceResponse agentPerformance(int windowDays) {
        String orgId = TenantContext.requireOrganizationId();
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        List<Agents.AiAgent> published = agents
                .findByOrganizationIdAndStatus(orgId, AgentStatus.PUBLISHED, Pageable.unpaged())
                .getContent();

        List<AgentPerformanceRow> rows = new ArrayList<>(published.size());
        for (Agents.AiAgent agent : published) {
            var agentCalls = calls
                    .findByOrganizationIdAndAgentId(orgId, agent.getId(), Pageable.unpaged())
                    .getContent()
                    .stream()
                    .filter(c -> c.getStartedAt() != null && c.getStartedAt().isAfter(since))
                    .toList();

            long total = agentCalls.size();
            long connected = agentCalls.stream()
                    .filter(c -> c.getConnectedAt() != null)
                    .count();
            int avgSeconds = connected == 0 ? 0 : (int) (agentCalls.stream()
                    .filter(c -> c.getConnectedAt() != null)
                    .mapToInt(c -> c.getDurationSeconds() == null ? 0 : c.getDurationSeconds())
                    .sum() / connected);

            rows.add(new AgentPerformanceRow(
                    agent.getId(),
                    agent.getName(),
                    total,
                    connected,
                    total == 0 ? 0 : (double) connected / total,
                    avgSeconds,
                    formatDuration(avgSeconds)));
        }

        rows.sort((a, b) -> Long.compare(b.totalCalls(), a.totalCalls()));
        return new AgentPerformanceResponse(rows);
    }

    @Transactional(readOnly = true)
    public LeadBandResponse leadBands() {
        String orgId = TenantContext.requireOrganizationId();

        Map<String, Long> counts = new LinkedHashMap<>();
        long total = 0;
        for (LeadBand band : LeadBand.values()) {
            long count = contacts.countByOrganizationIdAndLeadBand(orgId, band);
            counts.put(band.name(), count);
            total += count;
        }
        return new LeadBandResponse(counts, total);
    }

    @Transactional(readOnly = true)
    public UsageResponse usage() {
        String orgId = TenantContext.requireOrganizationId();
        Platform.Subscription subscription = subscriptions.findByOrganizationId(orgId)
                .orElse(null);

        LocalDate periodStart = subscription != null
                ? subscription.getCurrentPeriodStart()
                : LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        LocalDate periodEnd = subscription != null
                ? subscription.getCurrentPeriodEnd()
                : periodStart.plusMonths(1).minusDays(1);

        Instant from = periodStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = periodEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Platform.Plan plan = subscription == null
                ? null
                : plans.findById(subscription.getPlanId()).orElse(null);

        return new UsageResponse(
                periodStart,
                periodEnd,
                usage.sumQuantity(orgId, UsageMetric.VOICE_MINUTES, from, to),
                plan == null ? null : plan.getIncludedMinutes(),
                usage.sumQuantity(orgId, UsageMetric.STORAGE_GB, from, to),
                plan == null ? null : plan.getIncludedStorageGb(),
                phoneNumbers.countByOrganizationIdAndActiveTrue(orgId),
                agents.countByOrganizationIdAndStatus(orgId, AgentStatus.PUBLISHED),
                plan == null ? null : plan.getMaxAgents());
    }

    /**
     * Estimated charges for the current period.
     *
     * <p>Overage is charged only on minutes beyond the plan's allowance; the
     * subscription fee itself is charged regardless.
     */
    @Transactional(readOnly = true)
    public BillingResponse billing() {
        String orgId = TenantContext.requireOrganizationId();
        Platform.Subscription subscription = subscriptions.findByOrganizationId(orgId)
                .orElse(null);

        if (subscription == null) {
            LocalDate start = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
            return new BillingResponse(null, "No active subscription", 0L, "INR",
                    "NONE", start, start.plusMonths(1).minusDays(1), 0L, List.of());
        }

        Platform.Plan plan = plans.findById(subscription.getPlanId()).orElse(null);
        LocalDate periodStart = subscription.getCurrentPeriodStart();
        LocalDate periodEnd = subscription.getCurrentPeriodEnd();

        Instant from = periodStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = periodEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long subscriptionFee = plan == null || plan.getMonthlyPriceMinor() == null
                ? 0L
                : plan.getMonthlyPriceMinor();

        double minutesUsed = usage.sumQuantity(orgId, UsageMetric.VOICE_MINUTES, from, to);
        int included = plan == null || plan.getIncludedMinutes() == null
                ? 0
                : plan.getIncludedMinutes();
        double overageMinutes = Math.max(0, minutesUsed - included);
        long overageRate = plan == null || plan.getOveragePerMinuteMinor() == null
                ? 0L
                : plan.getOveragePerMinuteMinor();
        long overageFee = Math.round(overageMinutes * overageRate);

        List<BillingResponse.LineItem> items = new ArrayList<>();
        items.add(new BillingResponse.LineItem(
                plan == null ? "Subscription" : plan.getName() + " subscription",
                subscriptionFee));
        items.add(new BillingResponse.LineItem(
                "Overage minutes (" + Math.round(overageMinutes) + ")", overageFee));

        return new BillingResponse(
                plan == null ? null : plan.getCode(),
                plan == null ? "Unknown plan" : plan.getName(),
                plan == null ? null : plan.getMonthlyPriceMinor(),
                plan == null ? "INR" : plan.getCurrency(),
                subscription.getStatus().name(),
                periodStart,
                periodEnd,
                subscriptionFee + overageFee,
                items);
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    private static double ratio(long part, long total) {
        return total == 0 ? 0 : (double) part / total;
    }

    private static String formatDuration(int seconds) {
        Duration d = Duration.ofSeconds(Math.max(0, seconds));
        return String.format("%02d:%02d", d.toMinutes(), d.toSecondsPart());
    }

    /** JPA returns the grouped date as java.sql.Date on some drivers. */
    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}

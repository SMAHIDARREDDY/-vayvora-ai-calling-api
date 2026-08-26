package com.vayvora.campaignservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishCampaignStarted(Long campaignId, Map<String, Object> campaignData) {
        try {
            String message = objectMapper.writeValueAsString(campaignData);
            kafkaTemplate.send("campaign-events", campaignId.toString(), message);
            log.info("Published CAMPAIGN_STARTED event for campaign: {}", campaignId);
        } catch (Exception e) {
            log.error("Failed to publish CAMPAIGN_STARTED event", e);
        }
    }

    public void publishCampaignPaused(Long campaignId, Map<String, Object> campaignData) {
        try {
            String message = objectMapper.writeValueAsString(campaignData);
            kafkaTemplate.send("campaign-events", campaignId.toString(), message);
            log.info("Published CAMPAIGN_PAUSED event for campaign: {}", campaignId);
        } catch (Exception e) {
            log.error("Failed to publish CAMPAIGN_PAUSED event", e);
        }
    }
}

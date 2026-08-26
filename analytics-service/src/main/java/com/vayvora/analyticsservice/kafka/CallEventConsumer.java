package com.vayvora.analyticsservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallEventConsumer {
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = "call-events", groupId = "analytics-service-group")
    public void onCallEvent(String message) {
        try {
            Map<String, Object> callData = objectMapper.readValue(message, Map.class);
            
            // Process event based on type
            String eventType = (String) callData.get("eventType");
            
            if ("CALL_INITIATED".equals(eventType)) {
                handleCallInitiated(callData);
            } else if ("CALL_STATUS_UPDATED".equals(eventType)) {
                handleCallStatusUpdated(callData);
            } else if ("RECORDING_UPLOADED".equals(eventType)) {
                handleRecordingUploaded(callData);
            }
            
            log.info("Processed call event: {}", eventType);
        } catch (Exception e) {
            log.error("Failed to process call event", e);
        }
    }

    private void handleCallInitiated(Map<String, Object> callData) {
        // 1. Update dashboard - increment total calls
        // 2. Cache call data for quick access
        // 3. Publish to WebSocket for real-time updates
    }

    private void handleCallStatusUpdated(Map<String, Object> callData) {
        // 1. Update call metrics
        // 2. If completed, calculate analytics
        // 3. Update completion rate
        // 4. Process sentiment data
    }

    private void handleRecordingUploaded(Map<String, Object> callData) {
        // 1. Update call with recording URL
        // 2. Extract transcript and sentiment
        // 3. Update lead score
        // 4. Refresh analytics dashboard
    }
}

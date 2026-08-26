package com.vayvora.callservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CallEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishCallInitiated(String callId, Map<String, Object> callData) {
        try {
            String message = objectMapper.writeValueAsString(callData);
            kafkaTemplate.send("call-events", callId, message);
            log.info("Published CALL_INITIATED event for call: {}", callId);
        } catch (Exception e) {
            log.error("Failed to publish CALL_INITIATED event", e);
        }
    }

    public void publishCallStatusUpdated(String callId, String status, Map<String, Object> callData) {
        try {
            String message = objectMapper.writeValueAsString(callData);
            kafkaTemplate.send("call-events", callId, message);
            log.info("Published CALL_STATUS_UPDATED event for call: {} with status: {}", callId, status);
        } catch (Exception e) {
            log.error("Failed to publish CALL_STATUS_UPDATED event", e);
        }
    }

    public void publishRecordingUploaded(String callId, Map<String, Object> recordingData) {
        try {
            String message = objectMapper.writeValueAsString(recordingData);
            kafkaTemplate.send("call-events", callId, message);
            log.info("Published RECORDING_UPLOADED event for call: {}", callId);
        } catch (Exception e) {
            log.error("Failed to publish RECORDING_UPLOADED event", e);
        }
    }
}

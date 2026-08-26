package com.vayvora.campaignservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignResponse {
    private Long id;
    private Long organizationId;
    private String name;
    private Long agentId;
    private String phoneNumber;
    private List<Map<String, Object>> contacts;
    private Map<String, Object> schedule;
    private Map<String, Object> callingLimits;
    private String description;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.vayvora.campaignservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {
    @NotNull(message = "Campaign name is required")
    private String name;
    
    @NotNull(message = "Agent ID is required")
    private Long agentId;
    
    private String phoneNumber;
    private List<Map<String, Object>> contacts;
    private Map<String, Object> schedule;
    private Map<String, Object> callingLimits;
    private String description;
}

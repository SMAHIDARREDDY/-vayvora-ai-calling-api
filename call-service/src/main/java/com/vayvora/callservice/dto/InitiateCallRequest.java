package com.vayvora.callservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateCallRequest {
    @NotNull(message = "Organization ID is required")
    private Long organizationId;
    
    @NotNull(message = "Agent ID is required")
    private Long agentId;
    
    @NotNull(message = "Contact phone number is required")
    private String contactPhoneNumber;
    
    private Long campaignId;
    private Map<String, Object> variables;
}

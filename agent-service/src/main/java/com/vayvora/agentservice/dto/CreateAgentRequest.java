package com.vayvora.agentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentRequest {
    @NotBlank(message = "Agent name is required")
    private String name;
    
    private String purpose;
    private List<String> languages;
    private String personality;
    private String voice;
    private Map<String, Object> knowledge;
    private Map<String, Object> tools;
}

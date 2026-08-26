package com.vayvora.agentservice.dto;

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
public class AgentResponse {
    private Long id;
    private Long organizationId;
    private String name;
    private String purpose;
    private List<String> languages;
    private String personality;
    private String voice;
    private Map<String, Object> knowledge;
    private Map<String, Object> tools;
    private Integer version;
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

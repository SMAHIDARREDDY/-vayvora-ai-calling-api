package com.vayvora.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentPerformanceResponse {
    private Long agentId;
    private String agentName;
    private Integer totalCalls;
    private Integer completedCalls;
    private Double completionRate;
    private Integer avgDuration;
    private Double avgSentiment;
}

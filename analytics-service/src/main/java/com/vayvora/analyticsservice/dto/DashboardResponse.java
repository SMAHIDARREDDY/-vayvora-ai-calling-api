package com.vayvora.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private Integer totalCalls;
    private Integer completedCalls;
    private Double completionRate;
    private Integer avgDuration;
    private Integer activeAgents;
    private Integer activeCampaigns;
}

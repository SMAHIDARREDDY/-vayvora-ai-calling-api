package com.vayvora.campaignservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignMetricsResponse {
    private Integer totalContacts;
    private Integer connectedCalls;
    private Integer noAnswerCalls;
    private Integer busyCalls;
    private Integer failedCalls;
    private Double avgDuration;
}

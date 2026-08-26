package com.vayvora.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendResponse {
    private String date;
    private Integer totalCalls;
    private Integer completedCalls;
    private Double completionRate;
}

package com.vayvora.knowledgeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultResponse {
    private Long id;
    private String title;
    private String content;
    private Double relevanceScore;
}

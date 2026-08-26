package com.vayvora.knowledgeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    private Long id;
    private Long knowledgeBaseId;
    private String title;
    private String content;
    private String source;
    private LocalDateTime createdAt;
}

package com.vayvora.knowledgeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeBaseRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
}

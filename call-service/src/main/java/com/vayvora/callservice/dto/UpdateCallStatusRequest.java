package com.vayvora.callservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCallStatusRequest {
    private String status;
    private String transcript;
    private String summary;
    private String sentiment;
    private Integer leadScore;
    private Integer duration;
}

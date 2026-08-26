package com.vayvora.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private int status;
    private LocalDateTime timestamp;

    public ErrorResponse(String error) {
        this.error = error;
        this.status = 400;
        this.timestamp = LocalDateTime.now();
    }
}

package com.vayvora.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String token;
    private String refreshToken;
    private String role;
    private Long organizationId;
    private Long expiresIn;
}

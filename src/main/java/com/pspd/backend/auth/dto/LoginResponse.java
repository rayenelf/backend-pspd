package com.pspd.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String id;
    private String email;
    private String role;
    private String statutCompte;
    private String token; // simple opaque token (UUID) for now
}

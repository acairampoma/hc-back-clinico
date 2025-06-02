package com.formacionbdi.microservicios.app.oauth.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO para la respuesta de autenticación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private Date expiration;
    private String tokenType;

    
    /**
     * Crea una nueva instancia de AuthResponse
     */
    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder()
                .tokenType("Bearer ");
    }
}

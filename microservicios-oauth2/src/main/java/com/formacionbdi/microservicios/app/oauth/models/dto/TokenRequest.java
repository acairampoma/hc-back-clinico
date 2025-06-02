package com.formacionbdi.microservicios.app.oauth.models.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para los parámetros del request de Password Grant
 * Incluye validaciones y normalización
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    @NotBlank(message = "grant_type es requerido")
    private String grantType;

    @NotBlank(message = "username es requerido")
    private String username;

    @NotBlank(message = "password es requerido")
    private String password;

    private String scope;

    // Para refresh token
    private String refreshToken;

    // Información del cliente (extraída del Authorization header)
    private String clientId;
    private String clientSecret;

    /**
     * Verifica si es un password grant
     */
    public boolean isPasswordGrant() {
        return "password".equals(grantType);
    }

    /**
     * Verifica si es un refresh token grant
     */
    public boolean isRefreshTokenGrant() {
        return "refresh_token".equals(grantType);
    }

    /**
     * Normaliza el username (lowercase, trim)
     */
    public String getNormalizedUsername() {
        return username != null ? username.trim().toLowerCase() : null;
    }

    /**
     * Parsea los scopes solicitados
     */
    public String[] getScopes() {
        if (scope == null || scope.trim().isEmpty()) {
            return new String[0];
        }
        return scope.trim().split("\\s+");
    }
}
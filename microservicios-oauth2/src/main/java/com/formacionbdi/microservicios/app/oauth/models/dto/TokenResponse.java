package com.formacionbdi.microservicios.app.oauth.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de tokens OAuth2
 * Cumple con el estándar RFC 6749
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("scope")
    private String scope;

    // Campos adicionales para compatibilidad con diferentes clientes
    private String jti; // JWT ID

    // Getters adicionales para compatibilidad (algunos clientes esperan camelCase)
    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public Integer getExpiresIn() { return expiresIn; }
    public String getRefreshToken() { return refreshToken; }
    public String getScope() { return scope; }
}
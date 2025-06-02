package com.formacionbdi.microservicios.app.oauth.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO para respuestas de error OAuth2
 * Cumple con el estándar RFC 6749 Section 5.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Código de error estándar OAuth2
     * - invalid_request: Request malformado
     * - invalid_client: Autenticación de cliente falló
     * - invalid_grant: Grant inválido o expirado
     * - unauthorized_client: Cliente no autorizado para este grant
     * - unsupported_grant_type: Grant type no soportado
     * - invalid_scope: Scope inválido
     * - server_error: Error interno del servidor
     */
    @JsonProperty("error")
    private String error;

    /**
     * Descripción legible del error
     */
    @JsonProperty("error_description")
    private String errorDescription;

    /**
     * URI con información adicional del error (opcional)
     */
    @JsonProperty("error_uri")
    private String errorUri;

    /**
     * Timestamp del error
     */
    private String timestamp;

    /**
     * Información adicional para debugging (solo en desarrollo)
     */
    private String debugInfo;

    /**
     * Factory methods para errores comunes
     */
    public static ErrorResponse invalidRequest(String description) {
        return ErrorResponse.builder()
                .error("invalid_request")
                .errorDescription(description)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static ErrorResponse invalidClient(String description) {
        return ErrorResponse.builder()
                .error("invalid_client")
                .errorDescription(description)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static ErrorResponse invalidGrant(String description) {
        return ErrorResponse.builder()
                .error("invalid_grant")
                .errorDescription(description)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static ErrorResponse unauthorizedClient(String description) {
        return ErrorResponse.builder()
                .error("unauthorized_client")
                .errorDescription(description)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static ErrorResponse unsupportedGrantType(String description) {
        return ErrorResponse.builder()
                .error("unsupported_grant_type")
                .errorDescription(description)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static ErrorResponse invalidScope(String description) {
        return ErrorResponse.builder()
                .error("invalid_scope")
                .errorDescription(description)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static ErrorResponse serverError(String description) {
        return ErrorResponse.builder()
                .error("server_error")
                .errorDescription(description)
                .timestamp(Instant.now().toString())
                .build();
    }
}
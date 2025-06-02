package com.formacionbdi.microservicios.app.usuarios.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidad que representa los detalles de un cliente OAuth2 en el sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "oauth_client_details")
public class OAuthClientDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "client_id", nullable = false, length = 256)
    private String clientId;

    @Column(name = "resource_ids", length = 256)
    private String resourceIds;

    @Column(name = "client_secret", nullable = false, length = 256)
    private String clientSecret;

    @Column(name = "scope", nullable = false, length = 256)
    private String scope;

    @Column(name = "authorized_grant_types", nullable = false, length = 256)
    private String authorizedGrantTypes;

    @Column(name = "web_server_redirect_uri", length = 256)
    private String webServerRedirectUri;

    @Column(name = "authorities", length = 256)
    private String authorities;

    @Column(name = "access_token_validity")
    private Integer accessTokenValidity;

    @Column(name = "refresh_token_validity")
    private Integer refreshTokenValidity;

    @Lob
    @Column(name = "additional_information", columnDefinition = "TEXT")
    private String additionalInformation;

    @Column(name = "autoapprove", length = 256)
    private String autoApprove;

    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Column(name = "active", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Métodos de ayuda

    /**
     * Verifica si el cliente está activo.
     * @return true si el cliente está activo, false en caso contrario
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    /**
     * Activa el cliente.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Desactiva el cliente.
     */
    public void deactivate() {
        this.active = false;
    }
}
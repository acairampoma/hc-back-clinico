package com.formacionbdi.microservicios.app.usuarios.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidad que representa la relación muchos a muchos entre usuarios y roles.
 * Incluye información adicional como quién asignó el rol y cuándo se asignó.
 * 
 * Utiliza una clave primaria compuesta representada por la clase {@link UserRoleId}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
public class UserRole implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Role role;

    @Column(name = "assigned_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", referencedColumnName = "id", nullable = false)
    private User assignedBy;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.assignedAt == null) {
            this.assignedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Crea una nueva instancia de UserRole con los IDs proporcionados.
     * @param userId ID del usuario
     * @param roleId ID del rol
     * @param assignedBy Usuario que asigna el rol
     * @return una nueva instancia de UserRole
     */
    public static UserRole create(Long userId, Long roleId, User assignedBy) {
        return UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .assignedBy(assignedBy)
                .active(true)
                .build();
    }

    /**
     * Verifica si el rol del usuario está activo y no ha expirado.
     * @return true si el rol está activo y no ha expirado, false en caso contrario
     */
    public boolean isActive() {
        if (Boolean.FALSE.equals(active)) {
            return false;
        }
        return expiresAt == null || LocalDateTime.now().isBefore(expiresAt);
    }
}

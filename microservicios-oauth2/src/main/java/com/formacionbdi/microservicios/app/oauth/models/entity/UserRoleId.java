package com.formacionbdi.microservicios.app.oauth.models.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clase que representa la clave primaria compuesta para la entidad UserRole.
 * Se utiliza para mapear la clave primaria compuesta en la base de datos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleId implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private Long roleId;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        UserRoleId that = (UserRoleId) o;
        
        return Objects.equals(userId, that.userId) && 
               Objects.equals(roleId, that.roleId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
    
    @Override
    public String toString() {
        return "UserRoleId{" +
               "userId=" + userId +
               ", roleId=" + roleId +
               '}';
    }
}

package com.formacionbdi.microservicios.app.oauth.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // IMPORTANTE: Ignorar campos desconocidos
public class UserResponse {
    private Long id;
    private String username;
    private String password;  // PRESENTE - viene hasheado de BD
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;

    // CORREGIDO: Cambiar a Set<String> para coincidir con la respuesta real
    private Set<String> roles;  // "ROLE_ADMIN", "ROLE_USER", etc.

    // MÉTODO HELPER: Para convertir roles a GrantedAuthority
    public List<String> getRolesList() {
        return roles != null ? List.copyOf(roles) : List.of();
    }
}
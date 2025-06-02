package com.formacionbdi.microservicios.app.oauth.clients;

import com.formacionbdi.microservicios.app.oauth.models.dto.ClientResponse;
import com.formacionbdi.microservicios.app.oauth.models.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign para consumir el microservicio de usuarios
 * Se conecta via Eureka al microservicio-usuarios
 */
@FeignClient(
        name = "MICROSERVICIO-USUARIOS", // Nombre EN MAYÚSCULAS como aparece en Eureka
        path = "" // Sin path base
)
public interface UsuariosClient {

    /**
     * Buscar usuario por username - ajusta según tu endpoint real
     */
    @GetMapping("/users/username/{username}")
    UserResponse findUserByUsername(@PathVariable("username") String username);

    /**
     * Buscar usuario por ID - ajusta según tu endpoint real
     */
    @GetMapping("/users/{id}")
    UserResponse findUserById(@PathVariable("id") Long id);

    /**
     * Buscar cliente OAuth por clientId
     * GET /clients/{clientId} (según tu URL que funciona)
     */
    @GetMapping("/clients/{clientId}")
    ClientResponse findClientByClientId(@PathVariable("clientId") String clientId);

    /**
     * Buscar usuario por username CON password (para autenticación OAuth2)
     * OPCIÓN A: Endpoint específico
     */
    @GetMapping("/users/username/{username}/with-password")
    UserResponse findUserByUsernameWithPassword(@PathVariable("username") String username);
}
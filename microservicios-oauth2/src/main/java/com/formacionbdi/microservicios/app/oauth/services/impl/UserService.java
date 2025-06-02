package com.formacionbdi.microservicios.app.oauth.services.impl;

import com.formacionbdi.microservicios.app.oauth.clients.UsuariosClient;
import com.formacionbdi.microservicios.app.oauth.models.dto.UserResponse;
import com.formacionbdi.microservicios.app.oauth.services.IUserService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService, IUserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UsuariosClient usuariosClient;

    @Autowired
    public UserService(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.info("🔍 Buscando usuario para autenticación: {}", username);

            // SIMPLIFICADO: Usar método existente que ya incluye password
            UserResponse user = usuariosClient.findUserByUsername(username);

            if (user == null || !user.getEnabled()) {
                String errorMsg = String.format("❌ Error en el login, no existe el usuario '%s' en el sistema o está deshabilitado", username);
                log.error(errorMsg);
                throw new UsernameNotFoundException(errorMsg);
            }

            // Verificar que el password no sea null
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                String errorMsg = String.format("❌ Password no encontrado para el usuario '%s'", username);
                log.error(errorMsg);
                throw new UsernameNotFoundException(errorMsg);
            }

            // Mapear roles a authorities - CORREGIDO para Set<String>
            List<GrantedAuthority> authorities = user.getRoles().stream()
                    .filter(Objects::nonNull)
                    .map(roleName -> new SimpleGrantedAuthority(roleName)) // Directamente string
                    .peek(authority -> log.debug("Role asignado: {}", authority.getAuthority()))
                    .collect(Collectors.toList());

            log.info("✅ Usuario encontrado: {} con {} roles y password presente", username, authorities.size());

            // Crear UserDetails - el password ya viene hasheado de BD
            return new User(
                    user.getUsername(),
                    user.getPassword(), // Ahora debería venir con el password hasheado
                    user.getEnabled(),
                    true, // account non expired
                    true, // credentials non expired
                    true, // account non locked
                    authorities
            );

        } catch (FeignException.NotFound e) {
            log.error("❌ Usuario no encontrado via Feign: {}", username);
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        } catch (FeignException e) {
            log.error("❌ Error en Feign al buscar usuario {}: {} - {}", username, e.status(), e.getMessage());
            throw new UsernameNotFoundException("Error en el servicio de autenticación", e);
        } catch (Exception e) {
            log.error("❌ Error inesperado en loadUserByUsername: {}", e.getMessage(), e);
            throw new UsernameNotFoundException("Error en el servicio de autenticación", e);
        }
    }

    @Override
    public UserResponse findByUsername(String username) {
        try {
            log.info("🔍 Buscando usuario por username: {}", username);
            UserResponse user = usuariosClient.findUserByUsername(username);
            if (user != null) {
                log.info("✅ Usuario encontrado: {}", username);
            }
            return user;
        } catch (FeignException.NotFound e) {
            log.warn("⚠️ Usuario no encontrado: {}", username);
            return null;
        } catch (FeignException e) {
            log.error("❌ Error en Feign al buscar usuario por username {}: {}", username, e.getMessage());
            return null;
        }
    }

    @Override
    public UserResponse findById(Long id) {
        try {
            log.info("🔍 Buscando usuario por ID: {}", id);
            UserResponse user = usuariosClient.findUserById(id);
            if (user != null) {
                log.info("✅ Usuario encontrado por ID: {}", id);
            }
            return user;
        } catch (FeignException.NotFound e) {
            log.warn("⚠️ Usuario no encontrado con ID: {}", id);
            return null;
        } catch (FeignException e) {
            log.error("❌ Error en Feign al buscar usuario por ID {}: {}", id, e.getMessage());
            return null;
        }
    }
}
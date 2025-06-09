package com.formacionbdi.microservicios.app.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityConfig.class);

    @Value("${OAUTH2_JWK_URI:https://brilliant-bravery-production.up.railway.app/auth/.well-known/jwks.json}")
    private String jwkSetUri;
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Configuring Security Filter Chain WITH OPTIONS support");

        return http
                // ❌ DESHABILITAR CSRF
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // 🛡️ CONFIGURAR AUTORIZACIÓN
                .authorizeExchange(exchanges -> exchanges
                        // ✅ RUTAS PÚBLICAS
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/oauth2/**").permitAll()
                        .pathMatchers("/.well-known/**").permitAll()

                        // 🔧 PERMITIR TODAS LAS REQUESTS OPTIONS (PREFLIGHT)
                        .pathMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔐 RUTAS PROTEGIDAS
                        .pathMatchers("/api/usuarios/**").authenticated()
                        .pathMatchers("/api/listas/**").authenticated()
                        .pathMatchers("/api/test/**").authenticated()
                        .pathMatchers("/api/notas/**").authenticated()
                        .pathMatchers("/api/recetas/**").authenticated()
                        .pathMatchers("/api/ordenes/**").authenticated()
                        .pathMatchers("/api/pacientes/**").authenticated()
                        .pathMatchers("/api/catalogos/**").authenticated()

                        // 🔒 Todo lo demás requiere autenticación
                        .anyExchange().authenticated()
                )

                // 🎫 CONFIGURAR OAUTH2 RESOURCE SERVER
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                            jwt.jwkSetUri(jwkSetUri);
                            log.debug("JWT configured with JWK Set URI: {}", jwkSetUri);
                        })
                )
                .build();
    }
}
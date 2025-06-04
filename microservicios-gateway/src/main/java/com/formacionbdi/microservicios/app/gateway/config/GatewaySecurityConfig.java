package com.formacionbdi.microservicios.app.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class GatewaySecurityConfig {

    @Value("${oauth2.jwk-set-uri:http://localhost:8082/auth/.well-known/jwks.json}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // ✅ PÚBLICAS - No requieren token
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/oauth2/**").permitAll()
                        .pathMatchers("/.well-known/**").permitAll()

                        // 🔐 PROTEGIDAS - Requieren JWT válido
                        .pathMatchers("/api/usuarios/**").authenticated()
                        .pathMatchers("/api/listas/**").authenticated()
                        .pathMatchers("/api/test/**").authenticated()

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwkSetUri(jwkSetUri))
                )
                .build();
    }
}

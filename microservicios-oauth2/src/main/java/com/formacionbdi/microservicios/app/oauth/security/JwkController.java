package com.formacionbdi.microservicios.app.oauth.security;


import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class JwkController {

    private final JWKSource<SecurityContext> jwkSource;

    public JwkController(JWKSource<SecurityContext> jwkSource) {
        this.jwkSource = jwkSource;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwkSet() {
        try {
            log.info("🔑 JWK endpoint llamado");

            // USAR EL MISMO FIX QUE EN createJwtToken()
            JWKSelector selector = new JWKSelector(new JWKMatcher.Builder().build());
            List<JWK> keys = jwkSource.get(selector, null);

            if (keys.isEmpty()) {
                log.error("❌ No hay claves RSA disponibles");
                return ResponseEntity.status(500).build();
            }

            JWKSet jwkSet = new JWKSet(keys);
            return ResponseEntity.ok(jwkSet.toJSONObject());

        } catch (Exception e) {
            log.error("❌ Error generando JWK set: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}
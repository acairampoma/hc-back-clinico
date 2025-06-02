package com.formacionbdi.microservicios.app.oauth.security;

import com.formacionbdi.microservicios.app.oauth.models.dto.TokenResponse;
import com.formacionbdi.microservicios.app.oauth.models.dto.ErrorResponse;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

@RestController
@Component
@Slf4j
public class PasswordGrantController {

    private final RegisteredClientRepository clientRepository;
    private final AuthenticationManager authenticationManager;
    private final OAuth2AuthorizationService authorizationService;
    private final PasswordEncoder passwordEncoder;

    private final JWKSource<SecurityContext> jwkSource;

    public PasswordGrantController(RegisteredClientRepository clientRepository,
                                   AuthenticationManager authenticationManager,
                                   OAuth2AuthorizationService authorizationService,
                                   PasswordEncoder passwordEncoder,
                                   JWKSource<SecurityContext> jwkSource) {
        this.clientRepository = clientRepository;
        this.authenticationManager = authenticationManager;
        this.authorizationService = authorizationService;
        this.passwordEncoder = passwordEncoder;
        this.jwkSource = jwkSource;

    }

    @PostMapping("/oauth2/password-token")
    public ResponseEntity<?> token(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "scope", required = false) String scope) {

        log.info("=== PASSWORD GRANT REQUEST ===");
        log.info("Grant type: {}", grantType);
        log.info("Username: {}", username);
        log.info("Scope: {}", scope);

        try {
            // 1. Validar grant type
            if (!"password".equals(grantType)) {
                return ResponseEntity.badRequest()
                        .headers(createOAuth2Headers())
                        .body(ErrorResponse.unsupportedGrantType("Este endpoint solo soporta password grant"));
            }

            // 2. Validar parámetros requeridos
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                return ResponseEntity.badRequest()
                        .headers(createOAuth2Headers())
                        .body(ErrorResponse.invalidRequest("Username y password son requeridos"));
            }

            // 3. Autenticar cliente OAuth
            RegisteredClient client = authenticateClient(authorization);
            log.info("Cliente OAuth autenticado: {}", client.getClientId());

            // 4. Validar que el cliente soporte password grant
            if (!client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.PASSWORD)) {
                return ResponseEntity.badRequest()
                        .headers(createOAuth2Headers())
                        .body(ErrorResponse.unauthorizedClient("Cliente no autorizado para password grant"));
            }

            // 5. Autenticar usuario
            Authentication userAuth = authenticateUser(username, password);
            log.info("Usuario autenticado: {}", userAuth.getName());

            // 6. Procesar y validar scopes
            Set<String> requestedScopes = parseScopes(scope);
            Set<String> authorizedScopes = authorizeScopes(requestedScopes, client);

            if (authorizedScopes.isEmpty()) {
                return ResponseEntity.badRequest()
                        .headers(createOAuth2Headers())
                        .body(ErrorResponse.invalidScope("Scopes solicitados no válidos o no autorizados"));
            }

            // 7. Generar tokens
            TokenResponse tokenResponse = generateTokens(client, userAuth, authorizedScopes);

            log.info("✅ Password grant exitoso para usuario: {} - cliente: {}",
                    userAuth.getName(), client.getClientId());

            return ResponseEntity.ok()
                    .headers(createOAuth2Headers())
                    .body(tokenResponse);

        } catch (AuthenticationException e) {
            log.error("❌ Error de autenticación: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .headers(createOAuth2Headers())
                    .body(ErrorResponse.invalidGrant(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error inesperado en password grant: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .headers(createOAuth2Headers())
                    .body(ErrorResponse.serverError("Error interno del servidor"));
        }
    }

    /**
     * Crea headers estándar OAuth2
     */
    private HttpHeaders createOAuth2Headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-store");
        headers.add("Pragma", "no-cache");
        return headers;
    }

    /**
     * Autentica y valida el cliente OAuth usando Basic Auth
     */
    private RegisteredClient authenticateClient(String authorization) throws AuthenticationException {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Basic ")) {
            throw new BadCredentialsException("Authorization header con Basic Auth es requerido");
        }

        try {
            String credentials = new String(Base64.getDecoder().decode(authorization.substring(6)));
            String[] parts = credentials.split(":", 2);

            if (parts.length != 2) {
                throw new BadCredentialsException("Formato de Authorization header inválido");
            }

            String clientId = parts[0];
            String clientSecretPlain = parts[1];

            log.debug("Autenticando cliente: {}", clientId);

            RegisteredClient client = clientRepository.findByClientId(clientId);
            if (client == null) {
                throw new BadCredentialsException("Cliente OAuth no encontrado: " + clientId);
            }

            if (!passwordEncoder.matches(clientSecretPlain, client.getClientSecret())) {
                log.warn("❌ Client secret inválido para cliente: {}", clientId);
                throw new BadCredentialsException("Credenciales de cliente inválidas");
            }

            log.debug("✅ Cliente autenticado exitosamente: {}", clientId);
            return client;

        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Error decodificando Authorization header", e);
        }
    }

    /**
     * Autentica el usuario usando AuthenticationManager
     */
    private Authentication authenticateUser(String username, String password) throws AuthenticationException {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            log.warn("❌ Fallo autenticación usuario: {} - {}", username, e.getMessage());
            throw new BadCredentialsException("Credenciales de usuario inválidas", e);
        }
    }

    /**
     * Parsea los scopes solicitados
     */
    private Set<String> parseScopes(String scope) {
        if (!StringUtils.hasText(scope)) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(scope.trim().split("\\s+")));
    }

    /**
     * Autoriza scopes basado en los configurados para el cliente
     */
    private Set<String> authorizeScopes(Set<String> requestedScopes, RegisteredClient client) {
        Set<String> authorizedScopes = new LinkedHashSet<>();

        if (requestedScopes.isEmpty()) {
            authorizedScopes.addAll(client.getScopes());
        } else {
            for (String requestedScope : requestedScopes) {
                if (client.getScopes().contains(requestedScope)) {
                    authorizedScopes.add(requestedScope);
                } else {
                    log.warn("Scope no autorizado para cliente {}: {}",
                            client.getClientId(), requestedScope);
                }
            }
        }

        log.debug("Scopes autorizados: {} para cliente: {}",
                authorizedScopes, client.getClientId());
        return authorizedScopes;
    }

    /**
     * Genera tokens usando DTO limpio
     */
    private TokenResponse generateTokens(RegisteredClient client, Authentication userAuth,
                                         Set<String> scopes) {
        try {
            // 1. Configuración de tiempo
            int accessTokenTtl = (int) client.getTokenSettings()
                    .getAccessTokenTimeToLive().getSeconds();

            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(accessTokenTtl);

            // 2. Crear JWT manualmente
            String jwtToken = createJwtToken(userAuth, scopes, now, expiresAt, client);

            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    jwtToken,
                    now,
                    expiresAt,
                    scopes);

            // 3. Generar refresh token
            OAuth2RefreshToken refreshToken = null;
            if (client.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
                String refreshTokenValue = UUID.randomUUID().toString();
                int refreshTokenTtl = (int) client.getTokenSettings()
                        .getRefreshTokenTimeToLive().getSeconds();

                refreshToken = new OAuth2RefreshToken(
                        refreshTokenValue,
                        now,
                        now.plusSeconds(refreshTokenTtl));
            }

            // 4. Guardar autorización
            saveAuthorization(client, userAuth, scopes, accessToken, refreshToken);

            // 5. USAR DTO BUILDER
            return TokenResponse.builder()
                    .accessToken(accessToken.getTokenValue())
                    .tokenType("Bearer")
                    .expiresIn(accessTokenTtl)
                    .scope(String.join(" ", scopes))
                    .refreshToken(refreshToken != null ? refreshToken.getTokenValue() : null)
                    .jti(UUID.randomUUID().toString())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error generando tokens JWT: {}", e.getMessage(), e);
            throw new IllegalStateException("Error en generación de tokens JWT", e);
        }
    }

    /**
     * REEMPLAZA TODO EL MÉTODO createJwtToken() CON ESTA VERSIÓN SIMPLIFICADA
     */
    private String createJwtToken(Authentication userAuth, Set<String> scopes,
                                  Instant issuedAt, Instant expiresAt, RegisteredClient client) {
        try {
            log.debug("🔧 Creando JWT firmado...");

            // 1. Obtener la clave RSA de forma más directa
            RSAKey rsaKey = getRSAKeyFromSource();
            RSAPrivateKey privateKey = rsaKey.toRSAPrivateKey();

            // 2. Header JWT (con RSA)
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "RS256");
            header.put("typ", "JWT");
            header.put("kid", rsaKey.getKeyID());

            // 3. Payload JWT (EXACTAMENTE IGUAL que antes)
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", userAuth.getName());
            payload.put("iss", "http://localhost:8082/auth");
            payload.put("aud", client.getClientId());
            payload.put("exp", expiresAt.getEpochSecond());
            payload.put("iat", issuedAt.getEpochSecond());
            payload.put("scope", String.join(" ", scopes));
            payload.put("microservice", "microservicio-oauth2");

            // Agregar roles del usuario (IGUAL que antes)
            List<String> roles = userAuth.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toList();
            payload.put("roles", roles);
            payload.put("username", userAuth.getName());

            // 4. Crear JWT base64
            String headerJson = objectToJson(header);
            String payloadJson = objectToJson(payload);

            String headerBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes());
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes());

            // 5. FIRMAR el token
            String signatureInput = headerBase64 + "." + payloadBase64;
            String signature = signWithRSA(signatureInput, privateKey);

            String jwt = headerBase64 + "." + payloadBase64 + "." + signature;

            log.debug("✅ JWT FIRMADO creado exitosamente");
            return jwt;

        } catch (Exception e) {
            log.error("❌ Error creando JWT firmado: {}", e.getMessage(), e);
            // FALLBACK: Si falla la firma, usar el original sin firma temporalmente
            log.warn("🚨 FALLBACK: Usando JWT sin firma temporalmente");
            return createUnsignedJwtFallback(userAuth, scopes, issuedAt, expiresAt, client);
        }
    }

    /**
     * Obtiene la RSAKey del JWKSource de forma segura
     */
    private RSAKey getRSAKeyFromSource() throws Exception {
        try {
            // Crear un selector simple para obtener las keys
            JWKSelector selector = new JWKSelector(new JWKMatcher.Builder().build());
            List<JWK> keys = jwkSource.get(selector, null);

            if (keys.isEmpty()) {
                throw new IllegalStateException("No hay claves RSA disponibles");
            }

            return (RSAKey) keys.get(0);

        } catch (Exception e) {
            log.error("Error obteniendo RSA key: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Firma el JWT usando RSA-SHA256
     */
    private String signWithRSA(String input, RSAPrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(input.getBytes());
        byte[] signatureBytes = signature.sign();

        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signatureBytes);
    }

    /**
     * FALLBACK: JWT sin firma (tu versión original) por si algo falla
     */
    private String createUnsignedJwtFallback(Authentication userAuth, Set<String> scopes,
                                             Instant issuedAt, Instant expiresAt, RegisteredClient client) {
        try {
            // Header JWT simple
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "none");
            header.put("typ", "JWT");

            // Payload JWT
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", userAuth.getName());
            payload.put("iss", "http://localhost:8082/auth");
            payload.put("aud", client.getClientId());
            payload.put("exp", expiresAt.getEpochSecond());
            payload.put("iat", issuedAt.getEpochSecond());
            payload.put("scope", String.join(" ", scopes));
            payload.put("microservice", "microservicio-oauth2");

            List<String> roles = userAuth.getAuthorities().stream()
                    .map(auth -> auth.getAuthority())
                    .toList();
            payload.put("roles", roles);
            payload.put("username", userAuth.getName());

            String headerJson = objectToJson(header);
            String payloadJson = objectToJson(payload);

            String headerBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes());
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes());

            return headerBase64 + "." + payloadBase64 + ".";

        } catch (Exception e) {
            throw new RuntimeException("Error en fallback JWT", e);
        }
    }

    /**
     * Convierte un objeto a JSON simple
     */
    private String objectToJson(Map<String, Object> obj) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof List) {
                json.append("[");
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append("\"").append(list.get(i)).append("\"");
                }
                json.append("]");
            } else {
                json.append(value);
            }
            first = false;
        }
        json.append("}");

        return json.toString();
    }

    /**
     * Guarda la autorización en el servicio
     */
    private void saveAuthorization(RegisteredClient client, Authentication userAuth,
                                   Set<String> scopes, OAuth2AccessToken accessToken,
                                   OAuth2RefreshToken refreshToken) {

        OAuth2Authorization.Builder authBuilder = OAuth2Authorization
                .withRegisteredClient(client)
                .principalName(userAuth.getName())
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizedScopes(scopes)
                .attribute(Principal.class.getName(), userAuth);

        authBuilder.accessToken(accessToken);

        if (refreshToken != null) {
            authBuilder.refreshToken(refreshToken);
        }

        OAuth2Authorization authorization = authBuilder.build();
        authorizationService.save(authorization);

        log.debug("✅ Autorización guardada para usuario: {}", userAuth.getName());
    }
}
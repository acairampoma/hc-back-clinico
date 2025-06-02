package com.formacionbdi.microservicios.app.oauth.security;

import com.formacionbdi.microservicios.app.oauth.models.dto.ClientResponse;
import com.formacionbdi.microservicios.app.oauth.services.OAuthClientService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final OAuthClientService oauthClientService;
    private final UserDetailsService userDetailsService;

    // Constructor injection (mejor práctica que @Autowired)
    public SecurityConfig(PasswordEncoder passwordEncoder,
                          OAuthClientService oauthClientService,
                          UserDetailsService userDetailsService) {
        this.passwordEncoder = passwordEncoder;
        this.oauthClientService = oauthClientService;
        this.userDetailsService = userDetailsService;
    }


    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/**", "/login", "/error").permitAll()
                        .requestMatchers("/oauth2/password-token").permitAll()  // IMPORTANTE: Permitir acceso público
                        .requestMatchers("/oauth2/**").permitAll()  // AGREGADO: Permitir todos los endpoints OAuth2
                        .requestMatchers("/.well-known/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository() {
        return new DatabaseRegisteredClientRepository();
    }

    // ========== PASSWORD GRANT CONTROLLER ==========
    // REMOVIDO: Ya no necesario porque el controller tiene @Component
    // El PasswordGrantController se auto-registra por component scan

    // ========== AUTHENTICATION CONFIGURATION ==========

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        log.info("AuthenticationManager configurado con UserDetailsService: {}",
                userDetailsService.getClass().getSimpleName());
        return new ProviderManager(provider);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        log.info("Configurando InMemoryOAuth2AuthorizationService");
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator() {
        log.info("Configurando OAuth2TokenGenerator con JWT customizer");

        JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource());
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        jwtGenerator.setJwtCustomizer(tokenCustomizer());

        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }

    // ========== JWT CONFIGURATION ==========

    @Bean
    JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Error generando RSA key pair", ex);
        }
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8082/auth")
                .build();
    }

    // AGREGADO: Bean para AuthorizationServerContext
    @Bean
    @Primary
    public AuthorizationServerContext authorizationServerContext() {
        AuthorizationServerSettings settings = authorizationServerSettings();
        return new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return settings.getIssuer();
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return settings;
            }
        };
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                Authentication principal = context.getPrincipal();
                context.getClaims()
                        .claim("microservice", "microservicio-oauth2")
                        .claim("roles", principal.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toList()))
                        .claim("username", principal.getName());
            }
        };
    }

    // ========== DATABASE CLIENT REPOSITORY ==========

    /**
     * Implementación del RegisteredClientRepository que lee desde base de datos
     * via Feign Client al microservicio-usuarios
     */
    private class DatabaseRegisteredClientRepository implements RegisteredClientRepository {

        @Override
        public void save(RegisteredClient registeredClient) {
            throw new UnsupportedOperationException("Guardado de clientes no implementado");
        }

        @Override
        public RegisteredClient findById(String id) {
            log.debug("findById llamado con id: {} (no implementado)", id);
            return null;
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            log.info("Buscando cliente OAuth en BD via Feign: {}", clientId);

            return oauthClientService.findByClientId(clientId)
                    .map(this::mapToRegisteredClient)
                    .orElseGet(() -> {
                        log.warn("Cliente OAuth no encontrado: {}", clientId);
                        return null;
                    });
        }

        private RegisteredClient mapToRegisteredClient(ClientResponse client) {
            log.info("Mapeando cliente OAuth: {} con grant types: {}",
                    client.getClientId(), client.getGrantTypes());

            RegisteredClient.Builder builder = RegisteredClient
                    .withId(UUID.randomUUID().toString())
                    .clientId(client.getClientId())
                    .clientSecret(client.getClientSecret()) // Ya viene hasheado de BD
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

            // Configurar scopes
            configureScopes(builder, client);

            // Configurar grant types
            configureGrantTypes(builder, client);

            // Configurar settings
            configureTokenSettings(builder, client);
            configureClientSettings(builder);
            configureRedirectUris(builder);

            RegisteredClient registeredClient = builder.build();
            log.info("Cliente OAuth mapeado exitosamente: {}", client.getClientId());

            return registeredClient;
        }

        private void configureScopes(RegisteredClient.Builder builder, ClientResponse client) {
            if (client.getScopes() != null && !client.getScopes().isEmpty()) {
                client.getScopes().forEach(scope -> {
                    String cleanScope = scope.trim();
                    log.debug("Agregando scope: {}", cleanScope);
                    builder.scope(cleanScope);
                });
            }
        }

        private void configureGrantTypes(RegisteredClient.Builder builder, ClientResponse client) {
            if (client.getGrantTypes() != null && !client.getGrantTypes().isEmpty()) {
                client.getGrantTypes().forEach(grantType -> {
                    String cleanGrantType = grantType.trim();
                    log.debug("Agregando grant type: {}", cleanGrantType);

                    switch (cleanGrantType) {
                        case "authorization_code" ->
                                builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                        case "refresh_token" ->
                                builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                        case "client_credentials" ->
                                builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                        case "password" -> {
                            log.info("✅ Habilitando Password Grant para cliente: {}", client.getClientId());
                            builder.authorizationGrantType(AuthorizationGrantType.PASSWORD);
                        }
                        default -> {
                            log.warn("Grant type no reconocido: {}", cleanGrantType);
                            builder.authorizationGrantType(new AuthorizationGrantType(cleanGrantType));
                        }
                    }
                });
            }
        }

        private void configureTokenSettings(RegisteredClient.Builder builder, ClientResponse client) {
            Integer accessTokenValidity = client.getAccessTokenValidity() != null ?
                    client.getAccessTokenValidity() : 3600;
            Integer refreshTokenValidity = client.getRefreshTokenValidity() != null ?
                    client.getRefreshTokenValidity() : 86400;

            builder.tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofSeconds(accessTokenValidity))
                    .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenValidity))
                    .build());

            log.debug("Token settings: access={}s, refresh={}s", accessTokenValidity, refreshTokenValidity);
        }

        private void configureClientSettings(RegisteredClient.Builder builder) {
            builder.clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .build());
        }

        private void configureRedirectUris(RegisteredClient.Builder builder) {
            // TODO: Mover a configuración o BD - ajusta según tu gateway
            builder.redirectUri("http://127.0.0.1:8090/login/oauth2/code/gateway");
        }
    }
}
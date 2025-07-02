package com.formacionbdi.microservicios.app.usuarios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.Scopes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger para el microservicio de usuarios
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // Crear objeto Scopes
        Scopes scopes = new Scopes()
            .addString("read", "Acceso de lectura")
            .addString("write", "Acceso de escritura");
        
        return new OpenAPI()
                .info(new Info()
                        .title("API Microservicio Usuarios")
                        .description("Documentación de la API del microservicio de gestión de usuarios y clientes OAuth")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Alan Cairampoma")
                                .email("acairampoma@example.com")
                                .url("https://github.com/acairampoma"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server().url("http://localhost:8091").description("Microservicio Usuarios - Local"))
                .addServersItem(new Server().url("http://localhost:8090/api/usuarios").description("API Gateway - Local"))
                // Agregar configuración de seguridad OAuth2
                .components(new Components()
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("Autenticación OAuth2 con JWT")
                                .flows(new OAuthFlows()
                                        .password(new OAuthFlow()
                                                .tokenUrl("http://localhost:8090/oauth2/password-token")
                                                .refreshUrl("http://localhost:8090/oauth2/token")
                                                .scopes(scopes)
                                        )
                                )
                        ))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"));
    }
}

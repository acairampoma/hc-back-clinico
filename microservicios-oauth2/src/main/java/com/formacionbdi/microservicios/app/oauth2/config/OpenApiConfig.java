package com.formacionbdi.microservicios.app.oauth2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger para el microservicio OAuth2
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Microservicio OAuth2")
                        .description("Documentación de la API del microservicio de autenticación y autorización OAuth2")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Alan Cairampoma")
                                .email("acairampoma@example.com")
                                .url("https://github.com/acairampoma"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server().url("http://localhost:8082").description("Microservicio OAuth2 - Local"))
                .addServersItem(new Server().url("http://localhost:8090/oauth2").description("API Gateway - OAuth2"));
    }
}

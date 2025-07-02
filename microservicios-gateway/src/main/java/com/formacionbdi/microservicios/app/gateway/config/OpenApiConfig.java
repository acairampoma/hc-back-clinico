package com.formacionbdi.microservicios.app.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuración centralizada de OpenAPI/Swagger para el API Gateway
 * Permite documentar todos los microservicios desde un único punto
 */
@Configuration
public class OpenApiConfig {

    @Bean
    @Primary
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Centralizada - Sistema Hospitalario")
                        .description("Documentación centralizada de todos los microservicios del sistema hospitalario")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Alan Cairampoma")
                                .email("acairampoma@example.com")
                                .url("https://github.com/acairampoma"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server().url("http://localhost:8090").description("API Gateway - Local"));
    }
}

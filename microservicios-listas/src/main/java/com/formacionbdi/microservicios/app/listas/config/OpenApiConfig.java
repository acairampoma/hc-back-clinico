package com.formacionbdi.microservicios.app.listas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger para el microservicio de listas
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Microservicio Listas")
                        .description("Documentación de la API del microservicio de gestión de listas y pacientes")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Alan Cairampoma")
                                .email("acairampoma@example.com")
                                .url("https://github.com/acairampoma"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server().url("http://localhost:8001").description("Microservicio Listas - Local"))
                .addServersItem(new Server().url("http://localhost:8090/api/listas").description("API Gateway - Listas"))
                .addServersItem(new Server().url("http://localhost:8090/api/pacientes").description("API Gateway - Pacientes"));
    }
}

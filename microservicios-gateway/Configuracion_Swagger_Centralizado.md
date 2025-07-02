# Configuración de Swagger Centralizado para Microservicios

Este documento describe cómo configurar un Swagger centralizado a través del API Gateway para documentar todos los microservicios del proyecto.

## Configuración del API Gateway

Ya hemos configurado el API Gateway para soportar un Swagger centralizado mediante:

1. Adición de la dependencia `springdoc-openapi-starter-webflux-ui`
2. Configuración de la clase `OpenApiConfig` para agrupar los microservicios
3. Actualización del archivo `application.yml` con la configuración de SpringDoc
4. Modificación de la configuración de seguridad para permitir acceso público a los endpoints de Swagger

## Configuración de cada Microservicio

Para que cada microservicio exponga su documentación OpenAPI y se integre con el Swagger centralizado, sigue estos pasos para cada uno:

### 1. Agregar dependencias de SpringDoc OpenAPI

Añade estas dependencias al `pom.xml` de cada microservicio:

```xml
<!-- SpringDoc OpenAPI para documentación Swagger -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

### 2. Configurar OpenAPI en cada microservicio

Crea una clase de configuración en cada microservicio:

```java
package com.formacionbdi.microservicios.app.[nombre-microservicio].config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Microservicio [Nombre]")
                        .description("Documentación de la API del microservicio [Nombre]")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Alan Cairampoma")
                                .email("acairampoma@example.com")
                                .url("https://github.com/acairampoma"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
```

### 3. Configurar application.properties/yml de cada microservicio

Añade estas propiedades al archivo `application.properties` o `application.yml` de cada microservicio:

```yaml
# Configuración SpringDoc OpenAPI
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
  packages-to-scan: com.formacionbdi.microservicios.app.[nombre-microservicio].controllers
```

### 4. Anotar los controladores (opcional pero recomendado)

Para mejorar la documentación, puedes anotar tus controladores y métodos:

```java
@RestController
@RequestMapping("/endpoint")
@Tag(name = "Nombre del Controlador", description = "Descripción de las operaciones")
public class MiController {

    @Operation(
        summary = "Resumen de la operación", 
        description = "Descripción detallada de la operación",
        responses = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<MiDTO> obtenerPorId(@PathVariable Long id) {
        // Implementación
    }
}
```

## Acceso al Swagger Centralizado

Una vez configurado todo, podrás acceder al Swagger centralizado a través de:

- **URL**: http://localhost:8090/swagger-ui.html

Desde esta interfaz podrás:
1. Ver todos los microservicios disponibles
2. Cambiar entre diferentes microservicios usando el selector desplegable
3. Explorar todos los endpoints disponibles
4. Probar los endpoints directamente desde la interfaz

## Consideraciones Importantes

1. **Fechas**: Recuerda que hemos cambiado los campos de tipo `LocalDateTime` a `String` en algunos DTOs para evitar problemas de deserialización. Asegúrate de documentar correctamente el formato esperado en la documentación OpenAPI.

2. **Seguridad**: Los endpoints de Swagger están configurados como públicos en el API Gateway, pero los endpoints de la API siguen protegidos. Para probar endpoints protegidos desde Swagger, necesitarás obtener un token OAuth2 y configurarlo en la interfaz de Swagger.

3. **Generación de Clientes**: Puedes usar la documentación OpenAPI para generar clientes en diferentes lenguajes de programación.

## Solución de Problemas

Si encuentras problemas con la integración de Swagger:

1. Verifica que la ruta configurada en el API Gateway coincida con la ruta expuesta por el microservicio
2. Asegúrate de que los microservicios estén registrados correctamente en Eureka
3. Comprueba que la configuración CORS permita el acceso desde la interfaz de Swagger
4. Revisa los logs del API Gateway para identificar posibles errores de enrutamiento

# Microservicio de Autenticación OAuth2

Este es un microservicio de autenticación basado en Spring Security OAuth2 que proporciona autenticación y autorización para aplicaciones distribuidas.

## Características

- Autenticación con OAuth 2.0 y OpenID Connect
- Soporte para JWT (JSON Web Tokens)
- Integración con Eureka para el registro de servicios
- Base de datos PostgreSQL para el almacenamiento de usuarios, roles y clientes OAuth2
- Migraciones de base de datos con Flyway
- Configuración centralizada con Spring Cloud Config
- Documentación de la API con SpringDoc OpenAPI

## Requisitos previos

- Java 11 o superior
- Maven 3.6 o superior
- PostgreSQL 13 o superior
- Eureka Server (para el registro de servicios)
- Config Server (opcional, para configuración centralizada)

## Configuración

### Base de datos

1. Crear una base de datos PostgreSQL llamada `oauth2_db`
2. Configurar las credenciales en `application.yml` si es necesario

### Variables de entorno

Puedes configurar las siguientes variables de entorno:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/oauth2_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET=tu_clave_secreta_aqui
```

### Clientes OAuth2

Se crea automáticamente un cliente OAuth2 con las siguientes credenciales:

- Client ID: `gateway-client`
- Client Secret: `secret123`
- Redirect URI: `http://127.0.0.1:3000/authorized`
- Scopes: `openid`, `profile`, `email`, `read`, `write`

## Iniciar la aplicación

1. Asegúrate de que PostgreSQL esté en ejecución
2. Ejecuta el siguiente comando:

```bash
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8082/auth`

## Endpoints principales

- **OAuth2 Authorization Server**: `http://localhost:8082/auth/oauth2/authorize`
- **OAuth2 Token**: `http://localhost:8082/auth/oauth2/token`
- **JWK Set**: `http://localhost:8082/auth/oauth2/jwks`
- **User Info**: `http://localhost:8082/auth/userinfo`
- **OpenID Configuration**: `http://localhost:8082/auth/.well-known/openid-configuration`

## Documentación de la API

La documentación de la API está disponible en:

- **Swagger UI**: `http://localhost:8082/auth/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8082/auth/v3/api-docs`

## Estructura del proyecto

```
src/main/java/
└── com
    └── formacionbdi
        └── microservicios
            └── app
                └── oauth
                    ├── config          # Configuraciones de Spring
                    ├── controllers     # Controladores REST
                    ├── dto             # Objetos de transferencia de datos
                    ├── exception       # Manejo de excepciones
                    ├── models          # Entidades del dominio
                    ├── repositories    # Repositorios de datos
                    ├── security        # Configuración de seguridad
                    └── services        # Lógica de negocio
```

## Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más información.

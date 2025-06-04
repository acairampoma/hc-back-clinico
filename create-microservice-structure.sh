#!/bin/bash

# Script dinámico para crear estructura de microservicios
# Uso: ./create-microservice-structure.sh [nombre-microservicio]
# Ejemplo: ./create-microservice-structure.sh listas

# Validar que se proporcione el nombre del microservicio
if [ -z "$1" ]; then
    echo "❌ Error: Debe proporcionar el nombre del microservicio"
    echo "Uso: $0 [nombre-microservicio]"
    echo "Ejemplos:"
    echo "  $0 listas"
    echo "  $0 catalogos" 
    echo "  $0 recetas"
    echo "  $0 notas-vitales"
    exit 1
fi

MICROSERVICE_NAME=$1
PROJECT_NAME="microservicios-${MICROSERVICE_NAME}"
BASE_PACKAGE="com.formacionbdi.microservicios.app.${MICROSERVICE_NAME}"

echo "🚀 Creando estructura para microservicio: ${MICROSERVICE_NAME}"
echo "📁 Nombre del proyecto: ${PROJECT_NAME}"
echo "📦 Paquete base: ${BASE_PACKAGE}"

# Crear estructura de directorios
echo "📂 Creando estructura de carpetas..."

# Directorio principal del proyecto
mkdir -p ${PROJECT_NAME}
cd ${PROJECT_NAME}

# Estructura Maven
mkdir -p src/main/java
mkdir -p src/main/resources
mkdir -p src/test/java

# Convertir paquete a estructura de carpetas
PACKAGE_PATH=$(echo ${BASE_PACKAGE} | tr '.' '/')
mkdir -p src/main/java/${PACKAGE_PATH}

# Crear subdirectorios del microservicio
mkdir -p src/main/java/${PACKAGE_PATH}/controllers
mkdir -p src/main/java/${PACKAGE_PATH}/models/dto
mkdir -p src/main/java/${PACKAGE_PATH}/models/entity
mkdir -p src/main/java/${PACKAGE_PATH}/models/response
mkdir -p src/main/java/${PACKAGE_PATH}/repository
mkdir -p src/main/java/${PACKAGE_PATH}/services
mkdir -p src/main/java/${PACKAGE_PATH}/services/impl
mkdir -p src/main/java/${PACKAGE_PATH}/exception

# Crear archivos base
echo "📄 Creando archivos de configuración..."

# Crear application.yml
cat > src/main/resources/application.yml << EOF
spring:
  application:
    name: microservicio-${MICROSERVICE_NAME}
  datasource:
    url: jdbc:postgresql://localhost:5432/bd_hdigital
    username: postgres
    password: 123456
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQL10Dialect
    generate-ddl: true
    properties:
      hibernate:
        jdbc:
          lob:
            non_contextual_creation: true
  cloud:
    loadbalancer:
      ribbon:
        enabled: false

server:
  port: 8003

eureka:
  instance:
    instance-id: \${spring.application.name}:\${random.value}
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka

logging:
  level:
    org:
      hibernate:
        SQL: debug
EOF

# Crear archivo principal de aplicación
MAIN_CLASS="${MICROSERVICE_NAME^}Application"
cat > src/main/java/${PACKAGE_PATH}/${MAIN_CLASS}.java << EOF
package ${BASE_PACKAGE};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableEurekaClient
public class ${MAIN_CLASS} {

    public static void main(String[] args) {
        SpringApplication.run(${MAIN_CLASS}.class, args);
    }

}
EOF

# Crear pom.xml
cat > pom.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.2.0.RELEASE</version>
        <relativePath/>
    </parent>
    <groupId>com.formacionbdi.microservicios.app.${MICROSERVICE_NAME}</groupId>
    <artifactId>microservicios-${MICROSERVICE_NAME}</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>microservicios-${MICROSERVICE_NAME}</name>
    <description>Microservicio de ${MICROSERVICE_NAME} para Spring Boot</description>

    <properties>
        <java.version>11</java.version>
        <lombok.version>1.18.20</lombok.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>\${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>Hoxton.RC1</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

</project>
EOF

echo "✅ Estructura creada exitosamente!"
echo ""
echo "📋 Estructura generada:"
echo "└── ${PROJECT_NAME}/"
echo "    ├── pom.xml"
echo "    └── src/"
echo "        ├── main/"
echo "        │   ├── java/"
echo "        │   │   └── ${PACKAGE_PATH}/"
echo "        │   │       ├── ${MAIN_CLASS}.java"
echo "        │   │       ├── controllers/"
echo "        │   │       ├── models/"
echo "        │   │       │   ├── dto/"
echo "        │   │       │   ├── entity/"
echo "        │   │       │   └── response/"
echo "        │   │       ├── repository/"
echo "        │   │       ├── services/"
echo "        │   │       │   └── impl/"
echo "        │   │       └── exception/"
echo "        │   └── resources/"
echo "        │       └── application.yml"
echo "        └── test/"
echo "            └── java/"
echo ""
echo "🎯 Próximos pasos:"
echo "1. cd ${PROJECT_NAME}"
echo "2. Pegar los modelos, DTOs, repositories, services y controllers"
echo "3. mvn spring-boot:run"
echo ""
echo "🔧 Puerto asignado: 8003 (puedes cambiarlo en application.yml)"
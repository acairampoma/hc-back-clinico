#!/bin/bash

# Script para crear microservicio HL7 - ALAN CAIRAMPOMA
# Uso: ./create-hl7-microservice.sh
# Basado en tu estructura exitosa de microservicios

MICROSERVICE_NAME="hl7"
PROJECT_NAME="microservicios-${MICROSERVICE_NAME}"
BASE_PACKAGE="com.formacionbdi.microservicios.app.${MICROSERVICE_NAME}"

echo "🔥 Creando microservicio HL7 para Sistema Hospitalario"
echo "📁 Nombre del proyecto: ${PROJECT_NAME}"
echo "📦 Paquete base: ${BASE_PACKAGE}"
echo "📡 Puerto HL7: 2575 (TCP MLLP)"
echo "🌐 Puerto REST: 8004"

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

# Crear subdirectorios específicos para HL7
mkdir -p src/main/java/${PACKAGE_PATH}/controllers
mkdir -p src/main/java/${PACKAGE_PATH}/models/dto
mkdir -p src/main/java/${PACKAGE_PATH}/models/entity
mkdir -p src/main/java/${PACKAGE_PATH}/models/response
mkdir -p src/main/java/${PACKAGE_PATH}/repository
mkdir -p src/main/java/${PACKAGE_PATH}/services
mkdir -p src/main/java/${PACKAGE_PATH}/services/impl
mkdir -p src/main/java/${PACKAGE_PATH}/exception

# Estructura específica HL7
mkdir -p src/main/java/${PACKAGE_PATH}/server
mkdir -p src/main/java/${PACKAGE_PATH}/parser
mkdir -p src/main/java/${PACKAGE_PATH}/config
mkdir -p src/main/java/${PACKAGE_PATH}/protocol
mkdir -p src/main/java/${PACKAGE_PATH}/validator

# Crear archivos de configuración
echo "📄 Creando archivos de configuración..."

# Crear application.yml específico para HL7
cat > src/main/resources/application.yml << EOF
spring:
  application:
    name: microservicio-hl7
  datasource:
    url: jdbc:postgresql://localhost:5432/bd_hdigital
    username: postgres
    password: 123456
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQL10Dialect
    generate-ddl: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        jdbc:
          lob:
            non_contextual_creation: true
        format_sql: true
    show-sql: false
  cloud:
    loadbalancer:
      ribbon:
        enabled: false

server:
  port: 8004

# Configuración específica HL7
hl7:
  server:
    port: 2575
    host: 0.0.0.0
    max-connections: 100
    timeout: 30000
    auto-ack: true
  protocol:
    version: "2.5"
    charset: "UTF-8"
    start-char: "\\x0B"
    end-char: "\\x1C\\x0D"
  processing:
    thread-pool-size: 50
    retry-attempts: 3
    dead-letter-queue: true

eureka:
  instance:
    instance-id: \${spring.application.name}:\${random.value}
    metadata-map:
      hl7-port: \${hl7.server.port}
      service-type: "hl7-processor"
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka

logging:
  level:
    com.formacionbdi.microservicios.app.hl7: DEBUG
    org.hibernate.SQL: WARN
    org.springframework.data.jpa: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%logger{36}] - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%logger{36}] - %msg%n"
  file:
    name: logs/hl7-microservice.log

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
EOF

# Crear archivo principal de aplicación
MAIN_CLASS="Hl7Application"
cat > src/main/java/${PACKAGE_PATH}/${MAIN_CLASS}.java << EOF
package ${BASE_PACKAGE};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Microservicio HL7 - Sistema Hospitalario
 * Procesamiento de mensajes HL7 v2.5 con protocolo MLLP
 * 
 * Funcionalidades:
 * - Servidor HL7 en puerto 2575
 * - Procesamiento de mensajes ADT, ORU, ORM, SIU
 * - Integración con base de datos PostgreSQL
 * - API REST para consultas en puerto 8004
 * 
 * @author Alan Cairampoma
 */
@SpringBootApplication
@EnableFeignClients
@EnableEurekaClient
@EnableAsync
@EnableScheduling
public class ${MAIN_CLASS} {

    public static void main(String[] args) {
        System.out.println("🔥 Iniciando Microservicio HL7...");
        System.out.println("📡 Puerto HL7 MLLP: 2575");
        System.out.println("🌐 Puerto REST API: 8004");
        System.out.println("💾 Base de datos: bd_hdigital");
        
        SpringApplication.run(${MAIN_CLASS}.class, args);
        
        System.out.println("✅ Microservicio HL7 iniciado exitosamente!");
    }
}
EOF

# Crear pom.xml con dependencias HL7
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
    <groupId>com.formacionbdi.microservicios.app.hl7</groupId>
    <artifactId>microservicios-hl7</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>microservicios-hl7</name>
    <description>Microservicio HL7 para Sistema Hospitalario - Spring Boot</description>

    <properties>
        <java.version>11</java.version>
        <lombok.version>1.18.20</lombok.version>
        <hapi.version>2.3</hapi.version>
        <spring-cloud.version>Hoxton.RC1</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Core -->
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
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- HL7 Processing Libraries -->
        <dependency>
            <groupId>ca.uhn.hapi</groupId>
            <artifactId>hapi-base</artifactId>
            <version>\${hapi.version}</version>
        </dependency>
        
        <dependency>
            <groupId>ca.uhn.hapi</groupId>
            <artifactId>hapi-structures-v25</artifactId>
            <version>\${hapi.version}</version>
        </dependency>
        
        <!-- Netty para servidor TCP -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>4.1.50.Final</version>
        </dependency>
        
        <!-- Spring Cloud -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>\${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        
        <!-- Testing -->
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
        
        <!-- Development Tools -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>\${spring-cloud.version}</version>
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

# Crear README.md
cat > README.md << EOF
# 📡 Microservicio HL7 - Sistema Hospitalario

Microservicio especializado en el procesamiento de mensajes HL7 v2.5 con protocolo MLLP.

## 🎯 Funcionalidades

- **Servidor HL7**: Puerto 2575 (TCP MLLP)
- **API REST**: Puerto 8004 para consultas
- **Mensajes soportados**: ADT, ORU, ORM, SIU, MDM
- **Base de datos**: PostgreSQL (bd_hdigital)
- **Integración**: Eureka Discovery

## 🚀 Ejecución

\`\`\`bash
# Compilar
mvn clean compile

# Ejecutar
mvn spring-boot:run

# O con JAR
mvn clean package
java -jar target/microservicios-hl7-0.0.1-SNAPSHOT.jar
\`\`\`

## 📡 Endpoints

### HL7 MLLP
- **Puerto**: 2575
- **Protocolo**: TCP con MLLP
- **Formato**: HL7 v2.5

### REST API
- **Base URL**: http://localhost:8004
- **Health Check**: GET /actuator/health
- **Mensajes**: GET /api/hl7/messages
- **Estadísticas**: GET /api/hl7/stats

## 🔧 Configuración

Ver \`application.yml\` para configuración de:
- Puerto HL7 (2575)
- Base de datos PostgreSQL
- Timeouts y pools de conexión
- Logging y métricas

## 🏥 Integración con Equipos Médicos

El microservicio acepta mensajes HL7 de:
- Monitores de signos vitales
- Sistemas de laboratorio
- Equipos de radiología
- Sistemas HIS externos

## 📊 Monitoreo

- **Logs**: \`logs/hl7-microservice.log\`
- **Métricas**: \`/actuator/metrics\`
- **Health**: \`/actuator/health\`
EOF

echo "✅ Microservicio HL7 creado exitosamente!"
echo ""
echo "📋 Estructura generada:"
echo "└── ${PROJECT_NAME}/"
echo "    ├── pom.xml (con dependencias HL7 y Netty)"
echo "    ├── README.md"
echo "    └── src/"
echo "        ├── main/"
echo "        │   ├── java/"
echo "        │   │   └── ${PACKAGE_PATH}/"
echo "        │   │       ├── ${MAIN_CLASS}.java"
echo "        │   │       ├── controllers/ (API REST)"
echo "        │   │       ├── server/ (TCP HL7 Server)"
echo "        │   │       ├── parser/ (HL7 Parser)"
echo "        │   │       ├── protocol/ (MLLP Protocol)"
echo "        │   │       ├── config/ (Configuration)"
echo "        │   │       ├── models/ (DTOs y Entities)"
echo "        │   │       ├── repository/ (JPA Repositories)"
echo "        │   │       ├── services/ (Business Logic)"
echo "        │   │       └── validator/ (HL7 Validation)"
echo "        │   └── resources/"
echo "        │       └── application.yml (Config HL7)"
echo "        └── test/"
echo "            └── java/"
echo ""
echo "🎯 Próximos pasos:"
echo "1. cd ${PROJECT_NAME}"
echo "2. Implementar las clases HL7 (Server, Parser, etc.)"
echo "3. mvn spring-boot:run"
echo ""
echo "🔧 Puertos configurados:"
echo "   - HL7 MLLP: 2575 (TCP)"
echo "   - REST API: 8004 (HTTP)"
echo ""
echo "🏥 Eureka Discovery: http://localhost:8761"
echo "📊 Health Check: http://localhost:8004/actuator/health"
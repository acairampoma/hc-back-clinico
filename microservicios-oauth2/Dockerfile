# 🔐 DOCKERFILE OAUTH2 - JDK 17 RAILWAY COMPATIBLE
# Ubicación: /microservicios-oauth2/Dockerfile

# ============================================
# STAGE 1: BUILD con JDK 17
# ============================================
FROM eclipse-temurin:17-jdk-alpine AS builder

# Instalar Maven
RUN apk add --no-cache maven

# Crear directorio de trabajo
WORKDIR /app

# Copiar pom.xml (desde la misma carpeta)
COPY pom.xml .

# Descargar dependencias (layer cacheado)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar microservicio OAuth2 con JDK 17
RUN mvn clean package -DskipTests -Dspring.profiles.active=railway

# ============================================
# STAGE 2: RUNTIME con JRE 17
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Metadatos
LABEL maintainer="Alan Cairampoma <cumpa@hospital.pe>"
LABEL description="OAuth2 Authorization Server - Sistema Hospitalario"

# Crear usuario no-root
RUN addgroup -g 1001 -S oauth2 && \
    adduser -S oauth2 -u 1001 -G oauth2

# Directorio de trabajo
WORKDIR /app

# Copiar JAR compilado desde stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Cambiar ownership
RUN chown oauth2:oauth2 app.jar

# Usuario no-root
USER oauth2

# Puerto OAuth2
EXPOSE 8082

# Variables de entorno
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=railway \
    SERVER_PORT=8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/auth/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
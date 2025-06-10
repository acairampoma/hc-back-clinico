# 📝 DOCKERFILE NOTAS - JAVA 11 RAILWAY COMPATIBLE
# Ubicación: /microservicios-notas/Dockerfile

# ============================================
# STAGE 1: BUILD
# ============================================
FROM eclipse-temurin:11-jdk-alpine AS builder

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

# Compilar microservicio notas
RUN mvn clean package -DskipTests -Dspring.profiles.active=railway

# ============================================
# STAGE 2: RUNTIME
# ============================================
FROM eclipse-temurin:11-jre-alpine

# Metadatos
LABEL maintainer="Alan Cairampoma <cumpa@hospital.pe>"
LABEL description="Microservicio Notas Médicas - Sistema Hospitalario"

# Crear usuario no-root
RUN addgroup -g 1001 -S notas && \
    adduser -S notas -u 1001 -G notas

# Directorio de trabajo
WORKDIR /app

# Copiar JAR compilado desde stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Cambiar ownership
RUN chown notas:notas app.jar

# Usuario no-root
USER notas

# Puerto Notas
EXPOSE 8004

# Variables de entorno
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=railway \
    SERVER_PORT=8004

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8004/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
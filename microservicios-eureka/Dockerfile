# 🔍 DOCKERFILE EUREKA SERVER - RAILWAY COMPATIBLE
# Ubicación: /microservicios-eureka/Dockerfile

# ============================================
# STAGE 1: BUILD (Compilación en Railway)
# ============================================
FROM eclipse-temurin:11-jdk-alpine AS builder

# Instalar Maven
RUN apk add --no-cache maven

# Crear directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración Maven primero (para cache layers)
COPY pom.xml .

# Descargar dependencias (layer cacheado)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests -Dspring.profiles.active=railway

# ============================================
# STAGE 2: RUNTIME (Ejecución optimizada)
# ============================================
FROM eclipse-temurin:11-jre-alpine

# Metadatos
LABEL maintainer="Alan Cairampoma <cumpa@hospital.pe>"
LABEL description="Eureka Server - Sistema Hospitalario"

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S eureka && \
    adduser -S eureka -u 1001 -G eureka

# Directorio de trabajo
WORKDIR /app

# Copiar JAR compilado desde stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Cambiar ownership del archivo
RUN chown eureka:eureka app.jar

# Cambiar a usuario no-root
USER eureka

# Puerto que expondrá Eureka
EXPOSE 8761

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=railway \
    SERVER_PORT=8761

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


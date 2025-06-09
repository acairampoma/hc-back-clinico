# 🔧 DOCKERFILE EN ROOT DEL PROYECTO - RAILWAY COMPATIBLE
# Ubicación: /Dockerfile (en la raíz de hc-back-clinico)

# ============================================
# STAGE 1: BUILD Eureka en Monorepo
# ============================================
FROM eclipse-temurin:11-jdk-alpine AS builder

# Instalar Maven
RUN apk add --no-cache maven

# Crear directorio de trabajo
WORKDIR /app

# Copiar pom.xml del microservicio Eureka
COPY microservicios-eureka/pom.xml ./pom.xml

# Descargar dependencias (layer cacheado)
RUN mvn dependency:go-offline -B

# Copiar código fuente de Eureka
COPY microservicios-eureka/src ./src

# Compilar específicamente Eureka
RUN mvn clean package -DskipTests -Dspring.profiles.active=railway

# ============================================
# STAGE 2: RUNTIME
# ============================================
FROM eclipse-temurin:11-jre-alpine

# Metadatos
LABEL maintainer="Alan Cairampoma <cumpa@hospital.pe>"
LABEL description="Eureka Server - Sistema Hospitalario"

# Crear usuario no-root
RUN addgroup -g 1001 -S eureka && \
    adduser -S eureka -u 1001 -G eureka

# Directorio de trabajo
WORKDIR /app

# Copiar JAR compilado desde stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Cambiar ownership
RUN chown eureka:eureka app.jar

# Usuario no-root
USER eureka

# Puerto
EXPOSE 8761

# Variables de entorno
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=railway \
    SERVER_PORT=8761

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ============================================
# INSTRUCCIONES:
# 1. Guardar este archivo como "Dockerfile" en root de hc-back-clinico
# 2. Railway Settings → Root Directory: / (cambiar a root)
# 3. Push y Railway detectará Dockerfile automáticamente
# ============================================
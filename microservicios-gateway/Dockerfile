# 🚪 DOCKERFILE GATEWAY - DENTRO DEL MICROSERVICIO
FROM eclipse-temurin:11-jdk-alpine AS builder

# Instalar Maven
RUN apk add --no-cache maven

# Crear directorio de trabajo
WORKDIR /app

# Copiar pom.xml (está en el mismo directorio)
COPY pom.xml .

# Descargar dependencias
RUN mvn dependency:go-offline -B

# Copiar código fuente (está en el mismo directorio)
COPY src ./src

# Compilar Gateway
RUN mvn clean package -DskipTests -Dspring.profiles.active=railway

# ============================================
# STAGE 2: RUNTIME
# ============================================
FROM eclipse-temurin:11-jre-alpine

# Usuario no-root
RUN addgroup -g 1001 -S gateway && \
    adduser -S gateway -u 1001 -G gateway

WORKDIR /app

# Copiar JAR compilado
COPY --from=builder /app/target/*.jar app.jar

RUN chown gateway:gateway app.jar
USER gateway

EXPOSE 8090

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=railway

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
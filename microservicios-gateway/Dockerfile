# Use Eclipse Temurin 11 (tu versión)
FROM eclipse-temurin:11-jdk-alpine

# Workdir
WORKDIR /app

# Copiar solo el microservicio gateway
COPY microservicios-gateway/ ./

# Build con Maven wrapper
RUN chmod +x ./mvnw && ./mvnw -DoutputFile=target/mvn-dependency-list.log -B -DskipTests clean dependency:list install

# Ejecutar dinámicamente
CMD ["sh", "-c", "java -jar target/*.jar"]
# MULTISTAGE BUILD - Compilar + Ejecutar
FROM maven:3.8.6-openjdk-11 AS build
WORKDIR /app

# Copiar solo el microservicio Gateway
COPY microservicios-gateway/pom.xml ./
COPY microservicios-gateway/src ./src

# Compilar la aplicación
RUN mvn clean package -DskipTests

# STAGE 2 - Runtime
FROM openjdk:11-jre-slim
WORKDIR /app

# Copiar el JAR compilado del stage anterior
COPY --from=build /app/target/*.jar app.jar

# Configuración para Railway
EXPOSE ${PORT:-8080}
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
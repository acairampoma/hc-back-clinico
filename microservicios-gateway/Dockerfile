FROM openjdk:11-jre-slim

# Variables
ENV PORT=8080
ENV JAVA_OPTS="-Xmx256m -Xms128m"

# Crear app user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Workdir
WORKDIR /app

# Esperar que el JAR esté pre-compilado
COPY microservicios-gateway/target/*.jar app.jar

# Exponer puerto
EXPOSE $PORT

# Comando simple
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
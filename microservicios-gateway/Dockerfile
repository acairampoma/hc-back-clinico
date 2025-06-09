# Usar Java 11 específicamente
FROM openjdk:11-jre-slim

# Metadatos
LABEL maintainer="Alan Cairampoma"
LABEL description="Microservicio Hospital Digital"

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Crear directorio de trabajo
WORKDIR /app

# Copiar el JAR compilado
COPY target/*.jar app.jar

# Exponer puerto (Railway usa PORT variable)
EXPOSE ${PORT:-8080}

# Configuración de JVM para contenedores
ENV JAVA_OPTS="$JAVA_OPTS -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
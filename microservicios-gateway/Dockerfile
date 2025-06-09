# Use the Eclipse temurin alpine official image
FROM eclipse-temurin:17-jdk-alpine

# Create and change to the app directory.
WORKDIR /app

# Copy local code to the container image.
COPY microservicios-gateway/ ./

# Build the app.
RUN ./mvnw -DoutputFile=target/mvn-dependency-list.log -B -DskipTests clean dependency:list install

# Run the app by dynamically finding the JAR file in the target directory
CMD ["sh", "-c", "java -jar target/*.jar"]
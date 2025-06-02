# Use official Eclipse Temurin Java 21 JDK as base image
FROM eclipse-temurin:21-jdk as builder

# Set working directory
WORKDIR /app

# Copy build artifact (replace with actual JAR name if needed)
COPY target/*.jar app.jar

# Use a minimal runtime image
FROM eclipse-temurin:21-jre

# Set working directory in runtime container
WORKDIR /app

# Copy the JAR from the builder image
COPY --from=builder /app/app.jar .

# Expose the application port
EXPOSE 8080

# Run the Spring Boot app with the default profile
ENTRYPOINT ["java", "-jar", "app.jar"]

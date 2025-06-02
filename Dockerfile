# Use a minimal Java runtime
FROM eclipse-temurin:17-jre-alpine

# Create app directory
WORKDIR /app

# Copy jar file
COPY target/*.jar app.jar

# Expose port (optional)
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]

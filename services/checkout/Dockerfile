# --- Stage 1: Build the application ---
# Use a Gradle image with the JDK as the builder.
FROM gradle:jdk17 AS builder

# Set the working directory inside the container.
WORKDIR /app

# Copy Gradle wrapper and configuration files to leverage Docker's build cache.
# This step only runs again if these files change.
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Download dependencies first to leverage Docker's cache.
# The `go-offline` command for Gradle works well for this.
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon

# Copy the rest of the source code.
COPY src ./src

# Build the application, creating a runnable JAR file.
RUN ./gradlew build --no-daemon -x test -x checkstyleMain -x checkstyleTest

# --- Stage 2: Create the final, lightweight, and secure image ---
# Use a lightweight JRE image.
FROM eclipse-temurin:17-jre

# Set the working directory.
WORKDIR /app

# Copy the built JAR file from the builder stage.
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port on which the Spring Boot application runs.
EXPOSE 8085

# Run the application.
ENTRYPOINT ["java", "-jar", "app.jar"]

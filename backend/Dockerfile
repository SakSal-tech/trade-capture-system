# -----------------------------------------------------------------------------
# Backend Dockerfile
# For Spring Boot backend
# - Build stage: use Maven + Java 21 to compile the application and cache dependencies
# - Runtime stage: lightweight Java 21 JRE, run as non-root `appuser`, include a HEALTHCHECK
# Notes: Aligns build/runtime Java versions (21). Healthcheck calls /actuator/health —
# enable Spring Boot Actuator or adjust the probe to a public endpoint if Actuator is disabled.
# -----------------------------------------------------------------------------
# ============================
# BUILD STAGE (Java 21)
# ============================
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory inside container
WORKDIR /app

# Copy pom and download dependencies first (cached layer)
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B clean package -DskipTests


# ============================
# RUNTIME STAGE (Java 21, non-root)
# ============================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Install curl for healthchecks (minimal) and create non-root user
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl ca-certificates \
	&& rm -rf /var/lib/apt/lists/* \
	&& useradd --create-home --shell /bin/bash appuser

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Ensure non-root ownership
RUN chown appuser:appuser /app/app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Optional healthcheck (container-level)
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1

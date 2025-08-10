# ---- Build stage (Gradle) ----
FROM gradle:8.9-jdk21 AS build
WORKDIR /workspace

# 1) Copy only gradle descriptors first to leverage layer cache
COPY gradlew settings.gradle* build.gradle* ./
COPY gradle gradle
RUN chmod +x gradlew

# Pre-fetch dependencies (cache layer); doesn't need sources
RUN ./gradlew --no-daemon dependencies || true

# 2) Now copy sources and build the Boot jar
COPY src src
ARG APP_VERSION=dev
# If you have a version property, you can pass it in and use it in build.gradle(.kts)
RUN ./gradlew --no-daemon -x test bootJar

# Keep only one non-plain Spring Boot jar
RUN JAR="$(ls build/libs/*.jar | grep -v 'plain' | head -n1)" \
 && cp "$JAR" /workspace/app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Basic hardening: run as non-root
RUN adduser --system --group appuser
USER appuser

COPY --from=build /workspace/app.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dserver.shutdown=graceful -Dspring.profiles.active=prod"
EXPOSE 8080

# Healthcheck hits Actuator (inside the container)
HEALTHCHECK --interval=15s --timeout=5s --retries=8 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

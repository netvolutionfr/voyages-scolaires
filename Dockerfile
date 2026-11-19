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
# Épinglé par digest : builds reproductibles + PR Dependabot à chaque nouveau build Temurin 21
FROM eclipse-temurin:21-jre@sha256:ca73b4d3961703d6d7830a4361f49ef0478f8f28b39defc07bb72f4cc7f834f6
WORKDIR /app

# Basic hardening: run as non-root
RUN adduser --system --group appuser
USER appuser

COPY --from=build /workspace/app.jar /app/app.jar

# Java4Shell (CVE-2026-114938) : désactive le décodeur Base64 AVX-512 vulnérable.
# Aucun build Temurin corrigé au 2026-11-19 ; à retirer quand eclipse-temurin:21-jre >= 21.0.13.1.
# JAVA_TOOL_OPTIONS plutôt que JAVA_OPTS, que docker-compose écrase.
ENV JAVA_TOOL_OPTIONS="-XX:-UseBASE64Intrinsics"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dserver.shutdown=graceful -Dspring.profiles.active=prod"
EXPOSE 8080

# Healthcheck hits Actuator (inside the container)
HEALTHCHECK --interval=15s --timeout=5s --retries=8 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

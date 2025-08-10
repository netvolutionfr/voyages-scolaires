# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copie d'abord les descripteurs pour profiter du cache des deps
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Puis le code
COPY src ./src
ARG APP_VERSION=dev
RUN mvn -B -DskipTests -Dapp.version=${APP_VERSION} package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# sécurité de base
RUN adduser --system --group appuser
USER appuser

# Copie du jar
COPY --from=build /workspace/target/*.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dserver.shutdown=graceful -Dspring.profiles.active=prod"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=8 CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

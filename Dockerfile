# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /usr/src/app
COPY pom.xml .
COPY src ./src
# Cache dependencies
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B && \
    mvn -B -Dmaven.test.skip package && \
    rm -rf /root/.m2

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /usr/src/app/target/*.jar app.jar

# JVM tuning
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseG1GC", \
            "-XX:+HeapDumpOnOutOfMemoryError", \
            "-XX:+DisableExplicitGC", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "/app/app.jar"]
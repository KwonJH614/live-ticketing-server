# syntax=docker/dockerfile:1.6

# -------- Stage 1: Build --------
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version

COPY src ./src
RUN ./gradlew clean bootJar -x test --no-daemon

# -------- Stage 2: Runtime --------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar
RUN chown -R spring:spring /app
USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

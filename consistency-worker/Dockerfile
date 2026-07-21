FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY backend/.mvn .mvn
COPY backend/mvnw ./mvnw
COPY consistency-worker/pom.xml ./pom.xml

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw

COPY consistency-worker/src ./src

RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S consistency \
    && adduser -S consistency -G consistency

COPY --from=build /workspace/target/maintenance-consistency-worker-0.0.1-SNAPSHOT.jar app.jar

USER consistency

EXPOSE 8082

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
    CMD curl -fsS http://localhost:8082/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

FROM node:24-alpine AS frontend-build

WORKDIR /workspace

COPY frontend/package*.json frontend/

RUN cd frontend && npm ci

COPY frontend frontend

RUN mkdir -p backend/src/main/resources/static \
    && cd frontend \
    && npm run build

FROM eclipse-temurin:21-jdk-alpine AS backend-build

WORKDIR /workspace

COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw

COPY backend/src src
COPY --from=frontend-build /workspace/backend/src/main/resources/static src/main/resources/static

RUN --mount=type=cache,target=/root/.m2 ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S vmh \
    && adduser -S vmh -G vmh

COPY --from=backend-build /workspace/target/vehicle-maintenance-history-0.0.1-SNAPSHOT.jar app.jar

USER vmh

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

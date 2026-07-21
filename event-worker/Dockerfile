FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY backend/.mvn .mvn
COPY backend/mvnw ./mvnw
COPY event-worker/pom.xml ./pom.xml

RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY event-worker/src ./src

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S history \
    && adduser -S history -G history

COPY --from=build /workspace/target/vehicle-history-event-worker-0.0.1-SNAPSHOT.jar app.jar

USER history

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

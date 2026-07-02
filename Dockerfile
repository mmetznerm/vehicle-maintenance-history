FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S vmh \
    && adduser -S vmh -G vmh

COPY --from=build /workspace/target/vehicle-maintenance-history-0.0.1-SNAPSHOT.jar app.jar

USER vmh

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
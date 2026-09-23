FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY event-contracts /workspace/event-contracts
COPY notification-service/pom.xml /workspace/notification-service/pom.xml
RUN mvn -q -N install -f /workspace/pom.xml \
    && mvn -q -f /workspace/event-contracts/pom.xml install -DskipTests
COPY notification-service /workspace/notification-service
RUN mvn -q -f /workspace/notification-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S ledgerflow && adduser -S ledgerflow -G ledgerflow
WORKDIR /app
COPY --from=build /workspace/notification-service/target/notification-service.jar /app/notification-service.jar
USER ledgerflow
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/notification-service.jar"]

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY event-contracts /workspace/event-contracts
COPY settlement-service/pom.xml /workspace/settlement-service/pom.xml
RUN mvn -q -N install -f /workspace/pom.xml \
    && mvn -q -f /workspace/event-contracts/pom.xml install -DskipTests
COPY settlement-service /workspace/settlement-service
RUN mvn -q -f /workspace/settlement-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S ledgerflow && adduser -S ledgerflow -G ledgerflow
WORKDIR /app
COPY --from=build /workspace/settlement-service/target/settlement-service.jar /app/settlement-service.jar
USER ledgerflow
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/settlement-service.jar"]

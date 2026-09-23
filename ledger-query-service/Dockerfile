FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY event-contracts /workspace/event-contracts
COPY ledger-query-service/pom.xml /workspace/ledger-query-service/pom.xml
RUN mvn -q -N install -f /workspace/pom.xml \
    && mvn -q -f /workspace/event-contracts/pom.xml install -DskipTests
COPY ledger-query-service /workspace/ledger-query-service
RUN mvn -q -f /workspace/ledger-query-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S ledgerflow && adduser -S ledgerflow -G ledgerflow
WORKDIR /app
COPY --from=build /workspace/ledger-query-service/target/ledger-query-service.jar /app/ledger-query-service.jar
USER ledgerflow
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/ledger-query-service.jar"]

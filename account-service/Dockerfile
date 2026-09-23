FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY event-contracts /workspace/event-contracts
COPY account-service/pom.xml /workspace/account-service/pom.xml
RUN mvn -q -N install -f /workspace/pom.xml \
    && mvn -q -f /workspace/event-contracts/pom.xml install -DskipTests
COPY account-service /workspace/account-service
RUN mvn -q -f /workspace/account-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S ledgerflow && adduser -S ledgerflow -G ledgerflow
WORKDIR /app
COPY --from=build /workspace/account-service/target/account-service.jar /app/account-service.jar
USER ledgerflow
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/account-service.jar"]

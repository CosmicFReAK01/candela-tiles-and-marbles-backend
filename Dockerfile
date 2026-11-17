# Multi-stage build for Candela Tiles & Marbles Backend Monolith
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Production runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as non-root user for security hardening
RUN useradd -m -s /bin/bash candela && chown -R candela:candela /app
USER candela

COPY --from=builder /app/target/candela-monolith-1.0.0.jar /app/candela-monolith.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/candela-monolith.jar"]

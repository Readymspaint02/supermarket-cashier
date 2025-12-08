# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/target/gbs_commerce_system-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]

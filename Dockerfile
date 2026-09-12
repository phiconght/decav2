# Multi-stage build: build bang Maven, chay bang JRE de image nhe
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -S spring
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /data/storage && chown -R spring:spring /data/storage
USER spring
EXPOSE 9090
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]

FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:21-ea-34-slim

ARG APPLICATION_USER=spring

RUN addgroup --system $APPLICATION_USER && adduser --system --ingroup $APPLICATION_USER $APPLICATION_USER

WORKDIR /app

USER spring:spring

COPY --from=build target/prepeardness-api*.jar /app/prepeardness-api.jar
EXPOSE 8080
CMD ["java", "-jar", "prepeardness-api.jar"]
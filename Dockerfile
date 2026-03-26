FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:25-rc-jdk-slim-trixie

ARG APPLICATION_USER=spring

RUN groupadd -r $APPLICATION_USER && useradd -r -g $APPLICATION_USER $APPLICATION_USER

WORKDIR /app

USER spring:spring

COPY --from=build /app/target/prepeardness-api*.jar .
EXPOSE 8080
CMD ["java", "-jar", "prepeardness-api.jar"]
FROM eclipse-temurin:21-jre-alpine

# Sikkerhet: Lag en ikke-root-bruker
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Kopier filen som actions/download-artifact la i target-mappen
COPY target/*.jar app.jar

# Gi 'spring'-brukeren eierskap til filene
RUN chown -R spring:spring /app

USER spring

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
